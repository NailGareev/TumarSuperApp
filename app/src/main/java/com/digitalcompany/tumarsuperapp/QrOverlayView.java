package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class QrOverlayView extends View {

    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int FRAME_COLOR  = 0xFF6200EE;
    private static final int CORNER_LEN   = 72;  // px, scaled by density
    private static final int CORNER_W     = 6;
    private static final float FRAME_RATIO = 0.68f;

    private float laserY = 0f;
    private boolean laserDown = true;

    public QrOverlayView(Context context) {
        super(context);
        init();
    }

    public QrOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        maskPaint.setColor(Color.argb(160, 0, 0, 0));
        maskPaint.setStyle(Paint.Style.FILL);

        cornerPaint.setColor(FRAME_COLOR);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(CORNER_W * getResources().getDisplayMetrics().density);
        cornerPaint.setStrokeCap(Paint.Cap.SQUARE);

        laserPaint.setColor(Color.argb(200, 98, 0, 238));
        laserPaint.setStyle(Paint.Style.FILL);

        // Animate laser line
        postDelayed(laserRunnable, 16);
    }

    private final Runnable laserRunnable = new Runnable() {
        @Override
        public void run() {
            if (getWidth() == 0) { postDelayed(this, 16); return; }
            int size = (int)(Math.min(getWidth(), getHeight()) * FRAME_RATIO);
            int top = (getHeight() - size) / 2;
            float speed = size / 60f;
            if (laserDown) {
                laserY += speed;
                if (laserY > size) laserDown = false;
            } else {
                laserY -= speed;
                if (laserY < 0) laserDown = true;
            }
            invalidate();
            postDelayed(this, 16);
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        int size  = (int)(Math.min(w, h) * FRAME_RATIO);
        int left  = (w - size) / 2;
        int top   = (h - size) / 2;
        int right = left + size;
        int bot   = top  + size;
        float cl  = CORNER_LEN * getResources().getDisplayMetrics().density;

        // Dark mask (4 strips around scan box)
        canvas.drawRect(0, 0, w, top,  maskPaint);
        canvas.drawRect(0, top, left, bot, maskPaint);
        canvas.drawRect(right, top, w, bot, maskPaint);
        canvas.drawRect(0, bot, w, h,  maskPaint);

        // Laser line
        float laserAbsY = top + laserY;
        laserPaint.setAlpha(160);
        canvas.drawRect(left + cl, laserAbsY - 2, right - cl, laserAbsY + 2, laserPaint);

        // Purple corner brackets
        // Top-left
        canvas.drawLine(left, top, left + cl, top, cornerPaint);
        canvas.drawLine(left, top, left, top + cl, cornerPaint);
        // Top-right
        canvas.drawLine(right, top, right - cl, top, cornerPaint);
        canvas.drawLine(right, top, right, top + cl, cornerPaint);
        // Bottom-left
        canvas.drawLine(left, bot, left + cl, bot, cornerPaint);
        canvas.drawLine(left, bot, left, bot - cl, cornerPaint);
        // Bottom-right
        canvas.drawLine(right, bot, right - cl, bot, cornerPaint);
        canvas.drawLine(right, bot, right, bot - cl, cornerPaint);
    }
}
