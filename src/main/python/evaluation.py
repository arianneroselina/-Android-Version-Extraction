import json
import math
import pickle
import sys
from pathlib import Path

import numpy as np
from PyPDF2 import PdfWriter, PdfReader

import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

# constants
flutterKey = 'Flutter'
reactNativeKey = 'React Native'
qtKey = 'Qt'
unityKey = 'Unity'
cordovaKey = 'Cordova'
xamarinKey = 'Xamarin'

frameworkKeys = [flutterKey, reactNativeKey, qtKey, unityKey, cordovaKey, xamarinKey]

androidKey = 'AndroidAPI'
minSdkKey = 'minSdkVersion'
targetSdkKey = 'targetSdkVersion'
compileSdkKey = 'compileSdkVersion'
versionKey = 'Version'

implementationFound = 'Implementation found'
versionFoundInitial = 'Version found with initial method'
versionFoundByDate = 'Version found by date'
versionNotFound = 'Version not found'

totalEntries = 0

# added if one of minSdkVersion, targetSdkVersion, and compileSdkVersion is found
android = {
    implementationFound: 0,
    minSdkKey: {},
    targetSdkKey: {},
    compileSdkKey: {},
}

# first entry is added if an implementation is found, second is if the version is found, third if the version is found
# by date
frameworks = {
    flutterKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {}
    },
    reactNativeKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {}
    },
    qtKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {}
    },
    unityKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {}
    },
    cordovaKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {}
    },
    xamarinKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {}
    },
}


def evaluation_graphs(filepath):
    global totalEntries

    try:
        file = open(filepath, 'r')
        lines = file.readlines()

        for line in lines:
            try:
                jsonData = open(line.strip(), 'r')
                data = json.load(jsonData)
            except:
                print(f"Failed reading JSON data {line.strip()}.")
                continue

            try:
                # count number of file
                totalEntries += 1

                # android versions
                thisAndroidData = data[androidKey]
                if thisAndroidData[minSdkKey] != -1 or thisAndroidData[targetSdkKey] != -1 \
                        or thisAndroidData[compileSdkKey] != -1:
                    increment_dict(android, implementationFound)
                    if thisAndroidData[minSdkKey] != -1:
                        increment_dict(android[minSdkKey], thisAndroidData[minSdkKey])
                    if thisAndroidData[targetSdkKey] != -1:
                        increment_dict(android[targetSdkKey], thisAndroidData[targetSdkKey])
                    if thisAndroidData[compileSdkKey] != -1:
                        increment_dict(android[compileSdkKey], thisAndroidData[compileSdkKey])

                # framework versions
                for frameworkKey in frameworkKeys:
                    if frameworkKey in data:
                        thisFramework = data[frameworkKey]
                        if thisFramework:
                            increment_dict(frameworks[frameworkKey], implementationFound)
                            found = False

                            if "perhaps too old or too new?" in thisFramework[versionKey]:
                                increment_dict(frameworks[frameworkKey], versionNotFound)
                            elif "found by APK last modified date" in thisFramework[versionKey]:
                                increment_dict(frameworks[frameworkKey], versionFoundByDate)
                                found = True
                            else:
                                increment_dict(frameworks[frameworkKey], versionFoundInitial)
                                found = True

                            if found:
                                versions = thisFramework[versionKey].split(', ')
                                for version in versions:
                                    v = version.split(' ')

                                    # workarounds for some weird strings in versions
                                    if frameworkKey == cordovaKey:
                                        v[0] = fix_cordova_version(v[0])
                                    if frameworkKey == unityKey:
                                        v[0] = fix_unity_version(v[0])

                                    if v[0] and v[0] != "":
                                        increment_dict(frameworks[frameworkKey][versionKey], v[0])

                jsonData.close()
            except:
                print(f"Failed processing the JSON data {line.strip()}.")
                continue

        file.close()

        # sort keys
        android[minSdkKey] = dict(sorted(android[minSdkKey].items()))
        android[targetSdkKey] = dict(sorted(android[targetSdkKey].items()))
        android[compileSdkKey] = dict(sorted(android[compileSdkKey].items()))

    except:
        print(f"The file {filepath} cannot be opened.")


