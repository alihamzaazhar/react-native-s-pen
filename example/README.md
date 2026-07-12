# Example App

This folder contains a minimal React Native screen that subscribes to `SPen` events.

## What to do

1. Use Node `22.13.0` or newer for Metro and RN `0.86.0`.
2. Run `npm install` from the `example/` folder after linking this library locally.
3. The library postinstall step copies the bundled Samsung SDK jars into [`example/android/app/src/main/libs`](/Users/alihamza/Documents/Samsung%20S-Pen/example/android/app/src/main/libs).
4. Launch the app with `npm run android` or install the debug APK after running `./gradlew assembleDebug`.
5. Test on a Samsung device and enable Air actions in `Settings > Advanced features > S Pen > Air actions`.
