package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class DonutChartView extends View {

    private float[] percentages = {};
    private int[]   colors      = {};
    private String  centerLabel = "";
    private String  centerValue = "";

    private final Paint arcPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lblPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DonutChartView(Context ctx) { super(ctx); init(); }
    public DonutChartView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public DonutChartView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs, defStyle); init(); }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        holePaint.setColor(0xFFFFFFFF);
        holePaint.setStyle(Paint.Style.FILL);
        lblPaint.setTextAlign(Paint.Align.CENTER);
        lblPaint.setColor(0xFF777777);
        valPaint.setTextAlign(Paint.Align.CENTER);
        valPaint.setColor(0xFF111111);
        valPaint.setFakeBoldText(true);
    }

    public void setData(float[] percentages, int[] colors, String centerLabel, String centerValue) {
        this.percentages = percentages != null ? percentages : new float[0];
        this.colors      = colors      != null ? colors      : new int[0];
        this.centerLabel = centerLabel != null ? centerLabel : "";
        this.centerValue = centerValue != null ? centerValue : "";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (percentages.length == 0) return;

        float d  = getResources().getDisplayMetrics().density;
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) - 2 * d;
        float sw = r * 0.36f;

        arcPaint.setStrokeWidth(sw);
        RectF oval = new RectF(cx - r + sw / 2, cy - r + sw / 2,
                               cx + r - sw / 2, cy + r - sw / 2);

        float start = -90f;
        for (int i = 0; i < percentages.length && i < colors.length; i++) {
            float sweep = percentages[i] * 3.6f;
            arcPaint.setColor(colors[i]);
            canvas.drawArc(oval, start, sweep, false, arcPaint);
            start += sweep;
        }

        // inner hole
        canvas.drawCircle(cx, cy, r - sw, holePaint);

        // center text
        float textSize = 9 * d;
        lblPaint.setTextSize(textSize);
        valPaint.setTextSize(12 * d);
        canvas.drawText(centerLabel, cx, cy - 2 * d,         lblPaint);
        canvas.drawText(centerValue, cx, cy + 14 * d * 0.8f, valPaint);
    }
}