def increment_dict(myDict, key):
    if not myDict.get(key):
        myDict[key] = 1
    else:
        myDict[key] += 1


def get_elements(lst, pos):
    return [item[pos] for item in lst]


def integer_axis(lst):
    if lst:
        minimum = min(lst)
        maximum = max(lst)
        return range(min(0, math.floor(minimum)), math.ceil(maximum) + 1)
    else:
        return range(0, 1)


def fix_cordova_version(version):
    """Workaround for cordova version, because sometimes it contains weird strings."""
    result = ""
    for c in version:
        if c.isnumeric() or c == '.':
            result += c
        else:
            break
    return result


def fix_unity_version(version):
    """Workaround for unity version, because sometimes it contains weird strings."""
    if "." in version and len(version) < 15:
        return version
    return ""


def save_graphs(resultFile):
    try:
        p = PdfPages(resultFile)

        fig_nums = plt.get_fignums()
        figs = [plt.figure(n) for n in fig_nums]

        for fig in figs:
            fig.savefig(p, format='pdf')

        p.close()

        # delete last page because somehow an empty page is always created
        infile = PdfReader(resultFile)
        output = PdfWriter()

        for i in range(0, len(infile.pages)):
            p = infile.pages[i]
            output.add_page(p)

        with open(resultFile, 'wb') as f:
            output.write(f)
    except:
        print("The function save_graphs() runs with errors.")



def draw_graphs():
    plt.rcParams["figure.figsize"] = (15, 5)

    # found implementations
    try:
        implementationKeys = [androidKey]
        implementations = [android[implementationFound]]
        for f in frameworks:
            implementationKeys.append(f)
            implementations.append(frameworks[f][implementationFound])

        x = range(len(implementations))

        plt.bar(x, list([totalEntries] * len(implementations)), label='Total apps', color=(0.2, 0.4, 0.6, 0.6))
        bars = plt.bar(x, list(implementations), label='Implementation found')
        plt.xticks(x, list(implementationKeys))
        plt.yticks(np.arange(0, totalEntries + 1, step=math.ceil(totalEntries / 20)))
        plt.legend()
        plt.tight_layout()

        # add percentages above the bars
        for rect in bars:
            height = rect.get_height()
            plt.text(rect.get_x() + rect.get_width() / 2.0, height,
                     f'{height / totalEntries * 100:.0f}%', ha='center', va='bottom')

        plt.figure()
    except:
        print("Failed to plot 1 graph for found Android API and frameworks.")

    # android api
    try:
        for a in android:
            if a != implementationFound:
                x = range(len(android[a]))
                if not android[a].values():
                    highest = 1
                else:
                    highest = max(android[a].values())

                bars = plt.bar(x, list(android[a].values()))
                plt.xticks(x, list(android[a].keys()))
                plt.yticks(np.arange(0, highest + 1, step=math.ceil(highest/20)))
                plt.title(f"Found Android API {a}")

                # add counts above the bars
                for rect in bars:
                    height = rect.get_height()
                    if height != 0:
                        plt.text(rect.get_x() + rect.get_width() / 2.0, height, f'{height:.0f}', ha='center', va='bottom')

                plt.figure()
    except:
        print("Failed to plot 3 graphs for found Android API versions.")

    # frameworks
    try:
        frameworkNames = list(frameworks.keys())
        frameworkImpl = get_elements(list(frameworks.values()), implementationFound)
        frameworkInit = get_elements(list(frameworks.values()), versionFoundInitial)
        frameworkDate = get_elements(list(frameworks.values()), versionFoundByDate)
        frameworkNotFound = get_elements(list(frameworks.values()), versionNotFound)
        width = 0.3
        x = np.arange(len(frameworks.keys()))

        if not frameworkImpl:
            highest = 1
        else:
            highest = max(frameworkImpl)

        plt.bar(x, frameworkImpl, width=0.9, label='Implementation found', color=(0.2, 0.4, 0.6, 0.6))
        barsInit = plt.bar(x - width, frameworkInit, width=width, label='Version found by initial methods', color='green')
        barsDate = plt.bar(x, frameworkDate, width=width, label='Version found by date', color='orange')
        barsNotFound = plt.bar(x + width, frameworkNotFound, width=width, label='Version not found', color='red')
        plt.xticks(range(len(frameworks)), frameworkNames)
        plt.yticks(np.arange(0, highest + 1, step=math.ceil(highest/20)))
        plt.legend()
        plt.title('Framework versions')

        # add percentages above the bars
        for bars in [barsInit, barsDate, barsNotFound]:
            i = 0
            for rect in bars:
                height = rect.get_height()
                total = frameworkImpl[i]

                if total == 0:
                    percent = 0
                else:
                    percent = height / frameworkImpl[i] * 100

                plt.text(rect.get_x() + rect.get_width() / 2.0, height, f'{percent:.0f}%', ha='center', va='bottom')
                i += 1

        plt.figure()
    except:
        print("Failed to plot 1 graph for frameworks' statistics.")

    try:
        for f in frameworks:
            frameworkVersion = frameworks[f][versionKey]
            x = range(len(frameworkVersion))

            if not frameworkVersion.values():
                highest = 1
            else:
                highest = max(frameworkVersion.values())

            bars = plt.bar(x, list(frameworkVersion.values()), align='center')
            plt.xticks(x, list(frameworkVersion.keys()))
            plt.yticks(np.arange(0, highest + 1, step=math.ceil(highest/20)))
            plt.title("{} versions found".format(f))

            # add counts above the bars
            for rect in bars:
                height = rect.get_height()
                if height != 0:
                    plt.text(rect.get_x() + rect.get_width() / 2.0, height, f'{height:.0f}', ha='center', va='bottom')

            plt.figure()
    except:
        print("Failed to plot 6 graphs for found frameworks' versions.")


