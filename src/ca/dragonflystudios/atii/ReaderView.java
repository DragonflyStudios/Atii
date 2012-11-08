package ca.dragonflystudios.atii;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;

public class ReaderView extends View {

    public interface DrawingDelgate {
        public void draw(Canvas canvas);
    }

    public interface OnLayoutListener {
        public void onLayout(View view, boolean changed, int left, int top, int right, int bottom);
    }

    public ReaderView(Context context) {
        super(context);
    }

    public void setDrawingDelegate(DrawingDelgate wdd) {
        mWorldDrawingDelegate = wdd;
    }

    public void setOnLayoutListener(OnLayoutListener listener) {
        mOnLayoutListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mOnLayoutListener.onLayout(this, changed, left, top, right, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // Doesn't seem to help!
        mWorldDrawingDelegate.draw(canvas);

        // Draw other things
    }

    private DrawingDelgate mWorldDrawingDelegate;
    private OnLayoutListener mOnLayoutListener;
}
