package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderWorldPerspective.Tile;
import ca.dragonflystudios.atii.ReaderWorldView.WorldDrawingDelegate;

/*
 * TODO:
 *      [ ] Tiled drawing
 *          1. Given WorldWindow and Viewport ... return a series of tile specs
 *          2. Iterate through tile speces and draw them
 *      [ ] Asynchronous preparation of tile images
 */
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
        RectF worldWindow = mReaderWorldPerspective.getWorldWindow();
        canvas.translate(-worldWindow.left, -worldWindow.top);

        Paint paint = mReaderWorldView.getPaint();

        for (Tile worldTile : mReaderWorldPerspective.getCurrentWorldTiles()) {
            int level = worldTile.columnIndex * 20 + worldTile.rowIndex * 90;
            paint.setColor(Color.argb(255, level, level, level));
            canvas.drawRect(worldTile.tileRect, paint);

            mDrawableWorld.draw(canvas, worldTile.tileRect, paint);
        }

        canvas.restore();
    }

    private DrawableWorld mDrawableWorld;
    private ReaderWorldView mReaderWorldView;
    private ReaderWorldPerspective mReaderWorldPerspective;
}
