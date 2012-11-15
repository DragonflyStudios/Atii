package ca.dragonflystudios.atii.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import ca.dragonflystudios.atii.control.ReaderPerspective;
import ca.dragonflystudios.atii.control.tiling.ReaderTile;
import ca.dragonflystudios.atii.view.ReaderView.DrawingDelgate;

public class ReaderViewDrawer implements DrawingDelgate {

    public ReaderViewDrawer(ReaderPerspective rwp) {
        mPaint = new Paint();
        mReaderWorldPerspective = rwp;
    }

    @Override
    // DrawingDelegate implementation
    public void draw(Canvas canvas) {
        canvas.save();

        float scale = mReaderWorldPerspective.getWorldToViewScale();
        RectF worldWindow = mReaderWorldPerspective.getWorldWindow();
        canvas.scale(scale, scale);
        canvas.translate(-worldWindow.left, -worldWindow.top);

        for (ReaderTile worldTile : mReaderWorldPerspective.getCurrentWorldTiles()) {
            int levelR = 255 - (int) ((255f * worldTile.rowIndex) / worldTile.totalRows);
            int levelGB = 255 - (int) ((255f * worldTile.columnIndex) / worldTile.totalColumns);
            mPaint.setColor(Color.argb(255, levelR, levelGB, levelGB));
            canvas.drawRect(worldTile.tileRect, mPaint);
            if (worldTile.isPending()) {
                worldTile.draw(canvas, null, mPaint);
                Log.w(getClass().getName(), "drawing pending tile: <" + worldTile.columnIndex + ", " + worldTile.rowIndex + ">");
            }
        }
        canvas.restore();

        for (ReaderTile worldTile : mReaderWorldPerspective.getCurrentWorldTiles())
            if (worldTile.isReady()) {
                worldTile.draw(canvas, mReaderWorldPerspective.getViewRectForWorldRect(worldTile.tileRect), mPaint);
                Log.w(getClass().getName(), "drawing ready tile: <" + worldTile.columnIndex + ", " + worldTile.rowIndex + ">");
            }
    }

    private ReaderPerspective mReaderWorldPerspective;
    private Paint mPaint;
}
