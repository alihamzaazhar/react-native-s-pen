import React from "react";
import { StatusBar } from "react-native";
import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { SpenTestProvider } from "./src/context/SpenTestContext";
import type { RootStackParamList } from "./src/navigation/types";
import { AirMotionScreen } from "./src/screens/AirMotionScreen";
import { ButtonScreen } from "./src/screens/ButtonScreen";
import { ConnectionScreen } from "./src/screens/ConnectionScreen";
import { DrawingScreen } from "./src/screens/DrawingScreen";
import { EventsScreen } from "./src/screens/EventsScreen";
import { HomeScreen } from "./src/screens/HomeScreen";
import { HoverScreen } from "./src/screens/HoverScreen";
import { InsertionScreen } from "./src/screens/InsertionScreen";
import { colors } from "./src/styles";

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  return (
    <SafeAreaProvider>
      <SpenTestProvider>
        <NavigationContainer>
          <StatusBar barStyle="light-content" backgroundColor={colors.background} />
          <Stack.Navigator
            screenOptions={{
              headerStyle: { backgroundColor: colors.background },
              headerTintColor: colors.accent,
              headerTitleStyle: { color: colors.text, fontWeight: "700" },
              contentStyle: { backgroundColor: colors.background },
              animation: "slide_from_right",
            }}
          >
            <Stack.Screen name="home" component={HomeScreen} options={{ headerShown: false }} />
            <Stack.Screen name="connection" component={ConnectionScreen} options={{ title: "Framework connection" }} />
            <Stack.Screen name="insertion" component={InsertionScreen} options={{ title: "Insert and remove" }} />
            <Stack.Screen name="hover" component={HoverScreen} options={{ title: "Hover and tilt" }} />
            <Stack.Screen name="drawing" component={DrawingScreen} options={{ title: "Drawing pressure" }} />
            <Stack.Screen name="button" component={ButtonScreen} options={{ title: "Remote button" }} />
            <Stack.Screen name="airMotion" component={AirMotionScreen} options={{ title: "Air Motion" }} />
            <Stack.Screen name="events" component={EventsScreen} options={{ title: "Raw event log" }} />
          </Stack.Navigator>
        </NavigationContainer>
      </SpenTestProvider>
    </SafeAreaProvider>
  );
}
