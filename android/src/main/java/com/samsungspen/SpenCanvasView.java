package com.samsungspen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

final class SpenCanvasView extends View {
  private static final class StrokePoint {
    final float x;
    final float y;
    final float pressure;
    final boolean startsStroke;

    StrokePoint(float x, float y, float pressure, boolean startsStroke) {
      this.x = x;
      this.y = y;
      this.pressure = pressure;
      this.startsStroke = startsStroke;
    }
  }

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
  private final List<StrokePoint> points = new ArrayList<>();
  private float minStrokeWidth = 2f;
  private float maxStrokeWidth = 16f;
  private int clearToken = 0;

  SpenCanvasView(Context context) {
    super(context);
    paint.setColor(Color.rgb(17, 20, 15));
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeJoin(Paint.Join.ROUND);
    setBackgroundColor(Color.rgb(233, 237, 223));
    setFocusable(true);
  }

  void setInkColor(String color) {
    try {
      paint.setColor(Color.parseColor(color));
      invalidate();
    } catch (IllegalArgumentException ignored) {
      // Keep the previous valid color.
    }
  }

  void setMinStrokeWidth(float width) {
    minStrokeWidth = Math.max(0.5f, width);
    invalidate();
  }

  void setMaxStrokeWidth(float width) {
    maxStrokeWidth = Math.max(minStrokeWidth, width);
    invalidate();
  }

  void setClearToken(int token) {
    if (clearToken != token) {
      clearToken = token;
      points.clear();
      invalidate();
    }
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);
    if (points.isEmpty()) {
      return;
    }

    StrokePoint previous = null;
    for (StrokePoint point : points) {
      float width = pressureToWidth(point.pressure);
      paint.setStrokeWidth(width);
      if (previous == null || point.startsStroke) {
        canvas.drawPoint(point.x, point.y, paint);
      } else {
        Path segment = new Path();
        segment.moveTo(previous.x, previous.y);
        segment.lineTo(point.x, point.y);
        canvas.drawPath(segment, paint);
      }
      previous = point;
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int pointerIndex = Math.max(0, event.getActionIndex());
    int toolType = event.getToolType(pointerIndex);
    if (toolType != MotionEvent.TOOL_TYPE_STYLUS && toolType != MotionEvent.TOOL_TYPE_ERASER) {
      return false;
    }

    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      getParent().requestDisallowInterceptTouchEvent(true);
      addPoint(event, pointerIndex, true);
      emitPoint(event, pointerIndex, "down", toolType);
      return true;
    }
    if (action == MotionEvent.ACTION_MOVE) {
      addPoint(event, pointerIndex, false);
      emitPoint(event, pointerIndex, "move", toolType);
      return true;
    }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      addPoint(event, pointerIndex, false);
      emitPoint(event, pointerIndex, action == MotionEvent.ACTION_CANCEL ? "cancel" : "up", toolType);
      getParent().requestDisallowInterceptTouchEvent(false);
      return true;
    }
    return true;
  }

  private void addPoint(MotionEvent event, int pointerIndex, boolean startsStroke) {
    points.add(new StrokePoint(event.getX(pointerIndex), event.getY(pointerIndex), event.getPressure(pointerIndex), startsStroke));
    invalidate();
  }

  private float pressureToWidth(float pressure) {
    float normalized = Math.max(0f, Math.min(1f, pressure));
    return minStrokeWidth + ((maxStrokeWidth - minStrokeWidth) * normalized);
  }

  private void emitPoint(MotionEvent event, int pointerIndex, String action, int toolType) {
    WritableMap payload = Arguments.createMap();
    payload.putDouble("x", event.getX(pointerIndex));
    payload.putDouble("y", event.getY(pointerIndex));
    payload.putDouble("pressure", event.getPressure(pointerIndex));
    payload.putDouble("tilt", event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex));
    payload.putDouble("hoverDistance", event.getAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex));
    payload.putDouble("timestamp", event.getEventTime());
    payload.putString("action", action);
    payload.putString("toolType", toolType == MotionEvent.TOOL_TYPE_ERASER ? "eraser" : "stylus");

    EventDispatcher dispatcher =
        UIManagerHelper.getEventDispatcherForReactTag((ReactContext) getContext(), getId());
    if (dispatcher != null) {
      int surfaceId = UIManagerHelper.getSurfaceId(this);
      dispatcher.dispatchEvent(new SpenCanvasEvent(surfaceId, getId(), payload));
    }
  }

  private static final class SpenCanvasEvent extends Event<SpenCanvasEvent> {
    private final WritableMap payload;

    SpenCanvasEvent(int surfaceId, int viewTag, WritableMap payload) {
      super(surfaceId, viewTag);
      this.payload = payload;
    }

    @NonNull
    @Override
    public String getEventName() {
      return "topSpenDraw";
    }

    @Override
    public boolean canCoalesce() {
      return false;
    }

    @Override
    protected WritableMap getEventData() {
      return payload;
    }
  }
}
