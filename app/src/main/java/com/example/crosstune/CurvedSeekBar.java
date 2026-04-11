package com.example.crosstune;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A custom View that draws a curved (arc) seek bar.
 * The curve bows upward in the center, matching the "Now Playing" design.
 */
public class CurvedSeekBar extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint thumbPaint;

    private Path fullCurvePath;
    private Path progressPath;
    private PathMeasure pathMeasure;

    private int max = 100;
    private int progress = 0;
    private float thumbRadius = 8f;

    private OnSeekBarChangeListener listener;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(CurvedSeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(CurvedSeekBar seekBar);
        void onStopTrackingTouch(CurvedSeekBar seekBar);
    }

    public CurvedSeekBar(Context context) {
        super(context);
        init();
    }

    public CurvedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurvedSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Background track paint (dark grey thin line)
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(dpToPx(2f));
        trackPaint.setColor(0xFF3A3A3A);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        // Progress paint (white thin line)
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dpToPx(2f));
        progressPaint.setColor(0xFFFFFFFF);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // Thumb paint (white filled circle)
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(0xFFFFFFFF);

        thumbRadius = dpToPx(6f);

        fullCurvePath = new Path();
        progressPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildCurvePath(w, h);
    }

    /**
     * Builds a quadratic bezier that starts at bottom-left,
     * bows upward in the center, and ends at bottom-right.
     */
    private void buildCurvePath(int width, int height) {
        fullCurvePath.reset();

        float padding = thumbRadius + dpToPx(2f);
        float startX = padding;
        float endX = width - padding;
        float baseY = height - padding;          // bottom of the view
        float peakY = padding;                    // top of the view (peak of curve)
        float midX = (startX + endX) / 2f;

        // Quadratic bezier: start -> control point at top center -> end
        fullCurvePath.moveTo(startX, baseY);
        fullCurvePath.quadTo(midX, peakY, endX, baseY);

        pathMeasure = new PathMeasure(fullCurvePath, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pathMeasure == null) return;

        // Draw full background track
        canvas.drawPath(fullCurvePath, trackPaint);

        // Draw progress portion
        float fraction = (max > 0) ? (float) progress / max : 0f;
        float length = pathMeasure.getLength();
        float progressLength = length * fraction;

        progressPath.reset();
        pathMeasure.getSegment(0, progressLength, progressPath, true);
        canvas.drawPath(progressPath, progressPaint);

        // Draw thumb at the progress position
        float[] pos = new float[2];
        pathMeasure.getPosTan(progressLength, pos, null);
        canvas.drawCircle(pos[0], pos[1], thumbRadius, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (pathMeasure == null) return super.onTouchEvent(event);

        float touchX = event.getX();
        float padding = thumbRadius + dpToPx(2f);
        float trackWidth = getWidth() - 2 * padding;

        // Map touch X to progress (simple horizontal mapping)
        float fraction = (touchX - padding) / trackWidth;
        fraction = Math.max(0f, Math.min(1f, fraction));
        int newProgress = Math.round(fraction * max);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (listener != null) listener.onStartTrackingTouch(this);
                setProgress(newProgress);
                if (listener != null) listener.onProgressChanged(this, newProgress, true);
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                setProgress(newProgress);
                if (listener != null) listener.onProgressChanged(this, newProgress, true);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setProgress(newProgress);
                if (listener != null) listener.onProgressChanged(this, newProgress, true);
                if (listener != null) listener.onStopTrackingTouch(this);
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    // ---- Public API ----

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }

    public int getMax() {
        return max;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(max, progress));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    // ---- Utilities ----

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
