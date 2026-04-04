# Warpath — Build Instructions for Claude Code Sessions

Every Claude Code web session starts with a clean container that has **no Android SDK**.
Run the steps below at the start of any session where you need to compile or build an APK.
The whole setup takes ~2 minutes.

---

## Quick-start (copy-paste block)

```bash
# ── 1. Set paths ───────────────────────────────────────────────────────────
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/opt/android-sdk

# ── 2. Download Android platform 35 ────────────────────────────────────────
cd /tmp
curl -sL "https://dl.google.com/android/repository/platform-35-ext14_r01.zip" -o platform35.zip
unzip -q platform35.zip
mkdir -p $ANDROID_HOME/platforms
mv android-35 $ANDROID_HOME/platforms/android-35

# ── 3. Download build-tools r34 ─────────────────────────────────────────────
curl -sL "https://dl.google.com/android/repository/build-tools_r34-linux.zip" -o bt34.zip
unzip -q bt34.zip
mkdir -p $ANDROID_HOME/build-tools
mv android-14 $ANDROID_HOME/build-tools/34.0.0

# ── 4. Patch platform metadata (ext14 zip is missing proper descriptors) ────
cat > $ANDROID_HOME/platforms/android-35/source.properties << 'EOF'
Pkg.Desc=Android SDK Platform 15
Pkg.UserSrc=false
Platform.Version=15
Platform.CodeName=
Pkg.Revision=1
AndroidVersion.ApiLevel=35
AndroidVersion.IsBaseSdk=true
Layoutlib.Api=15
Layoutlib.Revision=1
Platform.MinToolsRev=22
EOF

cat > $ANDROID_HOME/platforms/android-35/package.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02" xmlns:ns11="http://schemas.android.com/sdk/android/repo/repository2/03">
<license id="android-sdk-license" type="text">Terms</license>
<localPackage path="platforms;android-35" obsolete="false">
<type-details xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns11:platformDetailsType">
<api-level>35</api-level><base-extension>true</base-extension><layoutlib api="15"/>
</type-details>
<revision><major>1</major></revision>
<display-name>Android SDK Platform 35</display-name>
<uses-license ref="android-sdk-license"/>
</localPackage>
</ns2:repository>
EOF

# ── 5. Point the project at the SDK ────────────────────────────────────────
echo "sdk.dir=$ANDROID_HOME" > /home/user/Warpath/local.properties

# ── 6. Build ────────────────────────────────────────────────────────────────
cd /home/user/Warpath
gradle assembleDebug
```

---

## Copy the APK

After a successful build, copy the output to the `APK/` folder using the next version number:

```bash
cp app/build/outputs/apk/debug/app-debug.apk APK/Warpath_vX_X_X.apk
```

Current APK history:
| File | Description |
|------|-------------|
| `Warpath_v0_0_1.apk` | Initial prototype |
| `Warpath_v0_5_0.apk` | Touch-first prototype (battle + campaign) |
| `Warpath_v0_6_0.apk` | Live map movement + UI upgrade |

---

## Notes & gotchas

| Issue | Fix |
|-------|-----|
| `JAVA_HOME` not found | Java is at `/usr/lib/jvm/java-21-openjdk-amd64` — use that, **not** the `mise` path |
| `SDK location not found` | `local.properties` is git-ignored; re-create it every session with `echo "sdk.dir=..." > local.properties` |
| `platform-35-ext14` zip extracts to `android-35`, not `platforms/android-35` | The `mv` in step 3 handles this |
| `build-tools_r34` zip extracts to `android-14` | The `mv` in step 3 handles this |
| Gradle uses a daemon from a previous run | Run `gradle --stop` then retry if the build hangs |
| Deprecation warnings about `systemUiVisibility` | These are pre-existing warnings in untouched files — safe to ignore |

---

## Environment details (verified 2026-04-04)

- **Java**: OpenJDK 21.0.10 at `/usr/lib/jvm/java-21-openjdk-amd64`
- **Gradle**: 8.14.3 (system install at `/usr/local/gradle`)
- **Android platform**: 35 (ext14)
- **Build-tools**: 34.0.0
- **minSdk**: 24 | **targetSdk**: 35
