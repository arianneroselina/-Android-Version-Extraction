import sys
import csv
import glob
import hashlib
import os
from os.path import exists
import subprocess

flutter_file = 'libflutter.so'
react_native_file = 'libreact*.so'
xamarin_so_file = 'libxa-internal-api.so'
xamarin_dll_file = 'Java.Interop.dll'
qt_files = ['libQt5Core*.so', 'libQt6Core*.so']

xamarin_folders = ['arm64-v8a', 'armeabi-v7a']
flutter_folders = xamarin_folders + ['x86_64']
react_native_folders = flutter_folders + ['x86']
qt_folders = react_native_folders


def hash_file(filename):
    """"Return the SHA-1 hash of the file passed into it"""

    # make a hash object
    h = hashlib.sha256()

    # open file for reading in binary mode
    with open(filename, 'rb') as file:
        # loop till the end of the file
        chunk = 0
        while chunk != b'':
            # read only 1024 bytes at a time
            chunk = file.read(1024)
            h.update(chunk)

    # return the hex representation of digest
    return h.hexdigest()


def write_hash_to_file(filepath, version, csv_file):
    output_hash = hash_file(filepath)

    finish = False
    versions_list = []

    if exists(csv_file):
        with open(csv_file, 'r') as csv_read:
            csv_reader = csv.reader(csv_read)
            for row in csv_reader:
                change = False
                for i in range(len(row)):
                    if change:
                        row[i] = output_hash
                        change = False
                        finish = True
                    if row[i] == version:
                        change = True
                if len(row) != 0:
                    versions_list.append(row)

        with open(csv_file, 'w', newline='') as csv_write:
            writer = csv.writer(csv_write)
            writer.writerows(versions_list)

    if not finish:
        os.makedirs(os.path.dirname(csv_file), exist_ok=True)
        with open(csv_file, 'a') as csv_append:
            csv_append.write(version + ',' + output_hash)


def hash_flutter_xamarin(version, path, framework_type, file, folders):
    """"Get the hash of $file file from the given $folders folders, then write them in the corresponding .csv files."""

    for folder in folders:
        csv_file = f'../../files/hashes/{framework_type}/{folder}.csv'
        try:
            # check if file exists
            filepath = path + 'lib\\' + folder + '\\' + file
            if not os.path.isfile(filepath):
                continue

            write_hash_to_file(filepath, version, csv_file)

        except subprocess.CalledProcessError as e:
            raise RuntimeError("Command '{}' return with error (code {}): {}".format(e.cmd, e.returncode, e.output))


def hash_react_native(version, react_native_files):
    """"Get the hash of libreact*.so file from arm64_v8a, armeabi_v7a, x86, and x86_64 folders,
    then write them in the corresponding .csv files."""

    for filepath in react_native_files:
        splitted = str(filepath).split('\\')
        filename = splitted[len(splitted) - 1]

        for folder in react_native_folders:
            csv_file = f'../../files/hashes/React Native/{folder}.csv'
            try:
                output_hash = hash_file(filepath)

                finish = False
                versions_list = []

                if exists(csv_file):
                    with open(csv_file, 'r') as csv_read:
                        csv_reader = csv.reader(csv_read)
                        for row in csv_reader:
                            change = False
                            for i in range(len(row)):
                                if change and i == 2:
                                    row[i] = output_hash
                                    change = False
                                    finish = True
                                if row[i] == version and row[i + 1] == filename:
                                    change = True
                            if len(row) != 0:
                                versions_list.append(row)

                    with open(csv_file, 'w', newline='') as csv_write:
                        writer = csv.writer(csv_write)
                        writer.writerows(versions_list)

                if not finish:
                    os.makedirs(os.path.dirname(csv_file), exist_ok=True)
                    with open(csv_file, 'a') as csv_append:
                        csv_append.write(f'{version},{filename},{output_hash}')

            except subprocess.CalledProcessError as e:
                raise RuntimeError("Command '{}' return with error (code {}): {}".format(e.cmd, e.returncode, e.output))


def hash_xamarin(version, path):
    """"Get the hash of Java.Interop.dll file from assemblies folder, then write them in the corresponding .csv
    files."""

    csv_file = f'../../files/hashes/Xamarin/assemblies.csv'
    try:
        # check if file exists
        filepath = path + 'assemblies\\' + xamarin_dll_file
        if not os.path.isfile(filepath):
            return

        write_hash_to_file(filepath, version, csv_file)

    except subprocess.CalledProcessError as e:
        raise RuntimeError("Command '{}' return with error (code {}): {}".format(e.cmd, e.returncode, e.output))


def hash_qt(version, path):
    """"Get the hash of libQt5/6Core...so file from arm64_v8a, armeabi_v7a, x86, and x86_64 folders, then write them in
    the corresponding .csv files."""

    for folder in qt_folders:
        for qt_file in qt_files:
            csv_file = f'../../files/hashes/Qt/{folder}.csv'
            try:
                # check if file exists
                filepath = path + 'lib\\' + folder + '\\' + qt_file.replace('*', "_" + folder)
                if not os.path.isfile(filepath):
                    continue

                write_hash_to_file(filepath, version, csv_file)

            except subprocess.CalledProcessError as e:
                raise RuntimeError("Command '{}' return with error (code {}): {}".format(e.cmd, e.returncode, e.output))


def add_slash_to_path(path):
    if not path.endswith('\\'):
        path += '\\'
    return path


def compare_versions(version1: str, version2: str):
    split1 = version1.split('.')
    split2 = version2.split('.')
    if split1[0] < split2[0]:
        return -1
    elif split1[0] > split2[0]:
        return 1
    elif split1[0] == split2[0]:
        if split1[1] < split2[1]:
            return -1
        elif split1[1] > split2[1]:
            return 1
        elif split1[1] == split2[1]:
            if split1[2] < split2[2]:
                return -1
            elif split1[2] > split2[2]:
                return 1
            elif split1[2] == split2[2]:
                return 0


if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise Exception(f'Please specify the framework name, the version and the project path!'
                        f'Expected 3 arguments, but was {len(sys.argv) - 1}.\n'
                        f'Make sure there are no spaces in the path.')

    path = add_slash_to_path(sys.argv[3])

    if sys.argv[1].lower() == "flutter":
        hash_flutter_xamarin(sys.argv[2], path, "Flutter", flutter_file, flutter_folders)

    if sys.argv[1].lower() == "react_native":
        hash_react_native(sys.argv[2], glob.glob(path + 'lib\\' + react_native_folders[0] + '\\' + react_native_file))

    if sys.argv[1].lower() == "xamarin":
        hash_flutter_xamarin(sys.argv[2], path, "Xamarin", xamarin_so_file, xamarin_folders)
        hash_xamarin(sys.argv[2], path)

    if sys.argv[1].lower() == "qt":
        hash_qt(sys.argv[2], path)
