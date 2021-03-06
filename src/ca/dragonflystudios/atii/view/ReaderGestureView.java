package ca.dragonflystudios.atii.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;
import ca.dragonflystudios.atii.BuildConfig;

public class ReaderGestureView extends FrameLayout implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {

    public interface ReaderGestureListener {
        // in view coordinates
        public void onPanning(float deltaX, float deltaY);

        // in view coordinates
        public void onScaling(float scaling, float focusX, float focusY);
        
        public void onSingleTap(float x, float y);
    }

    public ReaderGestureView(Context context) {
        super(context);
        init();
    }

    public ReaderGestureView(Context context, AttributeSet attributes) {
        super(context, attributes);
        init();
    }

    public ReaderGestureView(Context context, AttributeSet attributes, int style) {
        super(context, attributes, style);
        init();
    }

    private void init() {
        mGestureDetector = new GestureDetector(getContext(), this);
        mGestureDetector.setIsLongpressEnabled(true);
        mGestureDetector.setOnDoubleTapListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
    }

    private ReaderGestureListener mListener;

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private boolean mScaling; // Whether the user is currently pinch zooming

    public void setReaderGestureListener(ReaderGestureListener listener) {
        mListener = listener;
    }

    //
    // GestureDetector.OnGestureListener implementation
    //

    @Override
    public boolean onDown(MotionEvent arg0) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onDown -- (" + arg0.getX() + ", " + arg0.getY() + ")");

        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onFling -- " + "e1: (" + e1.getX() + ", " + e1.getY() + ")\t\t" + "e2: (" + e2.getX()
                    + ", " + e2.getY() + ")\t\t" + "velocity: <" + velocityX + ", " + velocityY + ">");

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onLongPress -- (" + e.getX() + ", " + e.getY() + ")");
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onScroll -- " + "e1: (" + e1.getX() + ", " + e1.getY() + ")\t\t" + "e2: (" + e2.getX()
                    + ", " + e2.getY() + ")\t\t" + "distance: <" + distanceX + ", " + distanceY + ">");

        if (null != mListener)
            mListener.onPanning(distanceX, distanceY);
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onShowPress -- (" + e.getX() + ", " + e.getY() + ")");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onSingleTapUp -- (" + e.getX() + ", " + e.getY() + ")");

        return false;
    }

    //
    // GestureDetector.OnDoubleTapListener implementation
    //
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onDouleTap -- (" + e.getX() + ", " + e.getY() + ")");

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onDoubleTapEvent -- (" + e.getX() + ", " + e.getY() + ")");

        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onSingleTapConfirmed -- (" + e.getX() + ", " + e.getY() + ")");

        if (null != mListener)
            mListener.onSingleTap(e.getX(), e.getY());
        return true;
    }

    //
    // ScaleGestureDetector.OnScaleGestureListener implementation
    //

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onScaleBegin -- " + "focus: (" + detector.getFocusX() + ", " + detector.getFocusY() + ")"
                    + "scale: " + detector.getScaleFactor());

        mScaling = true;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onScaleBegin -- " + "focus: (" + detector.getFocusX() + ", " + detector.getFocusY() + ")"
                    + "scale: " + detector.getScaleFactor());

        mListener.onScaling(detector.getScaleFactor(), detector.getFocusX() - getLeft(), detector.getFocusY() - getTop());
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onScaleBegin -- " + "focus: (" + detector.getFocusX() + ", " + detector.getFocusY() + ")"
                    + "scale: " + detector.getScaleFactor());

        mScaling = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);

        if (!mScaling)
            mGestureDetector.onTouchEvent(event);

        return false;
    }
}