def pickle_data(pickleFile, jsonFile):
    # pickle data to a file
    try:
        with open(pickleFile, 'wb') as handle:
            pickle.dump({"Android": android, "Frameworks": frameworks}, handle, protocol=pickle.HIGHEST_PROTOCOL)
    except:
        print(f"Failed to write the data to the pickle file {pickleFile}.")

    # write data to json
    try:
        jsonObject = json.dumps({"Total apps:": totalEntries, "Android": android, "Frameworks": frameworks}, sort_keys=True, indent=4)
        with open(jsonFile, "w") as outfile:
            outfile.write(jsonObject)
    except:
        print(f"Failed to write the data to the JSON file {jsonFile}.")


# to draw the graphs if given a previous output JSON file from this evaluation script
def from_json_output(filepath):
    global totalEntries
    global android
    global frameworks

    jsonData = open(filepath, 'r')
    data = json.load(jsonData)

    totalEntries = data["Android"][implementationFound]
    android = data["Android"]
    frameworks = data["Frameworks"]


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise Exception(f'Please specify the path to the file containing the JSON filepaths!'
                        f'Expected 1 argument, but was {len(sys.argv) - 1}.\n'
                        f'Make sure there are no spaces in the path.')

    evaluation_graphs(sys.argv[1])
    # from_json_output(sys.argv[1])
    draw_graphs()

    path = Path(sys.argv[1]).parent
    file = Path(sys.argv[1]).stem

    evalFile = str(path) + "/" + str(file) + "_evaluation.pdf"
    save_graphs(evalFile)

    pickleFile = str(path) + "/" + str(file) + "_evaluation.p"
    jsonFile = str(path) + "/" + str(file) + "_evaluation.json"
    pickle_data(pickleFile, jsonFile)
