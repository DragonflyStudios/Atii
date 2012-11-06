package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderTiler.Tile;
import ca.dragonflystudios.atii.ReaderWorldView.WorldDrawingDelegate;

/*
 * TODOs:
 *      [x] Tiled drawing
 *          1. Given WorldWindow and Viewport ... return a series of tile specs
 *          2. Iterate through tile speces and draw them
 *      [x] test on very lengthy content ...
 *      [x] Factor out the Tiling algorithm stuff
 *      
 *      [2] Asynchronous Tile management
 *      - request; random wait; ready; call draw(Canvas) on UI thread
 *      - TileState: pending; ready; in_use; ...
 *      
 *      [3] Predictive fetching (the 16 tiles), cancelling, and cacheing
 *      
 *      [4] Default drawable
 *      - (how to do this?)
 *      
 *      [5] Bitmaps as drawables [precise rendering]
 *      
 *      [6] Blend images and drawings ...
 *      - Two kinds of drawings: part of scene (sprite?) and part of screen.

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

    @Override
    // implements WorldDrawingDelegate
    public void draw(Canvas canvas) {
        canvas.save();

        canvas.scale(mReaderWorldPerspective.getWorldToViewScale(), mReaderWorldPerspective.getWorldToViewScale());
        RectF worldWindow = mReaderWorldPerspective.getWorldWindow();
        canvas.translate(-worldWindow.left, -worldWindow.top);

        Paint paint = mReaderWorldView.getPaint();

        for (Tile worldTile : mReaderWorldPerspective.getCurrentWorldTiles()) {
            /*
            float unit = 255f / (worldTile.totalColumns * worldTile.totalRows);
            int level = 255 - (int)(unit * (worldTile.columnIndex + worldTile.rowIndex * worldTile.totalColumns));
            paint.setColor(Color.argb(255, level, level, level));
            */
            int levelR = 255 - (int)((255f * worldTile.rowIndex) / worldTile.totalRows);
            int levelGB = 255 - (int)((255f * worldTile.columnIndex) / worldTile.totalColumns);
            paint.setColor(Color.argb(255, levelR, levelGB, levelGB));
            canvas.drawRect(worldTile.tileRect, paint);

            mDrawableWorld.draw(canvas, worldTile.tileRect, paint);
        }

        canvas.restore();
    }

    private DrawableWorld mDrawableWorld;
    private ReaderWorldView mReaderWorldView;
    private ReaderWorldPerspective mReaderWorldPerspective;
}
