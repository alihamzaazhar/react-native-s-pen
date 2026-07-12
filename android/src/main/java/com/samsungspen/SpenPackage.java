package com.samsungspen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.uimanager.ViewManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpenPackage extends TurboReactPackage {
  private static final String MODULE_NAME = "SpenModule";

  @Nullable
  @Override
  public NativeModule getModule(String name, ReactApplicationContext reactContext) {
    if (MODULE_NAME.equals(name)) {
      return new NativeSpenModule(reactContext);
    }
    return null;
  }

  @Override
  public ReactModuleInfoProvider getReactModuleInfoProvider() {
    return new ReactModuleInfoProvider() {
      @NonNull
      @Override
      public Map<String, ReactModuleInfo> getReactModuleInfos() {
        Map<String, ReactModuleInfo> infos = new HashMap<>();
        infos.put(
            MODULE_NAME,
            new ReactModuleInfo(
                MODULE_NAME,
                NativeSpenModule.class.getName(),
                false,
                false,
                false,
                false,
                true));
        return infos;
      }
    };
  }

  @NonNull
  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
    return Collections.<ViewManager>singletonList(new SpenCanvasManager());
  }
}
