package ca.dragonflystudios.atii.player;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import ca.dragonflystudios.atii.BuildConfig;
import ca.dragonflystudios.atii.view.ReaderGestureView.ReaderGestureListener;

public class AtiiViewPager extends ViewPager implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    public AtiiViewPager(Context context) {
        super(context);
        init();
    }

    public AtiiViewPager(Context context, AttributeSet attributes) {
        super(context, attributes);
        init();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
       return super.onInterceptTouchEvent(ev);
    }

    private void init() {
        mGestureDetector = new GestureDetector(getContext(), this);
        mGestureDetector.setIsLongpressEnabled(true);
        mGestureDetector.setOnDoubleTapListener(this);
    }

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

    private ReaderGestureListener mListener;

    private GestureDetector mGestureDetector;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mGestureDetector.onTouchEvent(event))
            return super.onTouchEvent(event);

        return true;
    }

}