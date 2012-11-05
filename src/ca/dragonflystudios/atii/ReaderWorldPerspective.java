package ca.dragonflystudios.atii;

import android.graphics.RectF;

// TODO: part of the data model; should be persisted

/*
 * (1) Shape of Viewport determines shape of WorldWindow, which, in the simple case, is the X/Y ratio
 * (2) Size of WorldWindow determines how much content is shown
 * (3) Origin of WorldWindow determines which content is shown
 * (4) Size of Viewport in terms of pixels is size of bitmap
 *     (4.1) Viewport shows and only shows everything in WorldWindow at a resolution (# of X & Y pixels) determined by size of Viewport
 * (5) This means elimination of "WorldToViewScale"
 * (6) This also means there needs to be an initial "handshaking" through which initial WorldWindow size is determined:
 *     (6.1) Call "Perspective" the combination of Viewport size and WorldWindow
 *     (6.2) Perspective is persisted
 *     (6.3) If Perspective is never initialized (persisted), then "fit-to-width" to the given Viewport size
 *     (6.4) If Perspective is persisted, then update Viewport to the newly given one
 */

public class ReaderWorldPerspective {

    public static final float MIN_SCALE_FACTOR = 10f;
    public static final float MAX_SCALE_FACTOR = 500f;

    public interface WorldWindowDelegate {
        public float getLimitMinX(RectF worldWindow);
        public float getLimitMinY(RectF worldWindow);
        public float getLimitMaxX(RectF worldWindow);
        public float getLimitMaxY(RectF worldWindow);
        public RectF getWorldRect();
    }

    public ReaderWorldPerspective(WorldWindowDelegate wdd) {
        mInitialized = false;
        mWorldWindowDelegate = wdd;
    }

    public boolean updateViewport(float left, float top, float right, float bottom) {
        if (!mInitialized) {
            mViewport = new RectF(left, top, right, bottom);

            // fit width
            mWorldWindow = new RectF(mWorldWindowDelegate.getWorldRect());
            mWorldToViewScale = (right - left + 1) / (mWorldWindow.right - mWorldWindow.left);
            mWorldToViewScale = Math.max(MIN_SCALE_FACTOR, Math.min(mWorldToViewScale, MAX_SCALE_FACTOR));
            mInitialized = true;
        } else if (left == mViewport.left && top == mViewport.top && right == mViewport.right && bottom == mViewport.bottom)
            return false;

        mViewport.left = left;
        mViewport.top = top;
        mViewport.right = right;
        mViewport.bottom = bottom;
        mWorldWindow.right = mWorldWindow.left + (right - left + 1) / mWorldToViewScale;
        mWorldWindow.bottom = mWorldWindow.top + (bottom - top + 1) / mWorldToViewScale;
        
        return true;
    }

    public boolean panWorldWindow(float deltaX, float deltaY) {
        if (!mInitialized)
            throw new RuntimeException("ReaderWorldPerspective is used before initialization");

        float oldLeft = mWorldWindow.left, oldTop = mWorldWindow.top;
        mWorldWindow.offset(deltaX / mWorldToViewScale, deltaY / mWorldToViewScale);
        mWorldWindow.offsetTo(
                Math.max(mWorldWindowDelegate.getLimitMinX(mWorldWindow),
                        Math.min(mWorldWindow.left, mWorldWindowDelegate.getLimitMaxX(mWorldWindow))),
                Math.max(mWorldWindowDelegate.getLimitMinY(mWorldWindow),
                        Math.min(mWorldWindow.top, mWorldWindowDelegate.getLimitMaxY(mWorldWindow))));
        return (oldLeft != mWorldWindow.left || oldTop != mWorldWindow.top);
    }

    public boolean scaleWorldWindow(float scaleBy, float focusX, float focusY) {
        if (!mInitialized)
            throw new RuntimeException("ReaderWorldPerspective is used before initialization");

        float oldLeft = mWorldWindow.left, oldTop = mWorldWindow.top, oldRight = mWorldWindow.right, oldBottom = mWorldWindow.bottom;

        final float WorldWindowFocusX = oldLeft + (focusX - mViewport.left) / mWorldToViewScale;
        final float WorldWindowFocusY = oldTop + (focusY - mViewport.top) / mWorldToViewScale;

        float previousScaleFactor = mWorldToViewScale;
        mWorldToViewScale *= scaleBy;
        mWorldToViewScale = Math.max(MIN_SCALE_FACTOR, Math.min(mWorldToViewScale, MAX_SCALE_FACTOR));

        // "zooming in", i.e. scaleBy > 1.0, means a smaller world WorldWindow
        final float scaling = previousScaleFactor / mWorldToViewScale;
        mWorldWindow.offset(-WorldWindowFocusX, -WorldWindowFocusY);
        mWorldWindow.set(mWorldWindow.left * scaling, mWorldWindow.top * scaling, mWorldWindow.right * scaling, mWorldWindow.bottom
                * scaling);
        mWorldWindow.offset(WorldWindowFocusX, WorldWindowFocusY);
        mWorldWindow.offsetTo(
                Math.max(mWorldWindowDelegate.getLimitMinX(mWorldWindow),
                        Math.min(mWorldWindow.left, mWorldWindowDelegate.getLimitMaxX(mWorldWindow))),
                Math.max(mWorldWindowDelegate.getLimitMinY(mWorldWindow),
                        Math.min(mWorldWindow.top, mWorldWindowDelegate.getLimitMaxY(mWorldWindow))));

        return (oldLeft != mWorldWindow.left || oldTop != mWorldWindow.top || oldRight != mWorldWindow.right || oldBottom != mWorldWindow.bottom);
    }

    public float getWorldToViewScale() {
        return mWorldToViewScale;
    }

    public RectF getWorldWindow() {
        return mWorldWindow;
    }

    public RectF getViewport() {
        return mViewport;
    }

    private WorldWindowDelegate mWorldWindowDelegate;
    private RectF mViewport;
    private RectF mWorldWindow;
    private float mWorldToViewScale;
    private boolean mInitialized;
}
