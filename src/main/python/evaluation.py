import json
import math
import pickle
import sys
from pathlib import Path
from PyPDF2 import PdfWriter, PdfReader

import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

flutterKey = 'Flutter'
reactNativeKey = 'React Native'
qtKey = 'Qt'
unityKey = 'Unity'
cordovaKey = 'Cordova'
xamarinKey = 'Xamarin'

frameworkJsonKeys = [flutterKey, reactNativeKey, qtKey, unityKey, cordovaKey, xamarinKey]

androidJsonKey = 'AndroidAPI'
minSdkKey = 'minSdkVersion'
targetSdkKey = 'targetSdkVersion'
compileSdkKey = 'compileSdkVersion'
versionJsonKey = 'Version'

androidVersionFound = 0  # added if one of minSdkVersion, targetSdkVersion, and compileSdkVersion is found
frameworkImplementationAndVersionAndByDateFound = {
    # first entry is added if an implementation is found, second is if the version is found
    flutterKey: [0, 0, 0],
    reactNativeKey: [0, 0, 0],
    qtKey: [0, 0, 0],
    unityKey: [0, 0, 0],
    cordovaKey: [0, 0, 0],
    xamarinKey: [0, 0, 0]
}

# dictionaries to store the versions
androidMinSdkVersions = {}
androidTargetSdkVersions = {}
androidCompileSdkVersions = {}
frameworkVersions = {
    flutterKey: {},
    reactNativeKey: {},
    qtKey: {},
    unityKey: {},
    cordovaKey: {},
    xamarinKey: {}
}


def evaluation_graphs(filepath):
    global androidVersionFound
    global androidMinSdkVersions
    global androidTargetSdkVersions
    global androidCompileSdkVersions

    file = open(filepath, 'r')
    lines = file.readlines()

    for line in lines:
        try:
            jsonFile = open(line.strip(), 'r')
            data = json.load(jsonFile)
        except OSError:
            print("The file {} cannot be opened.".format(line.strip()))
            continue

        # android versions
        android = data[androidJsonKey]
        if android[minSdkKey] != -1 or android[targetSdkKey] != -1 or android[compileSdkKey] != -1:
            androidVersionFound += 1
            if android[minSdkKey] != -1:
                increment_dict(androidMinSdkVersions, android[minSdkKey])
            if android[targetSdkKey] != -1:
                increment_dict(androidTargetSdkVersions, android[targetSdkKey])
            if android[compileSdkKey] != -1:
                increment_dict(androidCompileSdkVersions, android[compileSdkKey])

        # framework versions
        for frameworkKey in frameworkJsonKeys:
            if frameworkKey in data:
                framework = data[frameworkKey]
                if framework:
                    increment_lists_dict(frameworkImplementationAndVersionAndByDateFound, frameworkKey, 0)
                    if "perhaps too old or too new?" not in framework[versionJsonKey]:
                        increment_lists_dict(frameworkImplementationAndVersionAndByDateFound, frameworkKey, 1)
                        if "found by APK last modified date" in framework[versionJsonKey]:
                            increment_lists_dict(frameworkImplementationAndVersionAndByDateFound, frameworkKey, 2)

                        versions = framework[versionJsonKey].split(', ')
                        for version in versions:
                            v = version.split(' ')
                            increment_dict(frameworkVersions[frameworkKey], v[0])
        jsonFile.close()
    file.close()

    # sort keys
    androidMinSdkVersions = dict(sorted(androidMinSdkVersions.items()))
    androidTargetSdkVersions = dict(sorted(androidTargetSdkVersions.items()))
    androidCompileSdkVersions = dict(sorted(androidCompileSdkVersions.items()))


def increment_lists_dict(myDict, key, pos):
    if not myDict.get(key):
        myDict[key][pos] = 1
    else:
        myDict[key][pos] += 1


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
        return range(min(0, math.floor(minimum)), math.ceil(maximum)+1)
    else:
        return range(0, 1)


def save_graphs(resultFile):
    p = PdfPages(resultFile)

    fig_nums = plt.get_fignums()
    figs = [plt.figure(n) for n in fig_nums]

    for fig in figs:
        fig.savefig(p, format='pdf')

    p.close()

    # delete last page because somehow an empty page is always created
    infile = PdfReader(resultFile)
    output = PdfWriter()

    for i in range(0, 12):
        p = infile.pages[i]
        output.add_page(p)

    with open(resultFile, 'wb') as f:
        output.write(f)


