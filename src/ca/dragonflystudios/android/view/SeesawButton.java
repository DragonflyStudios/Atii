package ca.dragonflystudios.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import ca.dragonflystudios.atii.R;

public class SeesawButton extends ImageButton {

    public SeesawButton(Context context) {
        super(context);
        setSaw(true);
    }

    public SeesawButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSaw(true);
    }

    public void setSaw(boolean isSaw) {
        mIsSaw = isSaw;
    }

    public boolean isSaw() {
        return mIsSaw;
    }

    public void seesaw() {
        mIsSaw = !mIsSaw;
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);

        if (mIsSaw) {
            mergeDrawableStates(drawableState, STATE_SAW);
        }
        return drawableState;
    }

    private boolean mIsSaw;
    private static final int[] STATE_SAW = { R.attr.state_saw };
}
