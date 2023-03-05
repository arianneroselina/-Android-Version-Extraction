import json
import math
import pickle
import sys
from pathlib import Path

import numpy as np
import pandas as pd
from pandas.plotting import table
from PyPDF2 import PdfWriter, PdfReader

import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

################# CONSTANTS #################


frameworkKey = 'Frameworks'
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
vulnerabilitiesKey = 'Vulnerabilities'

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
    vulnerabilitiesKey: 0
}

# first entry is added if an implementation is found, second is if the version is found, third if the version is found
# by date
frameworks = {
    flutterKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {},
        vulnerabilitiesKey: 0
    },
    reactNativeKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {},
        vulnerabilitiesKey: 0
    },
    qtKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {},
        vulnerabilitiesKey: 0
    },
    unityKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {},
        vulnerabilitiesKey: 0
    },
    cordovaKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {},
        vulnerabilitiesKey: 0
    },
    xamarinKey: {
        implementationFound: 0,
        versionFoundInitial: 0,
        versionFoundByDate: 0,
        versionNotFound: 0,
        versionKey: {},
        vulnerabilitiesKey: 0
    },
}


################# GET DATA #################


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
                    for v in thisAndroidData[vulnerabilitiesKey]:
                        increment_dict_by_n(android, vulnerabilitiesKey, len(thisAndroidData[vulnerabilitiesKey][v]))

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

                            for v in thisFramework[vulnerabilitiesKey]:
                                increment_dict_by_n(frameworks[frameworkKey], vulnerabilitiesKey,
                                                    len(thisFramework[vulnerabilitiesKey][v]))

                jsonData.close()
            except:
                print(f"Failed processing the JSON data {line.strip()}.")
                continue

        file.close()

        # sort keys
        android[minSdkKey] = dict(sorted(android[minSdkKey].items()))
        android[targetSdkKey] = dict(sorted(android[targetSdkKey].items()))
        android[compileSdkKey] = dict(sorted(android[compileSdkKey].items()))
        sort_framework_versions()

    except:
        print(f"The file {filepath} cannot be opened.")


################# SORTING FUNCTIONS #################


def sort_unity_framework_versions(x, y):
    vx = x[0].split('.')
    vy = y[0].split('.')

    try:
        for i in range(len(vx)):
            # version contains letters
            if vx[i].upper().isupper() or vy[i].upper().isupper():
                if vx[i] <= vy[i]:
                    return x, y
                else:
                    return y, x
            if int(vx[i]) < int(vy[i]):
                return x, y
            elif int(vx[i]) == int(vy[i]):
                continue
            else:
                return y, x
        return x, y
    except:
        print("Failed sorting framework versions")


def sort_other_frameworks_versions(x, y):
    vx = x[0].split('.')
    vy = y[0].split('.')

    try:
        for i in range(len(vx)):
            a = ''.join(filter(str.isdigit, vx[i]))
            b = ''.join(filter(str.isdigit, vy[i]))
            if int(a) < int(b):
                return x, y
            elif int(a) == int(b):
                continue
            else:
                return y, x
        return x, y
    except:
        print("Failed sorting framework versions")


def bubble(mydict, func):
    d_items = list(mydict.items())
    for j in range(len(d_items) - 1):
        for i in range(len(d_items) - 1):
            d_items[i], d_items[i + 1] = func(d_items[i], d_items[i + 1])
    return d_items


def sort_framework_versions():
    for f in frameworks:
        if f == unityKey:
            frameworks[f][versionKey] = dict(bubble(frameworks[f][versionKey], sort_unity_framework_versions))
        else:
            frameworks[f][versionKey] = dict(bubble(frameworks[f][versionKey], sort_other_frameworks_versions))


################# DRAW AND SAVE GRAPHS #################


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