def draw_graphs():
    # android api
    plt.bar(range(len(androidMinSdkVersions)), list(androidMinSdkVersions.values()), align='center')
    plt.xticks(range(len(androidMinSdkVersions)), list(androidMinSdkVersions.keys()))
    plt.yticks(integer_axis(androidMinSdkVersions.values()))
    plt.title('Found Android API minSdkVersions')
    plt.figure()

    plt.bar(range(len(androidTargetSdkVersions)), list(androidTargetSdkVersions.values()), align='center')
    plt.xticks(range(len(androidTargetSdkVersions)), list(androidTargetSdkVersions.keys()))
    plt.yticks(integer_axis(androidTargetSdkVersions.values()))
    plt.title('Found Android API targetSdkVersions')
    plt.figure()

    plt.bar(range(len(androidCompileSdkVersions)), list(androidCompileSdkVersions.values()), align='center')
    plt.xticks(range(len(androidCompileSdkVersions)), list(androidCompileSdkVersions.keys()))
    plt.yticks(integer_axis(androidCompileSdkVersions.values()))
    plt.title('Found Android API compileSdkVersions')
    plt.figure()

    # frameworks
    frameworkNames = list(frameworkImplementationAndVersionAndByDateFound.keys())
    implementationFound = get_elements(list(frameworkImplementationAndVersionAndByDateFound.values()), 0)
    versionFound = get_elements(list(frameworkImplementationAndVersionAndByDateFound.values()), 1)
    byDateFound = get_elements(list(frameworkImplementationAndVersionAndByDateFound.values()), 2)

    plt.bar(range(len(frameworkImplementationAndVersionAndByDateFound)), implementationFound, align='center')
    plt.xticks(range(len(frameworkImplementationAndVersionAndByDateFound)), frameworkNames)
    plt.yticks(integer_axis(implementationFound))
    plt.title('Found framework implementations')
    plt.figure()

    plt.bar(range(len(frameworkImplementationAndVersionAndByDateFound)), versionFound, align='center')
    plt.xticks(range(len(frameworkImplementationAndVersionAndByDateFound)), frameworkNames)
    plt.yticks(integer_axis(versionFound))
    plt.title('Found framework versions (total)')
    plt.figure()

    plt.bar(range(len(frameworkImplementationAndVersionAndByDateFound)), byDateFound, align='center')
    plt.xticks(range(len(frameworkImplementationAndVersionAndByDateFound)), frameworkNames)
    plt.yticks(integer_axis(versionFound))
    plt.title('Found framework versions (by date)')
    plt.figure()

    for framework in frameworkVersions:
        frameworkVersion = frameworkVersions[framework]
        plt.bar(range(len(frameworkVersion)), list(frameworkVersion.values()), align='center')
        plt.xticks(range(len(frameworkVersion)), list(frameworkVersion.keys()))
        plt.yticks(integer_axis(frameworkVersion.values()))
        plt.title("{} versions found".format(framework))
        plt.figure()


def pickle_data(pickleFile, jsonFile):
    # pickle data to a file
    with open(pickleFile, 'wb') as handle:
        pickle.dump([androidMinSdkVersions,
                     androidTargetSdkVersions,
                     androidCompileSdkVersions,
                     frameworkImplementationAndVersionAndByDateFound,
                     frameworkVersions],
                    handle,
                    protocol=pickle.HIGHEST_PROTOCOL)

    # convert pickled data to json (if needed)
    myDict = pickle.load(open(pickleFile, 'rb'))
    jsonObject = json.dumps(myDict, sort_keys=True, indent=4)

    with open(jsonFile, "w") as outfile:
        outfile.write(jsonObject)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise Exception(f'Please specify the path to the file containing the JSON filepaths!'
                        f'Expected 1 argument, but was {len(sys.argv) - 1}.\n'
                        f'Make sure there are no spaces in the path.')

    evaluation_graphs(sys.argv[1])
    draw_graphs()

    path = Path(sys.argv[1]).parent
    file = Path(sys.argv[1]).stem

    evalFile = str(path) + "\\" + str(file) + "_evaluation.pdf"
    save_graphs(evalFile)

    pickleFile = str(path) + "\\" + str(file) + "_pickle.p"
    jsonFile = str(path) + "\\" + str(file) + "_pickle.json"
    pickle_data(pickleFile, jsonFile)
