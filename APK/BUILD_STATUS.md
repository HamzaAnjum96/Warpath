# APK build status

Expected output filename: `APK/Warpath_v0_0_1.apk`

This repository now includes a minimal Android "Hello, Warpath!" app scaffold and build script.
In this execution environment, Android repositories are blocked by proxy (HTTP 403), so Android Gradle Plugin / SDK dependencies could not be fetched and a real APK could not be produced here.

To build locally on a machine with Android SDK + network access:

```bash
./scripts/build_apk.sh
```
