import React from "react";
import { Text, View } from "react-native";
import { Instructions, Metric, ScreenFrame } from "../components/TestComponents";
import { useSpenTest } from "../context/SpenTestContext";
import { styles } from "../styles";

export function ButtonScreen() {
  const { lastButton, buttonEnabled, connection, buttonClickCount, lastButtonDuration } = useSpenTest();
  const state = lastButton?.action === "down" ? "pressed" : lastButton?.action === "up" ? "released" : "waiting";
  return (
    <ScreenFrame>
      <Instructions>Remove the charged pen, keep this app foregrounded, then click and release the side button.</Instructions>
      <View style={[styles.buttonStage, lastButton?.action === "down" && styles.buttonPressed]}>
        <Text style={styles.stateLabel}>REMOTE BUTTON</Text>
        <Text style={styles.stateValue}>{state}</Text>
      </View>
      <View style={styles.card}>
        <Metric label="Feature enabled" value={buttonEnabled} good={buttonEnabled === true} />
        <Metric label="Framework" value={connection} good={connection === "connected"} />
        <Metric label="Button state" value={lastButton?.buttonState} />
        <Metric label="Clicks received" value={buttonClickCount} good={buttonClickCount > 0} />
        <Metric label="Last hold duration" value={lastButtonDuration === null ? null : `${lastButtonDuration.toFixed(0)} ms`} />
        <Metric label="Last timestamp" value={lastButton ? new Date(lastButton.timestamp).toLocaleTimeString() : null} />
      </View>
    </ScreenFrame>
  );
}
