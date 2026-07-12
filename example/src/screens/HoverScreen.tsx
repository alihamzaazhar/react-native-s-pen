import React from "react";
import { Text, View } from "react-native";
import { Instructions, Metric, ScreenFrame } from "../components/TestComponents";
import { useSpenTest } from "../context/SpenTestContext";
import { styles } from "../styles";

export function HoverScreen() {
  const { lastStylusPoint, stylusMode } = useSpenTest();
  return (
    <ScreenFrame>
      <Instructions>Hold the pen above the display and move it around. Use Drawing Pressure to test contact pressure.</Instructions>
      <View style={styles.hoverTarget}>
        <Text style={styles.targetCross}>+</Text>
        <Text style={styles.targetLabel}>{stylusMode.toUpperCase()}</Text>
      </View>
      <View style={styles.card}>
        <Metric label="Interaction" value={stylusMode} good={stylusMode !== "waiting"} />
        <Metric label="X" value={lastStylusPoint?.x?.toFixed(1)} />
        <Metric label="Y" value={lastStylusPoint?.y?.toFixed(1)} />
        <Metric label="Hover distance" value={lastStylusPoint?.hoverDistance?.toFixed(4)} />
        <Metric label="Tilt" value={lastStylusPoint?.tilt?.toFixed(4)} />
        <Metric label="Tool" value={lastStylusPoint?.toolType} good={lastStylusPoint?.toolType === "stylus"} />
      </View>
    </ScreenFrame>
  );
}
