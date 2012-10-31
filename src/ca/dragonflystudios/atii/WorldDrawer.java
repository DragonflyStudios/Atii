package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.RectF;

public class WorldDrawer implements ViewportSizeChangeListener, WorldWindowChangeRequestListener,
        WorldDrawingView.WorldDrawingDelegate {

    public interface WorldWindowDelegate {
        public float getLimitMinX(RectF worldWindow);

        public float getLimitMinY(RectF worldWindow);

        public float getLimitMaxX(RectF worldWindow);

        public float getLimitMaxY(RectF worldWindow);
    }

    public interface DrawableWorld {
        public void draw(Canvas canvas, RectF worldWindow);
    }

    public WorldDrawer(DrawableWorld dw, WorldWindowDelegate wd) {
        mViewport = new RectF();
        mViewport.left = 0f;
        mViewport.top = 0f;

        mDrawableWorld = dw;
        mWorldWindow = new RectF();
        mWorldWindowDelegate = wd;
    }

    public static final float MIN_SCALE_FACTOR = 0.1f;
    public static final float MAX_SCALE_FACTOR = 0.5f;

    private RectF mViewport;

    private DrawableWorld mDrawableWorld;
    private RectF mWorldWindow;
    private WorldWindowDelegate mWorldWindowDelegate;

    private float mWorldToViewScale;

    // ViewportSizeChangeListener implementation
    @Override
    public void onViewportSizeChanged(float newWidth, float newHeight) {
        mViewport.right = newWidth;
        mViewport.bottom = newHeight;
        mWorldWindow.right = mWorldWindow.left + newWidth / mWorldToViewScale;
        mWorldWindow.bottom = mWorldWindow.top + newHeight / mWorldToViewScale;
    }

    // WorldWindowChangeRequestListener implementation
    @Override
    public void onRequestWorldWindowTranslation(float deltaX, float deltaY) {
        mWorldWindow.offset(deltaX / mWorldToViewScale, deltaY / mWorldToViewScale);
        mWorldWindow.offsetTo(
                Math.max(mWorldWindowDelegate.getLimitMinX(mWorldWindow),
                        Math.min(mWorldWindow.left, mWorldWindowDelegate.getLimitMaxX(mWorldWindow))),
                Math.max(mWorldWindowDelegate.getLimitMinY(mWorldWindow),
                        Math.min(mWorldWindow.top, mWorldWindowDelegate.getLimitMaxY(mWorldWindow))));
    }

    // WorldWindowChangeRequestListener implementation
    @Override
    public void onRequestWorldWindowScaling(float scaleBy, float focusX, float focusY) {
        // assuming origin of viewport is (0.0, 0.0)
        final float WorldWindowFocusX = mWorldWindow.left + focusX / mWorldToViewScale;
        final float WorldWindowFocusY = mWorldWindow.top + focusY / mWorldToViewScale;

        float previousScaleFactor = mWorldToViewScale;
        mWorldToViewScale *= scaleBy;
        mWorldToViewScale = Math.max(MIN_SCALE_FACTOR, Math.min(mWorldToViewScale, MAX_SCALE_FACTOR));

        // "zooming in", i.e. scaleBy > 1.0, means a smaller world WorldWindow
        final float scaling = previousScaleFactor / mWorldToViewScale;
        mWorldWindow.offset(WorldWindowFocusX - WorldWindowFocusX * scaling, WorldWindowFocusY - WorldWindowFocusY * scaling);
        mWorldWindow.right = mWorldWindow.left + (mWorldWindow.right - mWorldWindow.left) * scaling;
        mWorldWindow.bottom = mWorldWindow.top + (mWorldWindow.bottom - mWorldWindow.top) * scaling;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.translate(mWorldWindow.left * mWorldToViewScale, mWorldWindow.top * mWorldToViewScale);
        canvas.scale(mWorldToViewScale, mWorldToViewScale);
        mDrawableWorld.draw(canvas, mWorldWindow);
        canvas.restore();
    }
}
