# Android Version Extraction

## Introduction
This repository is introduced in my Bachelor thesis with the title "Vulnerability Detection for Android Apps Using Version Information".
This tool automates the version extraction of an Android application (frameworks and languages used) and use this information
to determine the security and privacy vulnerabilities of the app.

Furthermore, this repository also includes a python script in folder `src/main/hashing/` that can append a new hashed .lib file 
of  the frameworks to the existing tables in `src/files/hashes/..`.

## Usage
Given an APK file path, this tool creates a JSON file stating the versions and the corresponding vulnerability website links.
The JSON file can be found in the same directory as the APK file.
````
scalac src/main/scala/extraction/Main.scala
&&
scala src/main/scala/extraction/Main --apk-filepath C:\Bachelorarbeit\FrameworkApps\ReactNativeApps\Apks\reactnative_0_68_0.apk
````