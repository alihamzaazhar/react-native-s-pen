import type {TurboModule} from "react-native";
import {TurboModuleRegistry} from "react-native";

type TurboSpenCapability =
  | "stylusInput"
  | "hover"
  | "button"
  | "airActions"
  | "airCommand"
  | "pressure";

type TurboSpenDeviceInfo = {
  platform: "android";
  manufacturer: string;
  brand: string;
  model: string;
  sdkInt: number;
  isSamsungDevice: boolean;
  capabilities: Array<TurboSpenCapability>;
  samsungSdk?: {
    available: boolean;
    versionCode?: number;
    versionName?: string;
    connected?: boolean;
    featureFlags?: {
      button?: boolean;
      airMotion?: boolean;
    };
  };
};

export interface Spec extends TurboModule {
  isSupported(): Promise<boolean>;
  getDeviceInfo(): Promise<TurboSpenDeviceInfo>;
  getVersionCode(): Promise<number>;
  getVersionName(): Promise<string>;
  getPenInsertionState(): Promise<"inserted" | "detached" | "unknown">;
  isConnected(): Promise<boolean>;
  isFeatureEnabled(feature: "button" | "airMotion"): Promise<boolean>;
  startListening(): Promise<void>;
  stopListening(): Promise<void>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
  addConnectionStateListener(eventName: string): void;
  removeConnectionStateListeners(count: number): void;
}

const NativeSpenModule = TurboModuleRegistry.get<Spec>("SpenModule");

export default NativeSpenModule;
