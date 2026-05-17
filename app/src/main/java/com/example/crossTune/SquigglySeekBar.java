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

    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wavePath = new Path();

    private float amplitude = 5f;
    private float waveLength = 45f;
    private float strokeWidth = 4.5f;
    private float thumbRadius = 6f;
    private float thumbGlowRadius = 18f;

    private float phase = 0f;
    private float thumbScale = 1f;
    private ValueAnimator waveAnimator;
    private boolean isWavy = false;

    public SquigglySeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
        setThumb(new ColorDrawable(Color.TRANSPARENT));
        setSplitTrack(false);

        float density = getResources().getDisplayMetrics().density;
        amplitude *= density;
        waveLength *= density;
        strokeWidth *= density;
        thumbRadius *= density;
        thumbGlowRadius *= density;

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);
        wavePaint.setStrokeWidth(strokeWidth);
        wavePaint.setColor(Color.WHITE);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(Color.parseColor("#33FFFFFF"));

        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.WHITE);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(Color.parseColor("#4DFFFFFF"));

        waveAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        waveAnimator.setDuration(1200);
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setInterpolator(new LinearInterpolator());
        waveAnimator.addUpdateListener(anim -> {
            if (isWavy) {
                phase = (float) anim.getAnimatedValue();
                invalidate();
            }
        });
    }

    public void setWavy(boolean wavy) {
        this.isWavy = wavy;
        if (wavy && !waveAnimator.isRunning()) {
            waveAnimator.start();
        } else if (!wavy && waveAnimator.isRunning()) {
            waveAnimator.cancel();
            phase = 0;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: animateThumb(1.5f); break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: animateThumb(1f); break;
        }
        return super.onTouchEvent(event);
    }

    private void animateThumb(float targetScale) {
        ValueAnimator anim = ValueAnimator.ofFloat(thumbScale, targetScale);
        anim.setDuration(250);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> { thumbScale = (float) a.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float centerY = getHeight() / 2f;
        float progressRatio = getMax() > 0 ? (float) getProgress() / getMax() : 0;
        float progressX = getPaddingLeft() + (width * progressRatio);

        if (progressX < getWidth() - getPaddingRight()) {
            canvas.drawLine(progressX, centerY, getWidth() - getPaddingRight(), centerY, trackPaint);
        }

        wavePath.reset();
        wavePath.moveTo(getPaddingLeft(), centerY);

        int segments = (int) (progressX - getPaddingLeft());
        if (segments > 0) {
            for (int i = 0; i <= segments; i++) {
                float x = getPaddingLeft() + i;
                float distanceToThumb = progressX - x;
                float taperFactor = Math.min(1f, distanceToThumb / (waveLength / 1.5f));
                // If not wavy, amplitude is 0 (flat line)
                float currentAmplitude = isWavy ? (amplitude * taperFactor) : 0;

                float relativeX = x / waveLength;
                float y = centerY + (float) (Math.sin(relativeX * Math.PI * 2 - phase) * currentAmplitude);

                if (i == 0) wavePath.moveTo(x, y);
                else wavePath.lineTo(x, y);
            }
            canvas.drawPath(wavePath, wavePaint);
        }

        float currentThumbRadius = thumbRadius * thumbScale;
        float currentGlowRadius = thumbGlowRadius * thumbScale;
        canvas.drawCircle(progressX, centerY, currentGlowRadius, glowPaint);
        canvas.drawCircle(progressX, centerY, currentThumbRadius, thumbPaint);
    }

    public void setAccentColor(int color) {
        wavePaint.setColor(color);
        thumbPaint.setColor(color);
        glowPaint.setColor(Color.argb(77, Color.red(color), Color.green(color), Color.blue(color)));
        invalidate();
    }
}
