import React from "react";
import { Text, View } from "react-native";
import { Instructions, ScreenFrame } from "../components/TestComponents";
import { useSpenTest } from "../context/SpenTestContext";
import { styles } from "../styles";

export function InsertionScreen() {
  const { insertion } = useSpenTest();
  return (
    <ScreenFrame>
      <Instructions>Remove the S Pen from its silo, wait for detached, then insert it and wait for inserted.</Instructions>
      <View style={[styles.stateStage, insertion === "inserted" ? styles.stateInserted : styles.stateDetached]}>
        <Text style={styles.stateLabel}>PHYSICAL PEN</Text>
        <Text style={styles.stateValue}>{insertion}</Text>
      </View>
      <Text style={styles.note}>Unsupported devices report unknown. This is separate from the S Pen Framework connection.</Text>
    </ScreenFrame>
  );
}
