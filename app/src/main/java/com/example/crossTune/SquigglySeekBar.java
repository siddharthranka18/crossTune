package com.example.crossTune;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatSeekBar;

public class SquigglySeekBar extends AppCompatSeekBar {

    // Paints
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path wavePath = new Path();

    // Aesthetic Configuration (Dynamically scaled to screen density later)
    private float amplitude = 5f;     // Subtle, elegant wave height
    private float waveLength = 45f;   // Smooth, long wave cycles
    private float strokeWidth = 4.5f; // Thick, premium track
    private float thumbRadius = 6f;   // Sleek thumb
    private float thumbGlowRadius = 18f; // Beautiful aura

    // State Tracking
    private float phase = 0f;
    private float thumbScale = 1f;
    private ValueAnimator waveAnimator;

    public SquigglySeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 1. Destroy Default Android Rendering completely
        setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
        setThumb(new ColorDrawable(Color.TRANSPARENT));
        setSplitTrack(false);

        // 2. Scale aesthetics to match the device's exact pixel density
        float density = getResources().getDisplayMetrics().density;
        amplitude *= density;
        waveLength *= density;
        strokeWidth *= density;
        thumbRadius *= density;
        thumbGlowRadius *= density;

        // 3. Setup Paints
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);
        wavePaint.setStrokeWidth(strokeWidth);
        wavePaint.setColor(Color.WHITE);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(Color.parseColor("#33FFFFFF")); // 20% opacity flat line

        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.WHITE);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(Color.parseColor("#4DFFFFFF")); // 30% opacity glow

        startWaveAnimation();
    }

    private void startWaveAnimation() {
        waveAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        waveAnimator.setDuration(1200); // 1.2 seconds per wave cycle (Butter smooth)
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setInterpolator(new LinearInterpolator());
        waveAnimator.addUpdateListener(anim -> {
            phase = (float) anim.getAnimatedValue();
            invalidate(); // 60FPS UI Redraw
        });
        waveAnimator.start();
    }

    // ==========================================
    // TACTILE FEEDBACK: Grow/Shrink on Touch
    // ==========================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animateThumb(1.5f); // Grow 50% larger when dragged
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateThumb(1f); // Shrink back to normal
                break;
        }
        return super.onTouchEvent(event);
    }

    private void animateThumb(float targetScale) {
        ValueAnimator anim = ValueAnimator.ofFloat(thumbScale, targetScale);
        anim.setDuration(250);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            thumbScale = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    // ==========================================
    // THE MASTERPIECE: Custom Render Engine
    // ==========================================
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float centerY = getHeight() / 2f;
        float progressRatio = getMax() > 0 ? (float) getProgress() / getMax() : 0;
        float progressX = getPaddingLeft() + (width * progressRatio);

        // 1. Draw the Flat Track (The part of the song not played yet)
        if (progressX < getWidth() - getPaddingRight()) {
            canvas.drawLine(progressX, centerY, getWidth() - getPaddingRight(), centerY, trackPaint);
        }

        // 2. Draw the Wavy Track (The part of the song already played)
        wavePath.reset();
        wavePath.moveTo(getPaddingLeft(), centerY);

        int segments = (int) (progressX - getPaddingLeft());
        if (segments > 0) {
            for (int i = 0; i <= segments; i++) {
                float x = getPaddingLeft() + i;
                float distanceToThumb = progressX - x;

                // FLUID PHYSICS: The wave flattens out perfectly as it reaches the thumb
                float taperFactor = Math.min(1f, distanceToThumb / (waveLength / 1.5f));
                float currentAmplitude = amplitude * taperFactor;

                // `- phase` makes the wave flow forward INTO the thumb (looks amazing)
                float relativeX = x / waveLength;
                float y = centerY + (float) (Math.sin(relativeX * Math.PI * 2 - phase) * currentAmplitude);

                if (i == 0) wavePath.moveTo(x, y);
                else wavePath.lineTo(x, y);
            }
            canvas.drawPath(wavePath, wavePaint);
        }

        // 3. Draw the Glowing Thumb
        float currentThumbRadius = thumbRadius * thumbScale;
        float currentGlowRadius = thumbGlowRadius * thumbScale;

        canvas.drawCircle(progressX, centerY, currentGlowRadius, glowPaint); // The Aura
        canvas.drawCircle(progressX, centerY, currentThumbRadius, thumbPaint); // The Core
    }

    // ==========================================
    // DYNAMIC ACCENT COLOR SUPPORT
    // ==========================================
    public void setAccentColor(int color) {
        wavePaint.setColor(color);
        thumbPaint.setColor(color);
        // Extract RGB and apply 30% alpha for the glow effect
        glowPaint.setColor(Color.argb(77, Color.red(color), Color.green(color), Color.blue(color)));
        invalidate();
    }
}