def draw_graphs(filepath):
    # found implementations
    implementationKeys = [androidKey]
    try:
        implementations = [android[implementationFound]]
        for f in frameworks:
            implementationKeys.append(f)
            implementations.append(frameworks[f][implementationFound])

        x = range(len(implementations))

        plt.figure(figsize=(8, 5))
        plt.bar(x, list([totalEntries] * len(implementations)), label=f'Total apps = {totalEntries}', color=(0.2, 0.4, 0.6, 0.6))
        bars = plt.bar(x, list(implementations), label='Implementation found')
        plt.xticks(x, list(implementationKeys))
        plt.margins(x=0)
        plt.yticks(np.arange(0, totalEntries + 1, step=math.ceil(totalEntries / 20)))
        plt.legend()
        plt.tight_layout()

        # add percentages above the bars
        for rect in bars:
            height = rect.get_height()
            plt.text(rect.get_x() + rect.get_width() / 2.0, height,
                     f'{height / totalEntries * 100:.0f}%', ha='center', va='bottom')

        plt.savefig(f"{filepath}_implementations.png")
    except:
        print("Failed to plot 1 graph for found Android API and frameworks.")

    # android api
    try:
        for a in android:
            if a == minSdkKey or a == targetSdkKey or a == compileSdkKey:
                x = range(len(android[a]))
                if not android[a].values():
                    highest = 1
                else:
                    highest = max(android[a].values())

                plt.figure(figsize=(15, 5))
                bars = plt.bar(x, list(android[a].values()))
                plt.xticks(x, list(android[a].keys()))
                plt.margins(x=0)
                plt.yticks(np.arange(0, highest + 1, step=math.ceil(highest / 20)))
                plt.title(f"Found Android API {a}")

                # add counts above the bars
                for rect in bars:
                    height = rect.get_height()
                    if height != 0:
                        plt.text(rect.get_x() + rect.get_width() / 2.0, height, f'{height:.0f}', ha='center',
                                 va='bottom')

                plt.savefig(f"{filepath}_{a}.png")
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

        plt.figure(figsize=(8, 5))
        plt.bar(x, frameworkImpl, width=0.9, label='Implementation found', color=(0.2, 0.4, 0.6, 0.6))
        barsInit = plt.bar(x - width, frameworkInit, width=width, label='Version found by initial methods',
                           color='green')
        barsDate = plt.bar(x, frameworkDate, width=width, label='Version found by date', color='orange')
        barsNotFound = plt.bar(x + width, frameworkNotFound, width=width, label='Version not found', color='red')
        plt.xticks(range(len(frameworks)), frameworkNames)
        plt.margins(x=0)
        plt.yticks(np.arange(0, highest + 1, step=math.ceil(highest / 20)))
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

        plt.savefig(f"{filepath}_framework_versions.png")
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

            plt.figure(figsize=(15, 5))
            plt.bar(x, list(frameworkVersion.values()))
            a = plt.gca()
            plt.xticks(x, list(frameworkVersion.keys()))
            plt.margins(x=0)
            plt.yticks(np.arange(0, highest + 1, step=math.ceil(highest / 20)))

            # show only nth label in x-axis
            i = 0
            if totalEntries > 1000:
                for label in a.xaxis.get_ticklabels():
                    if f == unityKey:
                        if i % 70 != 0:
                            label.set_visible(False)
                    elif f == reactNativeKey or f == qtKey:
                        if i % 10 != 0:
                            label.set_visible(False)
                    elif f == flutterKey:
                        if i % 3 != 0:
                            label.set_visible(False)
                    else:
                        if i % 5 != 0:
                            label.set_visible(False)
                    i += 1

            plt.title("{} versions found".format(f))
            plt.savefig(f"{filepath}_{f}.png")
    except:
        print("Failed to plot 6 graphs for found frameworks' versions.")

    # vulnerabilities
    try:
        col1 = '# apps with found versions'
        col2 = 'Total vulnerabilities'
        col3 = 'Mean # of vulnerabilities per app'

        aFound = android[implementationFound]
        aVul = android[vulnerabilitiesKey]
        if aVul == 0 or aFound == 0:
            aMean = 0
        else:
            aMean = round(aVul / aFound)
        foundAndVuls = {col1: [aFound], col2: [aVul], col3: [aMean]}

        for f in frameworks:
            found = frameworks[f][versionFoundInitial] + frameworks[f][versionFoundByDate]
            vul = frameworks[f][vulnerabilitiesKey]

            foundAndVuls[col1].append(found)
            foundAndVuls[col2].append(vul)
            if vul == 0 or found == 0:
                foundAndVuls[col3].append(0)
            else:
                foundAndVuls[col3].append(round(vul / found))

        # generate the table
        df = pd.DataFrame(foundAndVuls)
        df.index = implementationKeys
        pd.set_option("display.max_columns", None)
        print("Found versions and total # of vulnerabilities:\n", df, "\n")
        ax = plt.subplot(111, frame_on=False)  # no visible frame
        ax.xaxis.set_visible(False)
        ax.yaxis.set_visible(False)
        table(ax, df, loc='center')
        plt.title("The number of apps with found framework versions and the total number of the frameworks' "
                  "vulnerabilities")
        plt.savefig(f"{filepath}_vulnerability_table.png")

        # generate the pie chart
        sizes = [foundAndVuls[col3][0], sum(foundAndVuls[col3][1:])]
        plt.figure(figsize=(15, 5))
        plt.pie(sizes, labels=[androidKey, frameworkKey], autopct='%1.1f%%')
        plt.title("The ratio of Android and frameworks' vulnerabilities found in one app")
        plt.savefig(f"{filepath}_vulnerability_chart.png")

    except:
        print("Failed to get vulnerability statistics")

    save_graphs(filepath + "_evaluation.pdf")


