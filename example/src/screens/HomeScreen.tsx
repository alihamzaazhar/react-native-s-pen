import React from "react";
import { Pressable, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { Metric, ScreenFrame } from "../components/TestComponents";
import { useSpenTest } from "../context/SpenTestContext";
import type { RootStackParamList, TestRoute } from "../navigation/types";
import { styles } from "../styles";

const tests: Array<{ screen: TestRoute; title: string; description: string }> = [
  { screen: "connection", title: "Framework connection", description: "SDK availability, feature support, and connection state" },
  { screen: "insertion", title: "Insert and remove", description: "Physical S Pen silo state" },
  { screen: "hover", title: "Hover and tilt", description: "Coordinates, hover distance, and pen angle" },
  { screen: "drawing", title: "Drawing pressure", description: "Draw a pressure-sensitive trail" },
  { screen: "button", title: "Remote button", description: "Air Actions button down and up" },
  { screen: "airMotion", title: "Air Motion", description: "Remote X/Y movement deltas" },
  { screen: "events", title: "Raw event log", description: "Inspect every event emitted by the library" },
];

export function HomeScreen({ navigation }: NativeStackScreenProps<RootStackParamList, "home">) {
  const { connection, insertion } = useSpenTest();
  return (
    <ScreenFrame>
      <View style={styles.hero}>
        <Text style={styles.eyebrow}>REACT NATIVE S PEN</Text>
        <Text style={styles.title}>Hardware test lab</Text>
        <Text style={styles.subtitle}>Run each test independently and verify the native event channel on real Samsung hardware.</Text>
      </View>
      <View style={styles.summary}>
        <Metric label="Framework" value={connection} good={connection === "connected"} />
        <Metric label="Physical pen" value={insertion} good={insertion !== "unknown"} />
      </View>
      {tests.map((test, index) => (
        <Pressable key={test.screen} style={styles.testRow} onPress={() => navigation.navigate(test.screen)}>
          <Text style={styles.testIndex}>{String(index + 1).padStart(2, "0")}</Text>
          <View style={styles.testCopy}>
            <Text style={styles.testTitle}>{test.title}</Text>
            <Text style={styles.testDescription}>{test.description}</Text>
          </View>
          <Text style={styles.arrow}>›</Text>
        </Pressable>
      ))}
    </ScreenFrame>
  );
}
