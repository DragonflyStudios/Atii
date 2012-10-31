package ca.dragonflystudios.atii;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class GestureDetectionView extends View implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private boolean mScaling; // Whether the user is currently pinch zooming

    private void init(Context context) {
        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setIsLongpressEnabled(true);
        mGestureDetector.setOnDoubleTapListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    public GestureDetectionView(Context context) {
        super(context);
        init(context);
    }

    public GestureDetectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GestureDetectionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    //
    // GestureDetector.OnGestureListener implementation
    //

    @Override
    public boolean onDown(MotionEvent arg0) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onDown -- (" + arg0.getX() + ", " + arg0.getY() + ")");

        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getName(), "onFling -- " + "e1: (" + e1.getX() + ", " + e1.getY() + ")\t\t" + "e2: (" + e2.getX()
                    + ", " + e2.getY() + ")\t\t" + "velocity: <" + velocityX + ", " + velocityY + ">");

        return true;
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

        return true;
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
    // GestureDetector.OnGestureListener implementation
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

        return true;
    }
}
