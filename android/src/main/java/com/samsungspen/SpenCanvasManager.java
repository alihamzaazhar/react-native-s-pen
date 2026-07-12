package com.samsungspen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.viewmanagers.SpenCanvasManagerDelegate;
import com.facebook.react.viewmanagers.SpenCanvasManagerInterface;

import java.util.Map;

public final class SpenCanvasManager extends SimpleViewManager<SpenCanvasView>
    implements SpenCanvasManagerInterface<SpenCanvasView> {
  static final String NAME = "SpenCanvas";
  private final ViewManagerDelegate<SpenCanvasView> delegate =
      new SpenCanvasManagerDelegate<>(this);

  @Override
  protected ViewManagerDelegate<SpenCanvasView> getDelegate() {
    return delegate;
  }

  @NonNull
  @Override
  public String getName() {
    return NAME;
  }

  @NonNull
  @Override
  protected SpenCanvasView createViewInstance(@NonNull ThemedReactContext context) {
    return new SpenCanvasView(context);
  }

  @ReactProp(name = "inkColor")
  public void setInkColor(SpenCanvasView view, @Nullable String color) {
    if (color != null) {
      view.setInkColor(color);
    }
  }

  @ReactProp(name = "minStrokeWidth", defaultFloat = 2f)
  public void setMinStrokeWidth(SpenCanvasView view, float width) {
    view.setMinStrokeWidth(width);
  }

  @ReactProp(name = "maxStrokeWidth", defaultFloat = 16f)
  public void setMaxStrokeWidth(SpenCanvasView view, float width) {
    view.setMaxStrokeWidth(width);
  }

  @ReactProp(name = "clearToken", defaultInt = 0)
  public void setClearToken(SpenCanvasView view, int token) {
    view.setClearToken(token);
  }

  @Override
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.<String, Object>builder()
        .put("topSpenDraw", MapBuilder.of("registrationName", "onSpenDraw"))
        .build();
  }
}
