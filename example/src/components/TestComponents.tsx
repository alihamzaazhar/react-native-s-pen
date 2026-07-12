import React from "react";
import { ScrollView, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { styles } from "../styles";

export function Metric({ label, value, good }: { label: string; value: unknown; good?: boolean }) {
  const displayValue = value === null || value === undefined ? "waiting" : String(value);
  return (
    <View style={styles.metric}>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={[styles.metricValue, good === true && styles.good, good === false && styles.bad]}>{displayValue}</Text>
    </View>
  );
}

export function Instructions({ children }: { children: React.ReactNode }) {
  return (
    <View style={styles.instructions}>
      <Text style={styles.instructionsLabel}>TEST</Text>
      <Text style={styles.instructionsText}>{children}</Text>
    </View>
  );
}

export function ScreenFrame({ children, scrollEnabled = true }: { children: React.ReactNode; scrollEnabled?: boolean }) {
  return (
    <SafeAreaView style={styles.root} edges={["bottom", "left", "right"]}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false} scrollEnabled={scrollEnabled}>
        {children}
      </ScrollView>
    </SafeAreaView>
  );
}
