import React from "react";
import { Text, View } from "react-native";
import { Instructions, Metric, ScreenFrame } from "../components/TestComponents";
import { useSpenTest } from "../context/SpenTestContext";
import { styles } from "../styles";

export function AirMotionScreen() {
  const { lastAirMotion, airMotionEnabled, connection } = useSpenTest();
  return (
    <ScreenFrame>
      <Instructions>Remove the charged pen and move it in the air. If no values arrive, repeat while holding the side button.</Instructions>
      <View style={styles.motionStage}>
        <Text style={styles.motionX}>{lastAirMotion?.point?.x?.toFixed(4) ?? "0.0000"}</Text>
        <Text style={styles.motionDivider}>X / Y</Text>
        <Text style={styles.motionY}>{lastAirMotion?.point?.y?.toFixed(4) ?? "0.0000"}</Text>
      </View>
      <View style={styles.card}>
        <Metric label="Feature enabled" value={airMotionEnabled} good={airMotionEnabled === true} />
        <Metric label="Framework" value={connection} good={connection === "connected"} />
        <Metric label="Last event" value={lastAirMotion ? new Date(lastAirMotion.timestamp).toLocaleTimeString() : null} />
      </View>
    </ScreenFrame>
  );
}
