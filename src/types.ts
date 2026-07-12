export type SpenCapability =
  | "stylusInput"
  | "hover"
  | "button"
  | "airActions"
  | "airCommand"
  | "pressure";

export type SpenRemoteFeature = "button" | "airMotion";

export type SpenInsertionState = "inserted" | "detached" | "unknown";

export type SpenConnectionState =
  | "connected"
  | "disconnected"
  | "disconnectedByUnknownReason"
  | "connectionFailed"
  | "unsupportedDevice"
  | "unknown";

export type SpenErrorCode =
  | "unsupportedDevice"
  | "connectionFailed"
  | "unknown";

export type SpenDeviceInfo = {
  platform: "android";
  manufacturer: string;
  brand: string;
  model: string;
  sdkInt: number;
  isSamsungDevice: boolean;
  capabilities: SpenCapability[];
  samsungSdk?: {
    available: boolean;
    versionCode?: number;
    versionName?: string;
    connected?: boolean;
    featureFlags?: Partial<Record<SpenRemoteFeature, boolean>>;
  };
};

export type SpenEventName =
  | "spen-hover"
  | "spen-button"
  | "spen-down"
  | "spen-move"
  | "spen-up"
  | "spen-status"
  | "spen-insertion-state"
  | "spen-connection-state"
  | "spen-error"
  | "spen-air-action";

export type SpenPoint = {
  x: number;
  y: number;
  pressure?: number;
  /** Raw device-specific distance from the display during hover events. */
  hoverDistance?: number;
  tilt?: number;
  tiltX?: number;
  tiltY?: number;
  toolType?: "stylus" | "finger" | "mouse" | "unknown";
};

export type SpenEventPayload = {
  name: SpenEventName;
  timestamp: number;
  point?: SpenPoint;
  action?: string;
  buttonState?: number;
  connectionState?: SpenConnectionState;
  insertionState?: SpenInsertionState;
  errorCode?: SpenErrorCode;
  raw?: Record<string, unknown>;
};

export type SpenListener = (event: SpenEventPayload) => void;
