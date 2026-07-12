package com.samsungspen;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;
import com.samsung.android.sdk.penremote.SpenEvent;
import com.samsung.android.sdk.penremote.SpenEventListener;
import com.samsung.android.sdk.penremote.SpenUnit;
import com.samsung.android.sdk.penremote.SpenUnitManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class NativeSpenModule extends com.facebook.react.bridge.ReactContextBaseJavaModule implements TurboModule, LifecycleEventListener {
  private static final String TAG = "SpenModule";
  private static final String EVENT_NAME = "SpenEvent";
  private static final String SPEN_INSERT_ACTION = "com.samsung.pen.INSERT";

  private final ReactApplicationContext reactContext;
  private final AtomicBoolean listening = new AtomicBoolean(false);

  @Nullable private View attachedRootView;
  @Nullable private Object spenRemote;
  @Nullable private Object spenUnitManager;
  @Nullable private Object spenConnectionCallback;
  @Nullable private Object spenConnectionStateListener;
  @Nullable private Object buttonEventListener;
  @Nullable private Object airMotionEventListener;
  @Nullable private Object buttonUnit;
  @Nullable private Object airMotionUnit;
  @Nullable private String lastConnectionState;
  @Nullable private String lastPenInsertionState;
  private boolean insertionReceiverRegistered = false;
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  private final BroadcastReceiver insertionReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          updatePenInsertionState(intent, true);
        }
      };

  public NativeSpenModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
  }

  @NonNull
  @Override
  public String getName() {
    return "SpenModule";
  }

  @ReactMethod
  public void isSupported(Promise promise) {
    promise.resolve(Boolean.valueOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP));
  }

  @ReactMethod
  public void getDeviceInfo(Promise promise) {
    WritableMap info = Arguments.createMap();
    info.putString("platform", "android");
    info.putString("manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
    info.putString("brand", Build.BRAND == null ? "" : Build.BRAND);
    info.putString("model", Build.MODEL == null ? "" : Build.MODEL);
    info.putInt("sdkInt", Build.VERSION.SDK_INT);
    info.putBoolean("isSamsungDevice", isSamsungDevice());
    info.putArray("capabilities", buildCapabilities());
    info.putMap("samsungSdk", buildSamsungSdkInfo());
    promise.resolve(info);
  }

  @ReactMethod
  public void getVersionCode(Promise promise) {
    promise.resolve(Double.valueOf(getSamsungRemoteInteger("getVersionCode", 0)));
  }

  @ReactMethod
  public void getVersionName(Promise promise) {
    promise.resolve(getSamsungRemoteString("getVersionName", ""));
  }

  @ReactMethod
  public void getPenInsertionState(Promise promise) {
    promise.resolve(readPenInsertionState());
  }

  @ReactMethod
  public void isConnected(Promise promise) {
    promise.resolve(Boolean.valueOf(isSamsungRemoteConnected()));
  }

  @ReactMethod
  public void isFeatureEnabled(String feature, Promise promise) {
    int featureType = mapFeatureNameToType(feature);
    promise.resolve(Boolean.valueOf(getSamsungRemoteBoolean("isFeatureEnabled", false, new Class<?>[] {int.class}, new Object[] {featureType})));
  }

  @ReactMethod
  public void startListening(Promise promise) {
    listening.set(true);
    attachRootViewListeners();
    registerPenInsertionReceiver();
    connectSamsungRemoteIfAvailable();
    promise.resolve(null);
  }

  @ReactMethod
  public void stopListening(Promise promise) {
    listening.set(false);
    disconnectSamsungRemote();
    unregisterPenInsertionReceiver();
    detachRootViewListeners();
    promise.resolve(null);
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Required by React Native's event emitter contract.
  }

  @ReactMethod
  public void removeListeners(double count) {
    // Required by React Native's event emitter contract.
  }

  @ReactMethod
  public void addConnectionStateListener(String eventName) {
    // Required by React Native's event emitter contract.
  }

  @ReactMethod
  public void removeConnectionStateListeners(double count) {
    // Required by React Native's event emitter contract.
  }

  @Override
  public void onHostResume() {
    if (listening.get()) {
      attachRootViewListeners();
      registerPenInsertionReceiver();
      connectSamsungRemoteIfAvailable();
    }
  }

  @Override
  public void onHostPause() {
    detachRootViewListeners();
    unregisterPenInsertionReceiver();
    disconnectSamsungRemote();
  }

  @Override
  public void onHostDestroy() {
    listening.set(false);
    detachRootViewListeners();
    unregisterPenInsertionReceiver();
    disconnectSamsungRemote();
  }

  private void attachRootViewListeners() {
    Activity activity = reactContext.getCurrentActivity();
    if (activity == null) {
      return;
    }

    Window window = activity.getWindow();
    if (window == null) {
      return;
    }

    View rootView = window.getDecorView();
    if (rootView == null || rootView == attachedRootView) {
      return;
    }

    detachRootViewListeners();
    attachedRootView = rootView;

    rootView.setOnTouchListener(
        new View.OnTouchListener() {
          @Override
          public boolean onTouch(View view, MotionEvent event) {
            if (!isStylusEvent(event)) {
              return false;
            }
            emitMotionEvent(event);
            return false;
          }
        });

    rootView.setOnHoverListener(
        new View.OnHoverListener() {
          @Override
          public boolean onHover(View view, MotionEvent event) {
            if (!isStylusEvent(event)) {
              return false;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER
                || event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE
                || event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
              emitMotionEvent(event);
            }
            return false;
          }
        });
  }

  private void detachRootViewListeners() {
    if (attachedRootView == null) {
      return;
    }

    attachedRootView.setOnTouchListener(null);
    attachedRootView.setOnHoverListener(null);
    attachedRootView = null;
  }

  private void registerPenInsertionReceiver() {
    if (!isSamsungDevice() || insertionReceiverRegistered) {
      return;
    }

    try {
      Intent stickyIntent =
          reactContext.registerReceiver(insertionReceiver, new IntentFilter(SPEN_INSERT_ACTION));
      insertionReceiverRegistered = true;
      if (stickyIntent != null) {
        updatePenInsertionState(stickyIntent, true);
      }
    } catch (Throwable t) {
      Log.w(TAG, "Samsung S Pen insertion state is unavailable", t);
    }
  }

  private void unregisterPenInsertionReceiver() {
    if (!insertionReceiverRegistered) {
      return;
    }

    try {
      reactContext.unregisterReceiver(insertionReceiver);
    } catch (Throwable t) {
      Log.w(TAG, "Failed to unregister Samsung S Pen insertion listener", t);
    } finally {
      insertionReceiverRegistered = false;
    }
  }

  private String readPenInsertionState() {
    if (!isSamsungDevice()) {
      return "unknown";
    }

    if (lastPenInsertionState != null) {
      return lastPenInsertionState;
    }

    try {
      Intent stickyIntent =
          reactContext.registerReceiver(null, new IntentFilter(SPEN_INSERT_ACTION));
      return updatePenInsertionState(stickyIntent, false);
    } catch (Throwable t) {
      Log.w(TAG, "Failed to read Samsung S Pen insertion state", t);
      return "unknown";
    }
  }

  private String updatePenInsertionState(@Nullable Intent intent, boolean shouldEmit) {
    String state = "unknown";
    if (intent != null && intent.hasExtra("penInsert")) {
      state = intent.getBooleanExtra("penInsert", false) ? "inserted" : "detached";
    }

    boolean changed = !state.equals(lastPenInsertionState);
    lastPenInsertionState = state;
    if (shouldEmit && changed) {
      WritableMap payload = buildBasePayload("spen-insertion-state", System.currentTimeMillis());
      payload.putString("insertionState", state);
      payload.putMap("raw", buildRawMap("action", SPEN_INSERT_ACTION, "penInsert", "inserted".equals(state)));
      emit(payload);
    }
    return state;
  }

  private void connectSamsungRemoteIfAvailable() {
    if (!isSamsungDevice() || connecting.get()) {
      return;
    }

    try {
      if (spenRemote != null) {
        boolean connected = getSamsungRemoteBoolean("isConnected", false);
        if (connected && spenUnitManager != null) {
          return;
        }

        clearSamsungRemoteReferences();
      }

      connecting.set(true);

      Class<?> remoteClass = Class.forName("com.samsung.android.sdk.penremote.SpenRemote");
      spenRemote = remoteClass.getMethod("getInstance").invoke(null);
      installSamsungConnectionStateListener(remoteClass);

      Class<?> callbackClass =
          Class.forName("com.samsung.android.sdk.penremote.SpenRemote$ConnectionResultCallback");
      spenConnectionCallback =
          Proxy.newProxyInstance(
              callbackClass.getClassLoader(),
              new Class<?>[] {callbackClass},
              new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  String methodName = method.getName();
                  if ("onSuccess".equals(methodName) && args != null && args.length > 0) {
                    connecting.set(false);
                    handleSamsungConnectionSuccess(args[0]);
                  } else if ("onFailure".equals(methodName) && args != null && args.length > 0) {
                    connecting.set(false);
                    Log.w(TAG, "Samsung S Pen Remote connection failed: " + args[0]);
                    emitErrorEvent(args[0]);
                    clearSamsungRemoteReferences();
                  }
                  return null;
                }
              });

      remoteClass
          .getMethod("connect", Context.class, callbackClass)
          .invoke(spenRemote, getConnectionContext(), spenConnectionCallback);
      Log.i(TAG, "Samsung S Pen Remote connection requested");
    } catch (Throwable t) {
      connecting.set(false);
      Log.w(TAG, "Samsung S Pen Remote SDK not available", t);
    }
  }

  private void handleSamsungConnectionSuccess(Object unitManager) {
    spenUnitManager = unitManager;

    try {
      Class<?> eventClass = Class.forName("com.samsung.android.sdk.penremote.SpenEvent");
      Class<?> remoteClass = Class.forName("com.samsung.android.sdk.penremote.SpenRemote");

      installSamsungConnectionStateListener(remoteClass);

      SpenUnitManager typedManager = (SpenUnitManager) unitManager;
      SpenEventListener typedButtonListener =
          new SpenEventListener() {
            @Override
            public void onEvent(SpenEvent event) {
              try {
                emitSamsungButtonEvent(event, eventClass);
              } catch (Throwable t) {
                Log.e(TAG, "Failed to decode Samsung button event", t);
              }
            }
          };
      SpenEventListener typedAirMotionListener =
          new SpenEventListener() {
            @Override
            public void onEvent(SpenEvent event) {
              try {
                emitSamsungAirMotionEvent(event, eventClass);
              } catch (Throwable t) {
                Log.e(TAG, "Failed to decode Samsung Air Motion event", t);
              }
            }
          };

      SpenUnit typedButtonUnit = typedManager.getUnit(SpenUnit.TYPE_BUTTON);
      SpenUnit typedAirMotionUnit = typedManager.getUnit(SpenUnit.TYPE_AIR_MOTION);
      if (typedButtonUnit != null) {
        typedManager.registerSpenEventListener(typedButtonListener, typedButtonUnit);
      }
      if (typedAirMotionUnit != null) {
        typedManager.registerSpenEventListener(typedAirMotionListener, typedAirMotionUnit);
      }
      buttonEventListener = typedButtonListener;
      airMotionEventListener = typedAirMotionListener;
      buttonUnit = typedButtonUnit;
      airMotionUnit = typedAirMotionUnit;
      Log.i(
          TAG,
          "Samsung S Pen Remote connected; button="
              + (buttonUnit != null)
              + ", airMotion="
              + (airMotionUnit != null));
      emitConnectionStateEvent("connected", "connected");
    } catch (Throwable t) {
      Log.w(TAG, "Failed to register Samsung S Pen listeners", t);
    }
  }

  private void installSamsungConnectionStateListener(Class<?> remoteClass) throws Exception {
    if (spenConnectionStateListener != null) {
      return;
    }

    Class<?> stateListenerClass =
        Class.forName("com.samsung.android.sdk.penremote.SpenRemote$ConnectionStateChangeListener");
    spenConnectionStateListener =
        Proxy.newProxyInstance(
            stateListenerClass.getClassLoader(),
            new Class<?>[] {stateListenerClass},
            new InvocationHandler() {
              @Override
              public Object invoke(Object proxy, Method method, Object[] args) {
                if ("onChange".equals(method.getName()) && args != null && args.length > 0) {
                  String state = mapConnectionState(args[0]);
                  Log.i(TAG, "Samsung S Pen Remote state changed: " + args[0] + " (" + state + ")");
                  emitConnectionStateEvent(state, String.valueOf(args[0]));
                }
                return null;
              }
            });

    remoteClass
        .getMethod("setConnectionStateChangeListener", stateListenerClass)
        .invoke(spenRemote, spenConnectionStateListener);
  }

  private void emitSamsungButtonEvent(Object spenEvent, Class<?> eventClass) throws Exception {
    Class<?> buttonEventClass = Class.forName("com.samsung.android.sdk.penremote.ButtonEvent");
    Constructor<?> buttonCtor = buttonEventClass.getConstructor(eventClass);
    Object buttonEvent = buttonCtor.newInstance(spenEvent);
    int action = (Integer) buttonEventClass.getMethod("getAction").invoke(buttonEvent);
    long timestamp = (Long) buttonEventClass.getMethod("getTimeStamp").invoke(buttonEvent);
    WritableMap payload = buildBasePayload("spen-button", timestamp / 1000000.0);
    payload.putInt("buttonState", action);
    payload.putString("action", action == 0 ? "down" : "up");
    payload.putMap("raw", buildRawMap("action", action, "eventClass", spenEvent.getClass().getName()));
    Log.d(TAG, "Samsung button event: " + action);
    emit(payload);
  }

  private void emitSamsungAirMotionEvent(Object spenEvent, Class<?> eventClass) throws Exception {
    Class<?> airMotionEventClass = Class.forName("com.samsung.android.sdk.penremote.AirMotionEvent");
    Constructor<?> airCtor = airMotionEventClass.getConstructor(eventClass);
    Object airMotionEvent = airCtor.newInstance(spenEvent);
    float deltaX = ((Number) airMotionEventClass.getMethod("getDeltaX").invoke(airMotionEvent)).floatValue();
    float deltaY = ((Number) airMotionEventClass.getMethod("getDeltaY").invoke(airMotionEvent)).floatValue();
    long timestamp = (Long) airMotionEventClass.getMethod("getTimeStamp").invoke(airMotionEvent);
    WritableMap payload = buildBasePayload("spen-air-action", timestamp / 1000000.0);
    WritableMap point = Arguments.createMap();
    point.putDouble("x", deltaX);
    point.putDouble("y", deltaY);
    payload.putMap("point", point);
    payload.putMap("raw", buildRawMap("deltaX", deltaX, "deltaY", deltaY, "eventClass", spenEvent.getClass().getName()));
    Log.d(TAG, "Samsung Air Motion event: x=" + deltaX + ", y=" + deltaY);
    emit(payload);
  }

  private void disconnectSamsungRemote() {
    try {
      if (spenUnitManager != null && buttonUnit != null) {
        Class<?> managerClass = Class.forName("com.samsung.android.sdk.penremote.SpenUnitManager");
        Class<?> unitClass = Class.forName("com.samsung.android.sdk.penremote.SpenUnit");
        managerClass.getMethod("unregisterSpenEventListener", unitClass).invoke(spenUnitManager, buttonUnit);
      }
      if (spenUnitManager != null && airMotionUnit != null) {
        Class<?> managerClass = Class.forName("com.samsung.android.sdk.penremote.SpenUnitManager");
        Class<?> unitClass = Class.forName("com.samsung.android.sdk.penremote.SpenUnit");
        managerClass.getMethod("unregisterSpenEventListener", unitClass).invoke(spenUnitManager, airMotionUnit);
      }
      if (spenRemote != null) {
        Class<?> remoteClass = Class.forName("com.samsung.android.sdk.penremote.SpenRemote");
        remoteClass.getMethod("setConnectionStateChangeListener", Class.forName("com.samsung.android.sdk.penremote.SpenRemote$ConnectionStateChangeListener")).invoke(spenRemote, new Object[] {null});
        remoteClass.getMethod("disconnect", Context.class).invoke(spenRemote, getConnectionContext());
      }
      emitConnectionStateEvent("disconnected", "disconnected");
    } catch (Throwable t) {
      Log.w(TAG, "Failed to disconnect Samsung S Pen Remote SDK", t);
    } finally {
      connecting.set(false);
      clearSamsungRemoteReferences();
    }
  }

  private void clearSamsungRemoteReferences() {
    spenRemote = null;
    spenUnitManager = null;
    spenConnectionCallback = null;
    spenConnectionStateListener = null;
    buttonEventListener = null;
    airMotionEventListener = null;
    buttonUnit = null;
    airMotionUnit = null;
  }

  private boolean isStylusEvent(MotionEvent event) {
    int toolType = event.getToolType(0);
    return toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER;
  }

  private void emitMotionEvent(MotionEvent event) {
    String eventName = mapMotionEventName(event.getActionMasked());
    if (eventName == null) {
      return;
    }

    WritableMap payload = buildBasePayload(eventName, event.getEventTime());
    WritableMap point = Arguments.createMap();
    int pointerIndex = event.getActionIndex() >= 0 ? event.getActionIndex() : 0;
    float pressure = event.getPressure(pointerIndex);
    float hoverDistance = event.getAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex);
    float tilt = getTilt(event, pointerIndex);
    point.putDouble("x", event.getX(pointerIndex));
    point.putDouble("y", event.getY(pointerIndex));
    point.putDouble("pressure", pressure);
    point.putDouble("hoverDistance", hoverDistance);
    point.putDouble("tilt", tilt);
    point.putString("toolType", mapToolType(event.getToolType(pointerIndex)));
    payload.putMap("point", point);
    payload.putMap(
        "raw",
        buildRawMap(
            "source", "android-digitizer",
            "actionMasked", event.getActionMasked(),
            "pointerCount", event.getPointerCount(),
            "actionIndex", event.getActionIndex()));
    emit(payload);
  }

  private float getTilt(MotionEvent event, int pointerIndex) {
    try {
      return event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex);
    } catch (Throwable ignored) {
      try {
        return event.getAxisValue(MotionEvent.AXIS_TILT);
      } catch (Throwable ignoredAgain) {
        return 0f;
      }
    }
  }

  @Nullable
  private String mapMotionEventName(int actionMasked) {
    switch (actionMasked) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        return "spen-down";
      case MotionEvent.ACTION_MOVE:
        return "spen-move";
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
      case MotionEvent.ACTION_CANCEL:
        return "spen-up";
      case MotionEvent.ACTION_HOVER_ENTER:
      case MotionEvent.ACTION_HOVER_MOVE:
      case MotionEvent.ACTION_HOVER_EXIT:
        return "spen-hover";
      default:
        return null;
    }
  }

  private String mapToolType(int toolType) {
    switch (toolType) {
      case MotionEvent.TOOL_TYPE_STYLUS:
        return "stylus";
      case MotionEvent.TOOL_TYPE_ERASER:
        return "stylus";
      case MotionEvent.TOOL_TYPE_FINGER:
        return "finger";
      case MotionEvent.TOOL_TYPE_MOUSE:
        return "mouse";
      default:
        return "unknown";
    }
  }

  private WritableMap buildBasePayload(String name, double timestamp) {
    WritableMap payload = Arguments.createMap();
    payload.putString("name", name);
    payload.putDouble("timestamp", timestamp);
    return payload;
  }

  private WritableMap buildRawMap(Object... keyValues) {
    WritableMap raw = Arguments.createMap();
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      String key = String.valueOf(keyValues[i]);
      Object value = keyValues[i + 1];
      if (value instanceof Boolean) {
        raw.putBoolean(key, (Boolean) value);
      } else if (value instanceof Integer) {
        raw.putInt(key, (Integer) value);
      } else if (value instanceof Long) {
        raw.putDouble(key, ((Long) value).doubleValue());
      } else if (value instanceof Float) {
        raw.putDouble(key, ((Float) value).doubleValue());
      } else if (value instanceof Double) {
        raw.putDouble(key, (Double) value);
      } else if (value == null) {
        raw.putNull(key);
      } else {
        raw.putString(key, String.valueOf(value));
      }
    }
    return raw;
  }

  private WritableArray buildCapabilities() {
    WritableArray capabilities = Arguments.createArray();
    capabilities.pushString("stylusInput");
    capabilities.pushString("hover");
    capabilities.pushString("pressure");
    if (isFeatureEnabledForRemote("button")) {
      capabilities.pushString("button");
    }
    if (isFeatureEnabledForRemote("airMotion")) {
      capabilities.pushString("airActions");
      capabilities.pushString("airCommand");
    }
    return capabilities;
  }

  private boolean isSamsungDevice() {
    String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
    String brand = Build.BRAND == null ? "" : Build.BRAND;
    return manufacturer.toLowerCase(Locale.US).contains("samsung")
        || brand.toLowerCase(Locale.US).contains("samsung");
  }

  private boolean hasSamsungRemoteSdk() {
    return classExists("com.samsung.android.sdk.penremote.SpenRemote");
  }

  private WritableMap buildSamsungSdkInfo() {
    WritableMap sdk = Arguments.createMap();
    boolean available = hasSamsungRemoteSdk();
    sdk.putBoolean("available", available);
    sdk.putBoolean("connected", isSamsungRemoteConnected());
    if (available) {
      sdk.putInt("versionCode", getSamsungRemoteInteger("getVersionCode", 0));
      sdk.putString("versionName", getSamsungRemoteString("getVersionName", ""));
      WritableMap featureFlags = Arguments.createMap();
      featureFlags.putBoolean("button", isFeatureEnabledForRemote("button"));
      featureFlags.putBoolean("airMotion", isFeatureEnabledForRemote("airMotion"));
      sdk.putMap("featureFlags", featureFlags);
    }
    return sdk;
  }

  private boolean isSamsungRemoteConnected() {
    return "connected".equals(lastConnectionState)
        || getSamsungRemoteBoolean("isConnected", false);
  }

  private boolean isFeatureEnabledForRemote(String feature) {
    if (!hasSamsungRemoteSdk()) {
      return false;
    }

    int featureType = mapFeatureNameToType(feature);
    return getSamsungRemoteBoolean("isFeatureEnabled", false, new Class<?>[] {int.class}, new Object[] {featureType});
  }

  private int mapFeatureNameToType(String feature) {
    if (!hasSamsungRemoteSdk()) {
      return -1;
    }

    try {
      Class<?> remoteClass = Class.forName("com.samsung.android.sdk.penremote.SpenRemote");
      if ("airMotion".equals(feature)) {
        return remoteClass.getField("FEATURE_TYPE_AIR_MOTION").getInt(null);
      }
      return remoteClass.getField("FEATURE_TYPE_BUTTON").getInt(null);
    } catch (Throwable t) {
      return -1;
    }
  }

  private String mapConnectionState(Object stateValue) {
    if (stateValue == null) {
      return "unknown";
    }

    String normalized = String.valueOf(stateValue).trim().toUpperCase(Locale.US);

    try {
      Class<?> stateClass = Class.forName("com.samsung.android.sdk.penremote.SpenRemote$State");
      int connected = stateClass.getField("CONNECTED").getInt(null);
      int disconnected = stateClass.getField("DISCONNECTED").getInt(null);
      int disconnectedUnknown = stateClass.getField("DISCONNECTED_BY_UNKNOWN_REASON").getInt(null);

      if (stateValue instanceof Number) {
        int state = ((Number) stateValue).intValue();
        if (state == connected) {
          return "connected";
        }
        if (state == disconnected) {
          return "disconnected";
        }
        if (state == disconnectedUnknown) {
          return "disconnectedByUnknownReason";
        }
      }

      if (matchesStateName(normalized, "DISCONNECTED_BY_UNKNOWN_REASON")) {
        return "disconnectedByUnknownReason";
      }
      if (matchesStateName(normalized, "DISCONNECTED")) {
        return "disconnected";
      }
      if (matchesStateName(normalized, "CONNECTED")) {
        return "connected";
      }
    } catch (Throwable ignored) {
      // Fall through to generic mapping.
    }

    if (normalized.contains("DISCONNECTED_BY_UNKNOWN_REASON")) {
      return "disconnectedByUnknownReason";
    }
    if (normalized.contains("DISCONNECTED")) {
      return "disconnected";
    }
    if (normalized.contains("CONNECTED")) {
      return "connected";
    }

    return "unknown";
  }

  private boolean matchesStateName(String normalizedValue, String expected) {
    return normalizedValue.equals(expected)
        || normalizedValue.endsWith("." + expected)
        || normalizedValue.contains(expected);
  }

  private void emitConnectionStateEvent(String state, String rawState) {
    lastConnectionState = state;
    WritableMap payload = buildBasePayload("spen-connection-state", System.currentTimeMillis());
    payload.putString("connectionState", state);
    payload.putMap("raw", buildRawMap("state", state, "rawState", rawState));
    emit(payload);
  }

  private void emitErrorEvent(Object errorValue) {
    WritableMap payload = buildBasePayload("spen-error", System.currentTimeMillis());
    payload.putString("errorCode", mapErrorCode(errorValue));
    payload.putMap("raw", buildRawMap("error", String.valueOf(errorValue)));
    emit(payload);
    emitConnectionStateEvent(mapConnectionStateFromError(errorValue), String.valueOf(errorValue));
  }

  private String mapErrorCode(Object errorValue) {
    if (errorValue == null) {
      return "unknown";
    }

    try {
      Class<?> errorClass = Class.forName("com.samsung.android.sdk.penremote.SpenRemote$Error");
      int value = ((Number) errorValue).intValue();
      if (value == errorClass.getField("UNSUPPORTED_DEVICE").getInt(null)) {
        return "unsupportedDevice";
      }
      if (value == errorClass.getField("CONNECTION_FAILED").getInt(null)) {
        return "connectionFailed";
      }
      if (value == errorClass.getField("UNKNOWN").getInt(null)) {
        return "unknown";
      }
    } catch (Throwable ignored) {
      // Fall through to string-based fallback.
    }

    String normalized = String.valueOf(errorValue).toUpperCase(Locale.US);
    if (normalized.contains("UNSUPPORTED")) {
      return "unsupportedDevice";
    }
    if (normalized.contains("CONNECTION_FAILED")) {
      return "connectionFailed";
    }
    return "unknown";
  }

  private String mapConnectionStateFromError(Object errorValue) {
    String errorCode = mapErrorCode(errorValue);
    if ("unsupportedDevice".equals(errorCode)) {
      return "unsupportedDevice";
    }
    if ("connectionFailed".equals(errorCode)) {
      return "connectionFailed";
    }
    return "unknown";
  }

  private boolean getSamsungRemoteBoolean(String methodName, boolean fallback) {
    return getSamsungRemoteBoolean(methodName, fallback, new Class<?>[0], new Object[0]);
  }

  private boolean getSamsungRemoteBoolean(String methodName, boolean fallback, Class<?>[] parameterTypes, Object[] args) {
    Object remote = getSamsungRemoteInstance();
    if (remote == null) {
      return fallback;
    }
    try {
      Object value = remote.getClass().getMethod(methodName, parameterTypes).invoke(remote, args);
      return value instanceof Boolean ? (Boolean) value : fallback;
    } catch (Throwable ignored) {
      return fallback;
    }
  }

  private int getSamsungRemoteInteger(String methodName, int fallback) {
    Object remote = getSamsungRemoteInstance();
    if (remote == null) {
      return fallback;
    }
    try {
      Object value = remote.getClass().getMethod(methodName).invoke(remote);
      return value instanceof Number ? ((Number) value).intValue() : fallback;
    } catch (Throwable ignored) {
      return fallback;
    }
  }

  private String getSamsungRemoteString(String methodName, String fallback) {
    Object remote = getSamsungRemoteInstance();
    if (remote == null) {
      return fallback;
    }
    try {
      Object value = remote.getClass().getMethod(methodName).invoke(remote);
      return value == null ? fallback : String.valueOf(value);
    } catch (Throwable ignored) {
      return fallback;
    }
  }

  @Nullable
  private Context getConnectionContext() {
    Activity activity = reactContext.getCurrentActivity();
    return activity != null ? activity : reactContext;
  }

  @Nullable
  private Object getSamsungRemoteInstance() {
    if (spenRemote != null) {
      return spenRemote;
    }

    if (!hasSamsungRemoteSdk()) {
      return null;
    }

    try {
      Class<?> remoteClass = Class.forName("com.samsung.android.sdk.penremote.SpenRemote");
      spenRemote = remoteClass.getMethod("getInstance").invoke(null);
      return spenRemote;
    } catch (Throwable t) {
      Log.w(TAG, "Failed to obtain Samsung S Pen Remote instance", t);
      return null;
    }
  }

  private boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private void emitStatusEvent(String status, String message) {
    WritableMap payload = buildBasePayload("spen-status", System.currentTimeMillis());
    payload.putMap("raw", buildRawMap("status", status, "message", message));
    emit(payload);
  }

  private void emit(@NonNull WritableMap payload) {
    if (reactContext.hasActiveCatalystInstance()) {
      reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit(EVENT_NAME, payload);
    }
  }
}
