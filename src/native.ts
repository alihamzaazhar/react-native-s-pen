import { NativeEventEmitter, NativeModules, Platform } from "react-native";
import NativeSpenModule from "./specs/NativeSpenModule";
import type { Spec as NativeSpenModuleSpec } from "./specs/NativeSpenModule";
import type { SpenEventPayload } from "./types";

const LINKING_ERROR =
  `The package 'react-native-s-pen' doesn't seem to be linked. ` +
  Platform.select({ ios: "This library only supports Android.", default: "Run the native build step again." });

export function requireNativeModule(): NativeSpenModuleSpec {
  if (Platform.OS !== "android") {
    throw new Error(LINKING_ERROR);
  }

  const legacyModule = NativeModules.SpenModule as NativeSpenModuleSpec | undefined;
  const nativeModule = NativeSpenModule ?? legacyModule;

  if (!nativeModule) {
    throw new Error(LINKING_ERROR);
  }

  return nativeModule;
}

export function createEventEmitter() {
  return new NativeEventEmitter(requireNativeModule());
}

export type { SpenEventPayload };
