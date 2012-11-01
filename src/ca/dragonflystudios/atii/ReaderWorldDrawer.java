package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderWorldView.ViewportSizeChangeListener;
import ca.dragonflystudios.atii.ReaderWorldView.WorldDrawingDelegate;

public class ReaderWorldDrawer implements ViewportSizeChangeListener, WorldDrawingDelegate, ReaderGestureView.ReaderGestureListener {

    public interface WorldWindowDelegate {
        public float getLimitMinX(RectF worldWindow);

        public float getLimitMinY(RectF worldWindow);

        public float getLimitMaxX(RectF worldWindow);

        public float getLimitMaxY(RectF worldWindow);

        public RectF getWorldRect();
    }

    public interface DrawableWorld {
        public void draw(Canvas canvas, RectF worldWindow, Paint paint);
    }

    public ReaderWorldDrawer(DrawableWorld dw, WorldWindowDelegate wd, ReaderWorldView rwv) {
        mViewport = new RectF();
        mViewport.left = 0f;
        mViewport.top = 0f;

        mDrawableWorld = dw;
        mWorldWindowDelegate = wd;
        mWorldWindow = wd.getWorldRect();

        mReaderWorldView = rwv;
        mReaderWorldView.setWorldDrawingDelegate(this);
        mReaderWorldView.setViewportSizeChnageListener(this);

        mWorldToViewScale = -1f;
        mPaint = new Paint();
    }

    public static final float MIN_SCALE_FACTOR = 10f;
    public static final float MAX_SCALE_FACTOR = 500f;

    private RectF mViewport;

    private DrawableWorld mDrawableWorld;
    private RectF mWorldWindow;
    private WorldWindowDelegate mWorldWindowDelegate;

    private ReaderWorldView mReaderWorldView;

    private float mWorldToViewScale;
    private Paint mPaint;

    // ViewportSizeChangeListener implementation
    @Override
    public void onViewportSizeChanged(float newWidth, float newHeight) {
        if (mWorldToViewScale <= 0) {
            mWorldToViewScale = newWidth / (mWorldWindow.right - mWorldWindow.left); // fit width
            mWorldToViewScale = Math.max(MIN_SCALE_FACTOR, Math.min(mWorldToViewScale, MAX_SCALE_FACTOR));
        }

        mViewport.right = newWidth;
        mViewport.bottom = newHeight;
        mWorldWindow.right = mWorldWindow.left + newWidth / mWorldToViewScale;
        mWorldWindow.bottom = mWorldWindow.top + newHeight / mWorldToViewScale;

        mPaint.setStrokeWidth(1/mWorldToViewScale);

        mReaderWorldView.invalidate();
    }

    // WorldWindowChangeRequestListener implementation
    @Override
    public void onPanning(float deltaX, float deltaY) {
        mWorldWindow.offset(-deltaX / mWorldToViewScale, -deltaY / mWorldToViewScale);
        /*
        mWorldWindow.offsetTo(
                Math.max(mWorldWindowDelegate.getLimitMinX(mWorldWindow),
                        Math.min(mWorldWindow.left, mWorldWindowDelegate.getLimitMaxX(mWorldWindow))),
                Math.max(mWorldWindowDelegate.getLimitMinY(mWorldWindow),
                        Math.min(mWorldWindow.top, mWorldWindowDelegate.getLimitMaxY(mWorldWindow))));
        */
        mReaderWorldView.invalidate();
    }

    // WorldWindowChangeRequestListener implementation
    @Override
    public void onScaling(float scaleBy, float focusX, float focusY) {
        // assuming origin of viewport is (0.0, 0.0)
        final float WorldWindowFocusX = mWorldWindow.left + focusX / mWorldToViewScale;
        final float WorldWindowFocusY = mWorldWindow.top + focusY / mWorldToViewScale;

        float previousScaleFactor = mWorldToViewScale;
        mWorldToViewScale *= scaleBy;
        mWorldToViewScale = Math.max(MIN_SCALE_FACTOR, Math.min(mWorldToViewScale, MAX_SCALE_FACTOR));
        mPaint.setStrokeWidth(1/mWorldToViewScale);

        // "zooming in", i.e. scaleBy > 1.0, means a smaller world WorldWindow
        final float scaling = previousScaleFactor / mWorldToViewScale;
        mWorldWindow.offset(-WorldWindowFocusX, -WorldWindowFocusY);
        mWorldWindow.set(mWorldWindow.left * scaling, mWorldWindow.top * scaling, mWorldWindow.right * scaling, mWorldWindow.bottom * scaling);
        mWorldWindow.offset(WorldWindowFocusX * scaling, WorldWindowFocusY * scaling);
        /*
        mWorldWindow.offsetTo(
                Math.max(mWorldWindowDelegate.getLimitMinX(mWorldWindow),
                        Math.min(mWorldWindow.left, mWorldWindowDelegate.getLimitMaxX(mWorldWindow))),
                Math.max(mWorldWindowDelegate.getLimitMinY(mWorldWindow),
                        Math.min(mWorldWindow.top, mWorldWindowDelegate.getLimitMaxY(mWorldWindow))));
        */
        mReaderWorldView.invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.scale(mWorldToViewScale, mWorldToViewScale);
        mDrawableWorld.draw(canvas, mWorldWindow, mPaint);
        canvas.restore();
    }
}
