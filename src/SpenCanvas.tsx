import React from "react";
import {
  Platform,
  View,
  type NativeSyntheticEvent,
  type ViewProps,
} from "react-native";
import * as RequireNativeComponentModule from "react-native/Libraries/ReactNative/requireNativeComponent";

export type SpenCanvasPoint = {
  x: number;
  y: number;
  pressure: number;
  tilt: number;
  hoverDistance: number;
  timestamp: number;
  action: "down" | "move" | "up" | "cancel";
  toolType: "stylus" | "eraser";
};

export type SpenCanvasProps = ViewProps & {
  inkColor?: string;
  minStrokeWidth?: number;
  maxStrokeWidth?: number;
  clearToken?: number;
  onDraw?: (point: SpenCanvasPoint) => void;
};

type NativeCanvasProps = Omit<SpenCanvasProps, "onDraw"> & {
  onSpenDraw?: (event: NativeSyntheticEvent<SpenCanvasPoint>) => void;
};

// The separate NativeComponent spec generates Fabric metadata and the Android manager delegate.
const requireNativeComponent = (
  RequireNativeComponentModule as unknown as {
    default: <T>(name: string) => React.ComponentType<T>;
  }
).default;
const NativeSpenCanvas = requireNativeComponent<NativeCanvasProps>("SpenCanvas");

export function SpenCanvas({ onDraw, ...props }: SpenCanvasProps) {
  if (Platform.OS !== "android") {
    return <View {...props} />;
  }

  return <NativeSpenCanvas {...props} onSpenDraw={(event) => onDraw?.(event.nativeEvent)} />;
}
