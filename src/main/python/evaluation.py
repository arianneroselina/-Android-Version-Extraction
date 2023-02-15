import json
import math
import sys
import matplotlib.pyplot as plt

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
frameworkImplementationAndVersionFound = {
    # first entry is added if an implementation is found, second is if the version is found
    flutterKey: [0, 0],
    reactNativeKey: [0, 0],
    qtKey: [0, 0],
    unityKey: [0, 0],
    cordovaKey: [0, 0],
    xamarinKey: [0, 0]
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
                    increment_lists_dict(frameworkImplementationAndVersionFound, frameworkKey, 0)
                    if "perhaps too old or too new?" not in framework[versionJsonKey]:
                        increment_lists_dict(frameworkImplementationAndVersionFound, frameworkKey, 1)

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


def draw_graph():
    # android api
    plt.bar(range(len(androidMinSdkVersions)), list(androidMinSdkVersions.values()), align='center')
    plt.xticks(range(len(androidMinSdkVersions)), list(androidMinSdkVersions.keys()))
    plt.yticks(integer_axis(androidMinSdkVersions.values()))
    plt.title('Found Android API minSdkVersions')
    plt.show()

    plt.bar(range(len(androidTargetSdkVersions)), list(androidTargetSdkVersions.values()), align='center')
    plt.xticks(range(len(androidTargetSdkVersions)), list(androidTargetSdkVersions.keys()))
    plt.yticks(integer_axis(androidTargetSdkVersions.values()))
    plt.title('Found Android API targetSdkVersions')
    plt.show()

    plt.bar(range(len(androidCompileSdkVersions)), list(androidCompileSdkVersions.values()), align='center')
    plt.xticks(range(len(androidCompileSdkVersions)), list(androidCompileSdkVersions.keys()))
    plt.yticks(integer_axis(androidCompileSdkVersions.values()))
    plt.title('Found Android API compileSdkVersions')
    plt.show()

    # frameworks
    frameworkNames = list(frameworkImplementationAndVersionFound.keys())
    implementationFound = get_elements(list(frameworkImplementationAndVersionFound.values()), 0)
    versionFound = get_elements(list(frameworkImplementationAndVersionFound.values()), 1)

    plt.bar(range(len(frameworkImplementationAndVersionFound)), implementationFound, align='center')
    plt.xticks(range(len(frameworkImplementationAndVersionFound)), frameworkNames)
    plt.yticks(integer_axis(implementationFound))
    plt.title('Found framework implementations')
    plt.show()

    plt.bar(range(len(frameworkImplementationAndVersionFound)), versionFound, align='center')
    plt.xticks(range(len(frameworkImplementationAndVersionFound)), frameworkNames)
    plt.yticks(integer_axis(versionFound))
    plt.title('Found framework versions')
    plt.show()

    for framework in frameworkVersions:
        frameworkVersion = frameworkVersions[framework]
        plt.bar(range(len(frameworkVersion)), list(frameworkVersion.values()), align='center')
        plt.xticks(range(len(frameworkVersion)), list(frameworkVersion.keys()))
        plt.yticks(integer_axis(frameworkVersion.values()))
        plt.title("{} versions found".format(framework))
        plt.show()


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise Exception(f'Please specify the path to the file containing the JSON filepaths!'
                        f'Expected 1 argument, but was {len(sys.argv) - 1}.\n'
                        f'Make sure there are no spaces in the path.')

    evaluation_graphs(sys.argv[1])
    draw_graph()
