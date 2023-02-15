import sys
import os
from os.path import exists
import subprocess


# extract the CVE number and write them in the corresponding cve_links files
def write_cve_links(version, cve_list_path):
    try:
        if not os.path.isfile(cve_list_path):
            raise RuntimeError("{} is not a valid path".format(cve_list_path))

        csv_file = f'../../files/vulnerability_links/AndroidAPI/Android-all.csv'
        if version != "-1":
            csv_file = f'../../files/vulnerability_links/AndroidAPI/Android-{version}.csv'

        with open(cve_list_path, 'r') as cve_list_read:
            next(cve_list_read)
            for cve_list_row in cve_list_read:
                cve_number = cve_list_row.split()[1]
                found = False
                # check if cve number is already written
                if exists(csv_file):
                    with open(csv_file, 'r') as csv_read:
                        for row in csv_read:
                            if row.strip() == cve_number:
                                found = True
                                break
                else:
                    os.makedirs(os.path.dirname(csv_file), exist_ok=True)
                # write only when it is not yet written
                if not found:
                    with open(csv_file, 'a', newline='') as csv_append:
                        csv_append.write(cve_number + '\n')

    except subprocess.CalledProcessError as e:
        raise RuntimeError("Command '{}' return with error (code {}): {}".format(e.cmd, e.returncode, e.output))


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise Exception(f'Please specify the Android API level and the cve list file path!'
                        f'Expected 2 arguments, but was {len(sys.argv) - 1}.\n'
                        f'Make sure there are no spaces in the path.\n'
                        f'Set -1 to specify \'all\' Android versions.')

    write_cve_links(sys.argv[1], sys.argv[2])
