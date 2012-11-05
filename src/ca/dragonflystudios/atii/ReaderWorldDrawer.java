package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderWorldView.WorldDrawingDelegate;

public class ReaderWorldDrawer implements WorldDrawingDelegate {

    public interface DrawableWorld {
        public void draw(Canvas canvas, RectF worldWindow, Paint paint);
    }

    public ReaderWorldDrawer(DrawableWorld dw, ReaderWorldView rwv, ReaderWorldPerspective rwp) {
        mDrawableWorld = dw;
        mReaderWorldView = rwv;
        mReaderWorldPerspective = rwp;
    }

    @Override // implements WorldDrawingDelegate
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.scale(mReaderWorldPerspective.getWorldToViewScale(), mReaderWorldPerspective.getWorldToViewScale());
        canvas.translate(-mReaderWorldPerspective.getWorldWindow().left, -mReaderWorldPerspective.getWorldWindow().top);
        mDrawableWorld.draw(canvas, mReaderWorldPerspective.getWorldWindow(), mReaderWorldView.getPaint()); // call tiled drawer with mWorldWindow
        canvas.restore();
    }

    private DrawableWorld mDrawableWorld;
    private ReaderWorldView mReaderWorldView;
    private ReaderWorldPerspective mReaderWorldPerspective;
}
