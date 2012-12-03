package ca.dragonflystudios.atii;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class BookItemView extends FrameLayout {

    public BookItemView(Context context) {
        super(context);
    }

    public BookItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BookItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    // Override this one to force equal width and height
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
