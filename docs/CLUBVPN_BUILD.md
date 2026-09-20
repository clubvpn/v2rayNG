# ClubVPN build and upgrade guide

This fork tracks a tagged upstream v2rayNG release and adds one ClubVPN
integration: the app creates a `club` subscription on first launch. The
subscription URL is kept as the literal `REPLACE` placeholder in source and
must be injected into the APK before signing.

## Upgrade to a newer v2rayNG tag

Start from a clean checkout of the desired upstream tag. Keep the upstream
repository as `origin` and add the ClubVPN repository as a second remote:

```sh
git clone --branch <upstream-tag> --depth 1 https://github.com/2dust/v2rayNG.git
cd v2rayNG
git submodule update --init --recursive
git remote add clubvpn https://github.com/clubvpn/v2rayNG.git
```

Re-apply the ClubVPN changes from this branch:

1. Add `SubscriptionItem` and `MmkvManager` imports to
   `V2rayNG/app/src/main/java/com/v2ray/ang/AngApplication.kt`.
2. Add `CLUB_SUBSCRIPTION_REMARKS = "club"`.
3. Call `ensureClubVpnSubscription()` after application settings initialize.
4. Implement the idempotent subscription creation shown in this branch.
5. Add `club_subscription_url` with value `REPLACE` to
   `V2rayNG/app/src/main/res/values/strings.xml`.

Check the release's APIs before applying the patch. In particular, confirm
that `MmkvManager.decodeSubscriptions()`, `MmkvManager.encodeSubscription()`
and the `SubscriptionItem` constructor still have compatible signatures.

## Build the APK in Docker

Do not install the Android SDK on the host. Use a disposable Docker container
with the Android SDK, NDK, and Gradle wrapper inside it. The upstream GitHub
workflow is the reference for the required versions (`compile-hevtun.sh`,
NDK `29.0.14206865`, Android API 37, and the matching `libv2ray.aar`). Build a
universal debug template for the repackaging workflow:

```sh
./gradlew :app:assemblePlaystoreDebug --no-daemon --no-watch-fs --max-workers=1 \
  -Pkotlin.compiler.execution.strategy=in-process \
  -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8'
```

Copy `V2rayNG/app/build/outputs/apk/playstore/debug/v2rayNG_<version>_universal.apk`
to the ClubVPN build repository as the versioned template asset. Remove
generated `libs/`, `V2rayNG/app/libs/`, `V2rayNG/app/build/`, and Gradle caches
after copying the artifact.

## Inject the subscription and sign

The ClubVPN workflow in `apk-build/.github/workflows/build_apk.yml` performs
these steps:

```sh
java -jar assets/apktool_2.9.3.jar d assets/club_v2ray_<version>.apk -s -o decompiled_files
sed -i 's|REPLACE|<subscription-url>|g' decompiled_files/res/values/strings.xml
java -jar assets/apktool_2.9.3.jar b decompiled_files
java -jar assets/uber-apk-signer-1.3.0.jar \
  -a decompiled_files/dist/club_v2ray_<version>.apk --out final/
mv final/club_v2ray_<version>-aligned-debugSigned.apk final/vpn.apk
```

Always verify that the injected URL appears in the decoded XML, the rebuilt
APK passes `unzip -t`, all four ABIs are present, and the final APK is signed.
Do not commit `decompiled_files/`, `final/`, or generated Gradle/native build
outputs.
