import React from "react";
import { Pressable, Text, View } from "react-native";
import { ScreenFrame } from "../components/TestComponents";
import { useSpenTest } from "../context/SpenTestContext";
import { styles } from "../styles";

export function EventsScreen() {
  const { events, clearEvents } = useSpenTest();
  return (
    <ScreenFrame>
      <View style={styles.actions}>
        <Text style={styles.note}>{events.length} most recent events</Text>
        <Pressable style={styles.clearButton} onPress={clearEvents}><Text style={styles.clearText}>Clear</Text></Pressable>
      </View>
      {events.length === 0 ? <Text style={styles.empty}>No events received yet.</Text> : events.map((event, index) => (
        <View key={`${event.timestamp}-${index}`} style={styles.eventRow}>
          <Text style={styles.eventName}>{event.name}</Text>
          <Text style={styles.eventDetail}>{JSON.stringify(event)}</Text>
        </View>
      ))}
    </ScreenFrame>
  );
}
