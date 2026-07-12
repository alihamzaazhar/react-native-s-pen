export type RootStackParamList = {
  home: undefined;
  connection: undefined;
  insertion: undefined;
  hover: undefined;
  drawing: undefined;
  button: undefined;
  airMotion: undefined;
  events: undefined;
};

export type TestRoute = Exclude<keyof RootStackParamList, "home">;