################# HELPER FUNCTION #################


def increment_dict(myDict, key):
    if not myDict.get(key):
        myDict[key] = 1
    else:
        myDict[key] += 1


def increment_dict_by_n(myDict, key, n):
    if not myDict.get(key):
        myDict[key] = n
    else:
        myDict[key] += n


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


################# OUTPUT PICKLE and JSON DATA #################


def pickle_data(pickleFile, jsonFile):
    # pickle data to a file
    try:
        with open(pickleFile, 'wb') as handle:
            pickle.dump({"Total apps": totalEntries, androidKey: android, frameworkKey: frameworks}, handle,
                        protocol=pickle.HIGHEST_PROTOCOL)
    except:
        print(f"Failed to write the data to the pickle file {pickleFile}.")

    # write data to json
    try:
        jsonObject = json.dumps({"Total apps": totalEntries, androidKey: android, frameworkKey: frameworks},
                                sort_keys=True, indent=4)
        with open(jsonFile, "w") as outfile:
            outfile.write(jsonObject)
    except:
        print(f"Failed to write the data to the JSON file {jsonFile}.")


################# SECOND MAIN FUNCTION #################


# to draw the graphs if given a previous output JSON file from this evaluation script
def from_json_output(filepath):
    global totalEntries
    global android
    global frameworks

    jsonData = open(filepath, 'r')
    data = json.load(jsonData)

    totalEntries = data["Total apps"]
    android = data[androidKey]
    frameworks = data[frameworkKey]


################# MAIN FUNCTION #################


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise Exception(f'Please specify the flag and the path to the file containing the JSON filepaths!'
                        f'Expected 2 argument, but was {len(sys.argv) - 1}.\n'
                        f'Make sure there are no spaces in the path.\n'
                        f'Expected: -i $filepath (input from tool) or -o $filepath (input from the output JSON of this '
                        f'evaluation script).')

    path = Path(sys.argv[2]).parent
    file = Path(sys.argv[2]).stem

    # input from tool
    if sys.argv[1] == "-i":
        evaluation_graphs(sys.argv[2])
        # draw_graphs(str(path) + "/" + str(file))

        pickleFile = str(path) + "/" + str(file) + "_evaluation.p"
        jsonFile = str(path) + "/" + str(file) + "_evaluation.json"
        pickle_data(pickleFile, jsonFile)
    # input from the output JSON of this evaluation script
    elif sys.argv[1] == "-o":
        from_json_output(sys.argv[2])
        draw_graphs(str(path) + "/" + str(file))
