import React, { useState } from "react";
import { Pressable, Text, View } from "react-native";
import { SpenCanvas } from "react-native-s-pen";
import type { SpenCanvasPoint } from "react-native-s-pen";
import { Instructions, Metric, ScreenFrame } from "../components/TestComponents";
import { styles } from "../styles";

export function DrawingScreen() {
  const [sampleCount, setSampleCount] = useState(0);
  const [lastPoint, setLastPoint] = useState<SpenCanvasPoint | null>(null);
  const [lastPressure, setLastPressure] = useState(0);
  const [peakPressure, setPeakPressure] = useState(0);
  const [clearToken, setClearToken] = useState(0);

  const clear = () => { setClearToken((token) => token + 1); setSampleCount(0); setLastPoint(null); setLastPressure(0); setPeakPressure(0); };
  const onDraw = (point: SpenCanvasPoint) => {
    setLastPoint(point);
    setSampleCount((count) => count + 1);
    if (point.pressure > 0) { setLastPressure(point.pressure); setPeakPressure((current) => Math.max(current, point.pressure)); }
  };

  return (
    <ScreenFrame scrollEnabled={false}>
      <Instructions>Draw inside the panel. Press lightly and firmly; thicker ink represents greater pressure.</Instructions>
      <SpenCanvas style={styles.drawingArea} inkColor="#11140F" minStrokeWidth={2} maxStrokeWidth={18} clearToken={clearToken} onDraw={onDraw} />
      <View style={styles.actions}>
        <Text style={styles.note}>{sampleCount} native samples</Text>
        <Pressable style={styles.clearButton} onPress={clear}><Text style={styles.clearText}>Clear</Text></Pressable>
      </View>
      <View style={styles.card}>
        <Metric label="Last contact pressure" value={lastPressure.toFixed(4)} good={lastPressure > 0} />
        <Metric label="Peak pressure" value={peakPressure.toFixed(4)} good={peakPressure > 0} />
        <Metric label="Tilt" value={lastPoint?.tilt.toFixed(4)} />
        <Metric label="Tool" value={lastPoint?.toolType} good={lastPoint?.toolType === "stylus"} />
        <Metric label="Action" value={lastPoint?.action} />
      </View>
    </ScreenFrame>
  );
}
