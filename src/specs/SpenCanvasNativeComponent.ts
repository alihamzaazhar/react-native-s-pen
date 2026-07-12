import { codegenNativeComponent } from "react-native";
import type { CodegenTypes, HostComponent, ViewProps } from "react-native";

export type NativeSpenCanvasPoint = Readonly<{
  x: CodegenTypes.Float;
  y: CodegenTypes.Float;
  pressure: CodegenTypes.Float;
  tilt: CodegenTypes.Float;
  hoverDistance: CodegenTypes.Float;
  timestamp: CodegenTypes.Double;
  action: string;
  toolType: string;
}>;

export interface NativeProps extends ViewProps {
  inkColor?: string;
  minStrokeWidth?: CodegenTypes.Float;
  maxStrokeWidth?: CodegenTypes.Float;
  clearToken?: CodegenTypes.Int32;
  onSpenDraw?: CodegenTypes.DirectEventHandler<NativeSpenCanvasPoint>;
}

export default codegenNativeComponent<NativeProps>("SpenCanvas") as HostComponent<NativeProps>;
