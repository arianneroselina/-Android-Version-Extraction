# Android Version Extraction

## Introduction
This repository is introduced in my Bachelor thesis with the title "Vulnerability Detection for Android Apps Using Version Information".
This tool automates the version extraction of an Android application (frameworks and languages used) and use this information
to determine the security and privacy vulnerabilities of the app.

Android mobile app development frameworks that are supported by this tool:
1. Flutter
2. React Native
3. Qt
4. Xamarin
5. Unity
6. Apache Cordova

## Usage
Given an APK file path, this tool creates a JSON file stating the versions and the corresponding vulnerability website links.
The JSON file can be found in the same directory as the APK file.
````
sbt
&&
 ~run --apk-filepath C:/path/to/apk/filename.apk 
````

## Other Scripts

#### Write File Hashes
This repository also includes a python script in folder `src/main/hashing/` that can append a new hashed .lib/.dll file 
of the frameworks to the existing tables in `src/files/hashes/..`.
The hash in the tables will later be compared to the one given as an input.
If the hashes match, it means that they have the same version.
This method is used to extract Flutter, Qt, React Native, and Xamarin's versions.

#### Extract Android CVE
A function to extract Android API CVE vulnerability links can also be found in `src/main/cve/`, which adds vulnerability
links into the corresponding csv file in `src/files/cve_links/..`.
The input for this script can be obtained by downloading a CVE page using the "Download Results" button and rename it to
a csv file.
