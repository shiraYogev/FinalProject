package com.example.finalprojectappraisal.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom circular compass view.
 * Call {@link #setAzimuth(float)} with the device bearing in degrees (0=North, clockwise).
 * The needle rotates so its red tip always points to magnetic North.
 */
public class CompassView extends View {

    private float azimuth = 0f;

    private final Paint bgPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint northPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint southPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardinalPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  needlePath   = new Path();

    public CompassView(Context context) {
        super(context); init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }

    public CompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        bgPaint.setColor(Color.argb(220, 255, 255, 255));
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(Color.argb(180, 120, 120, 120));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        northPaint.setColor(Color.parseColor("#E53935"));
        northPaint.setStyle(Paint.Style.FILL);

        southPaint.setColor(Color.parseColor("#BDBDBD"));
        southPaint.setStyle(Paint.Style.FILL);

        centerPaint.setColor(Color.parseColor("#424242"));
        centerPaint.setStyle(Paint.Style.FILL);

        cardinalPaint.setColor(Color.parseColor("#212121"));
        cardinalPaint.setTextAlign(Paint.Align.CENTER);
        cardinalPaint.setFakeBoldText(true);

        tickPaint.setColor(Color.parseColor("#9E9E9E"));
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1.5f);
    }

    /**
     * @param azimuth device bearing in degrees, 0 = North, clockwise positive.
     */
    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) - 4f;

        // Background circle
        canvas.drawCircle(cx, cy, r, bgPaint);
        canvas.drawCircle(cx, cy, r, borderPaint);

        // Tick marks (every 30°)
        float innerTick = r * 0.84f;
        float outerTick = r * 0.94f;
        for (int deg = 0; deg < 360; deg += 30) {
            double rad = Math.toRadians(deg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            canvas.drawLine(
                cx + innerTick * cos, cy + innerTick * sin,
                cx + outerTick * cos, cy + outerTick * sin,
                tickPaint
            );
        }

        // Rotate canvas so the needle points to magnetic North
        canvas.save();
        canvas.rotate(-azimuth, cx, cy);

        float needleW = r * 0.11f;
        float needleLen = r * 0.58f;

        // North needle (red, pointing up)
        needlePath.reset();
        needlePath.moveTo(cx, cy - needleLen);
        needlePath.lineTo(cx - needleW, cy);
        needlePath.lineTo(cx + needleW, cy);
        needlePath.close();
        canvas.drawPath(needlePath, northPaint);

        // South needle (grey, pointing down)
        needlePath.reset();
        needlePath.moveTo(cx, cy + needleLen);
        needlePath.lineTo(cx - needleW, cy);
        needlePath.lineTo(cx + needleW, cy);
        needlePath.close();
        canvas.drawPath(needlePath, southPaint);

        canvas.restore();

        // Cardinal labels (fixed – do NOT rotate with needle)
        cardinalPaint.setTextSize(r * 0.20f);
        float labelR = r * 0.68f;
        float textOff = cardinalPaint.getTextSize() * 0.35f;

        cardinalPaint.setColor(Color.parseColor("#E53935")); // N in red
        canvas.drawText("N", cx, cy - labelR + textOff, cardinalPaint);

        cardinalPaint.setColor(Color.parseColor("#212121"));
        canvas.drawText("S", cx, cy + labelR + textOff, cardinalPaint);
        canvas.drawText("E", cx + labelR, cy + textOff, cardinalPaint);
        canvas.drawText("W", cx - labelR, cy + textOff, cardinalPaint);

        // Center dot
        canvas.drawCircle(cx, cy, r * 0.09f, centerPaint);
    }
}
