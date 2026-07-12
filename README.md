# react-native-s-pen

React Native 0.86+ library for Samsung S Pen input, built with TurboModules and a Fabric-compatible native canvas.

## Features

- Stylus down, move, up, and hover events
- Contact pressure, hover distance, and tilt
- Pressure-sensitive native `SpenCanvas`
- Samsung Remote button events
- Samsung Air Motion X/Y deltas
- S Pen Framework connection state
- Best-effort physical insertion/removal state on compatible Samsung phones
- Typed TypeScript API
- Android New Architecture support

## Requirements

- React Native `0.86.0` or newer
- React `19.2.3` or newer
- Android API 24+
- A Samsung device for Samsung-specific Remote features
- S Pen Remote SDK `1.0.x` for button and Air Motion support

Generic stylus events and `SpenCanvas` use Android `MotionEvent`. Remote button and Air Motion events require Samsung's SDK.

## Installation

```sh
npm install react-native-s-pen
```

The Samsung SDK is not available from Maven and its downloaded binaries cannot be redistributed by this repository. Sign in to [Samsung Developer](https://developer.samsung.com/galaxy-spen-remote/s-pen-remote-sdk.html), download the S Pen Remote SDK, and place these files in your application:

```text
android/app/src/main/libs/sdk-v1.0.0.jar
android/app/src/main/libs/spenremote-v1.0.1.jar
```

Add the local SDK directory to `android/app/build.gradle`:

```gradle
dependencies {
  implementation fileTree(dir: "src/main/libs", include: ["*.jar", "*.aar"])
}
```

Rebuild the Android application after adding the JARs.

## Basic usage

```ts
import { SPen } from "react-native-s-pen";

const supported = await SPen.isSupported();
const deviceInfo = await SPen.getDeviceInfo();
const insertionState = await SPen.getPenInsertionState();
const connected = await SPen.isConnected();

const removeEvents = SPen.addListener((event) => {
  console.log(event.name, event.point, event.action);
});

const removeConnection = SPen.addConnectionStateListener((state) => {
  console.log("S Pen Framework:", state);
});

const removeInsertion = SPen.addPenInsertionStateListener((state) => {
  console.log("Physical pen:", state);
});

// Call each returned function when the consuming component unmounts.
```

## Native canvas

`SpenCanvas` owns Android touch dispatch, so it works inside React Navigation and scroll views.

```tsx
import { useState } from "react";
import { Button, View } from "react-native";
import { SpenCanvas } from "react-native-s-pen";

export function SignatureScreen() {
  const [clearToken, setClearToken] = useState(0);

  return (
    <View>
      <SpenCanvas
        style={{ height: 400 }}
        inkColor="#11140f"
        minStrokeWidth={2}
        maxStrokeWidth={18}
        clearToken={clearToken}
        onDraw={(point) => {
          console.log(point.x, point.y, point.pressure, point.tilt);
        }}
      />
      <Button title="Clear" onPress={() => setClearToken((value) => value + 1)} />
    </View>
  );
}
```

`onDraw` returns `x`, `y`, `pressure`, `tilt`, `hoverDistance`, `timestamp`, `action`, and `toolType`.

## Events

| Event | Meaning |
| --- | --- |
| `spen-down` | Stylus touches the display |
| `spen-move` | Stylus moves while touching |
| `spen-up` | Stylus leaves the display |
| `spen-hover` | Stylus moves above the display |
| `spen-button` | Remote button `down` or `up` |
| `spen-air-action` | Air Motion X/Y delta |
| `spen-insertion-state` | Physical pen `inserted`, `detached`, or `unknown` |
| `spen-connection-state` | Samsung S Pen Framework state |
| `spen-error` | Samsung SDK error |

Pressure is normally zero while hovering. Use `hoverDistance` above the display and `pressure` while touching it.

## Testing Remote features

1. Enable **Settings > Advanced features > S Pen > Air actions**.
2. Charge and remove the S Pen.
3. Keep the application in the foreground.
4. Press and release the side button to receive `spen-button` events.
5. Move the detached pen to receive Air Motion deltas. On some devices, hold the button while moving.

`isConnected()` means the app is connected to Samsung's S Pen Framework. It does not mean that the physical pen is inserted or currently hovering.

## Example app

The `example` directory contains separate screens for:

- Framework and SDK diagnostics
- Physical insertion/removal
- Hover and tilt
- Pressure-sensitive drawing
- Remote button
- Air Motion
- Raw event inspection

After placing your downloaded SDK JARs in `example/android/app/src/main/libs`:

```sh
cd example
npm install
npm start
npm run android
```

## Compatibility notes

- Android only. Non-Android methods return safe fallback values where documented.
- Insertion detection uses Samsung's sticky `com.samsung.pen.INSERT` broadcast. It is not part of the public Remote SDK contract, so unsupported devices return `unknown`.
- Feature availability differs by Samsung model and S Pen generation.

## Development

```sh
npm install
npm run typecheck

cd example/android
./gradlew :app:assembleDebug
```

## Samsung SDK notice

Samsung SDK binaries are proprietary and are not included in this repository. Samsung and S Pen are trademarks of Samsung Electronics. This project is not affiliated with or endorsed by Samsung.
