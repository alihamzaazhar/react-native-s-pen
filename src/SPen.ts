import { Platform } from "react-native";
import { createEventEmitter, requireNativeModule } from "./native";
import type {
  SpenConnectionState,
  SpenDeviceInfo,
  SpenEventPayload,
  SpenInsertionState,
  SpenListener,
  SpenRemoteFeature,
} from "./types";

class SPenManager {
  private eventSubscriptions = new Set<{ remove: () => void }>();
  private emitter: ReturnType<typeof createEventEmitter> | null = null;
  private listenerCount = 0;

  private getEmitter() {
    if (!this.emitter) {
      this.emitter = createEventEmitter();
    }

    return this.emitter;
  }

  async isSupported(): Promise<boolean> {
    if (Platform.OS !== "android") {
      return false;
    }

    return requireNativeModule().isSupported();
  }

  async getDeviceInfo(): Promise<SpenDeviceInfo> {
    if (Platform.OS !== "android") {
      throw new Error("react-native-s-pen only supports Android.");
    }

    return requireNativeModule().getDeviceInfo();
  }

  async getVersionCode(): Promise<number> {
    if (Platform.OS !== "android") {
      return 0;
    }

    return requireNativeModule().getVersionCode();
  }

  async getVersionName(): Promise<string> {
    if (Platform.OS !== "android") {
      return "";
    }

    return requireNativeModule().getVersionName();
  }

  async getPenInsertionState(): Promise<SpenInsertionState> {
    if (Platform.OS !== "android") {
      return "unknown";
    }

    return requireNativeModule().getPenInsertionState();
  }

  async isConnected(): Promise<boolean> {
    if (Platform.OS !== "android") {
      return false;
    }

    return requireNativeModule().isConnected();
  }

  async isFeatureEnabled(feature: SpenRemoteFeature): Promise<boolean> {
    if (Platform.OS !== "android") {
      return false;
    }

    return requireNativeModule().isFeatureEnabled(feature);
  }

  async startListening(): Promise<void> {
    if (Platform.OS !== "android") {
      return;
    }

    await requireNativeModule().startListening();
  }

  async stopListening(): Promise<void> {
    if (Platform.OS !== "android") {
      return;
    }

    await requireNativeModule().stopListening();
  }

  addListener(listener: SpenListener) {
    const subscription = this.getEmitter().addListener("SpenEvent", (event: SpenEventPayload) => {
      listener(event);
    });

    this.eventSubscriptions.add(subscription);
    this.listenerCount += 1;
    void this.startListening();

    return () => {
      subscription.remove();
      this.eventSubscriptions.delete(subscription);
      this.listenerCount = Math.max(0, this.listenerCount - 1);

      if (this.listenerCount === 0) {
        void this.stopListening();
      }
    };
  }

  addConnectionStateListener(listener: (state: SpenConnectionState) => void) {
    let receivedLiveState = false;
    const subscription = this.getEmitter().addListener("SpenEvent", (event: SpenEventPayload) => {
      if (event.name === "spen-connection-state" && event.connectionState) {
        receivedLiveState = true;
        listener(event.connectionState);
      }
    });

    this.eventSubscriptions.add(subscription);
    this.listenerCount += 1;
    void this.startListening()
      .then(() => this.isConnected())
      .then((connected) => {
        if (connected && !receivedLiveState && this.eventSubscriptions.has(subscription)) {
          listener("connected");
        }
      });

    return () => {
      subscription.remove();
      this.eventSubscriptions.delete(subscription);
      this.listenerCount = Math.max(0, this.listenerCount - 1);

      if (this.listenerCount === 0) {
        void this.stopListening();
      }
    };
  }

  addPenInsertionStateListener(listener: (state: SpenInsertionState) => void) {
    const subscription = this.getEmitter().addListener("SpenEvent", (event: SpenEventPayload) => {
      if (event.name === "spen-insertion-state" && event.insertionState) {
        listener(event.insertionState);
      }
    });

    this.eventSubscriptions.add(subscription);
    this.listenerCount += 1;
    void this.startListening();

    return () => {
      subscription.remove();
      this.eventSubscriptions.delete(subscription);
      this.listenerCount = Math.max(0, this.listenerCount - 1);

      if (this.listenerCount === 0) {
        void this.stopListening();
      }
    };
  }

  removeAllListeners() {
    for (const subscription of this.eventSubscriptions) {
      subscription.remove();
    }
    this.eventSubscriptions.clear();
    this.listenerCount = 0;
    void this.stopListening();
  }
}

export const SPen = new SPenManager();
