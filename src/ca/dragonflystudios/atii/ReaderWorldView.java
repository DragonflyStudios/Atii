package ca.dragonflystudios.atii;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import ca.dragonflystudios.atii.ReaderGestureView.ReaderGestureListener;

public class ReaderWorldView extends View implements ReaderGestureListener {

    public interface WorldDrawingDelegate {
        public void draw(Canvas canvas);
    }

    public Paint getPaint() {
        return mPaint;
    }

    public ReaderWorldView(Context context, ReaderWorldPerspective rwp) {
        super(context);
        mPaint = new Paint();
        mReaderWorldPerspective = rwp;
    }

    public void setWorldDrawingDelegate(WorldDrawingDelegate wdd) {
        mWorldDrawingDelegate = wdd;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed && mReaderWorldPerspective.updateViewport(left, top, right, bottom)) {
            mPaint.setStrokeWidth(1 / mReaderWorldPerspective.getWorldToViewScale());
            invalidate();
        }
    }

    // WorldWindowChangeRequestListener implementation
    @Override
    public void onPanning(float deltaX, float deltaY) {
        if (mReaderWorldPerspective.panWorldWindow(deltaX, deltaY))
            invalidate();
    }

    // WorldWindowChangeRequestListener implementation
    @Override
    public void onScaling(float scaleBy, float focusX, float focusY) {
        if (mReaderWorldPerspective.scaleWorldWindow(scaleBy, focusX, focusY)) {
            mPaint.setStrokeWidth(1 / mReaderWorldPerspective.getWorldToViewScale());
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        mWorldDrawingDelegate.draw(canvas);

        // Draw other things
    }

    private Paint mPaint;
    private WorldDrawingDelegate mWorldDrawingDelegate;
    private ReaderWorldPerspective mReaderWorldPerspective;
}
