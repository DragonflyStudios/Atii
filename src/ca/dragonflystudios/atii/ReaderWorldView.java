package ca.dragonflystudios.atii;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public class ReaderWorldView extends View {

    public interface WorldDrawingDelegate {
        public void draw(Canvas canvas);
    }

    public interface ViewportSizeChangeListener {
        public void onViewportSizeChanged(float newWidth, float newHeight);
    }

    WorldDrawingDelegate mWorldDrawingDelegate;
    ViewportSizeChangeListener mViewportSizeChangeListener;

    public ReaderWorldView(Context context) {
        super(context);
        mPreviousWidth = 0;
        mPreviousHeight = 0;
    }

    public void setWorldDrawingDelegate(WorldDrawingDelegate delegate) {
        mWorldDrawingDelegate = delegate;
    }

    public void setViewportSizeChnageListener(ViewportSizeChangeListener listener) {
        mViewportSizeChangeListener = listener;
    }

    private int mPreviousWidth, mPreviousHeight;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            final int width = right - left + 1;
            final int height = bottom - top + 1;

            if (width != mPreviousWidth || height != mPreviousHeight) {
                mViewportSizeChangeListener.onViewportSizeChanged(width, height);
                mPreviousWidth = width;
                mPreviousHeight = height;
            }
        }

    }

    @Override
    public void onDraw(Canvas canvas) {
        mWorldDrawingDelegate.draw(canvas);

        // Draw other things
    }
}
