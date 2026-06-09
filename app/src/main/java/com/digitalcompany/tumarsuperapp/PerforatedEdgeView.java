package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PerforatedEdgeView extends View {

    private final Paint bgPaint   = new Paint();
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PerforatedEdgeView(Context ctx) { super(ctx); init(); }
    public PerforatedEdgeView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public PerforatedEdgeView(Context ctx, AttributeSet attrs, int def) { super(ctx, attrs, def); init(); }

    private void init() {
        bgPaint.setColor(0xFFF2F2F2);
        holePaint.setColor(0xFFFFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d  = getResources().getDisplayMetrics().density;
        float r  = 5.5f * d;
        float sp = 18f  * d;
        float cy = getHeight() / 2f;
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        for (float x = sp / 2; x < getWidth(); x += sp) {
            canvas.drawCircle(x, cy, r, holePaint);
        }
    }
}
