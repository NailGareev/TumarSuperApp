package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class MiniBarChartView extends View {

    private float[] values = {};
    private String[] labels = {};

    private final Paint barPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint peakPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int COLOR_ACTIVE   = 0xFF6B21A8;
    private static final int COLOR_PEAK     = 0x806B21A8;
    private static final int COLOR_LIGHT    = 0x2D6B21A8;
    private static final int COLOR_LABEL    = 0xFF9E9E9E;

    public MiniBarChartView(Context ctx) { super(ctx); init(); }
    public MiniBarChartView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public MiniBarChartView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs, defStyle); init(); }

    private void init() {
        barPaint.setColor(COLOR_LIGHT);
        activePaint.setColor(COLOR_ACTIVE);
        peakPaint.setColor(COLOR_PEAK);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(COLOR_LABEL);
    }

    public void setData(float[] values, String[] labels) {
        this.values = values != null ? values : new float[0];
        this.labels = labels != null ? labels : new String[0];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.length == 0) return;

        float d = getResources().getDisplayMetrics().density;
        int n = values.length;
        float labelH = 16f * d;
        float chartH = getHeight() - labelH - 4 * d;
        float gap = 5f * d;
        float barW = (getWidth() - gap * (n - 1)) / n;

        float maxVal = 1f;
        for (float v : values) if (v > maxVal) maxVal = v;

        int activeIdx = n - 1;
        int peakIdx = -1;
        float peakVal = 0f;
        for (int i = 0; i < n; i++) {
            if (values[i] > peakVal) { peakVal = values[i]; peakIdx = i; }
        }

        for (int i = 0; i < n; i++) {
            float x = i * (barW + gap);
            float barH = values[i] == 0 ? 4 * d : chartH * (values[i] / maxVal);
            float top = chartH - barH;
            RectF rect = new RectF(x, top, x + barW, chartH);
            float r = 3 * d;

            Paint p;
            if (i == activeIdx)      p = activePaint;
            else if (i == peakIdx)   p = peakPaint;
            else                     p = barPaint;
            canvas.drawRoundRect(rect, r, r, p);

            if (i < labels.length) {
                labelPaint.setTextSize(9 * d);
                labelPaint.setColor(i == activeIdx ? COLOR_ACTIVE : COLOR_LABEL);
                labelPaint.setFakeBoldText(i == activeIdx);
                canvas.drawText(labels[i], x + barW / 2f, getHeight() - 2 * d, labelPaint);
            }
        }
    }
}
