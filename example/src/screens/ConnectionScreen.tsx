import React from "react";
import { View } from "react-native";
import { Instructions, Metric, ScreenFrame } from "../components/TestComponents";
import { useSpenTest } from "../context/SpenTestContext";
import { styles } from "../styles";

export function ConnectionScreen() {
  const { connection, deviceInfo, buttonEnabled, airMotionEnabled } = useSpenTest();
  return (
    <ScreenFrame>
      <Instructions>Enable Air Actions, then reopen this screen. Framework should connect without a connect button.</Instructions>
      <View style={styles.card}>
        <Metric label="Framework state" value={connection} good={connection === "connected"} />
        <Metric label="SDK available" value={deviceInfo?.samsungSdk?.available} good={deviceInfo?.samsungSdk?.available} />
        <Metric label="SDK connected" value={deviceInfo?.samsungSdk?.connected} good={deviceInfo?.samsungSdk?.connected} />
        <Metric label="Remote button" value={buttonEnabled} good={buttonEnabled === true} />
        <Metric label="Air Motion" value={airMotionEnabled} good={airMotionEnabled === true} />
        <Metric label="SDK version" value={deviceInfo?.samsungSdk?.versionName} />
      </View>
    </ScreenFrame>
  );
}
