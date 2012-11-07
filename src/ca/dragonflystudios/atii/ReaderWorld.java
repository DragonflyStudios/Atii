package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import ca.dragonflystudios.atii.ReaderPerspective.WorldWindowDelegate;

public class ReaderWorld implements WorldWindowDelegate {

    // the "world" is made of GRID_COUNT horizontal and GRID_COUNT vertical grid
    // lines of shades of, respectively, blue and green
    public static final float GRID_COUNT = 64f;

    public static final float CONTENT_LEFT = 0f;
    public static final float CONTENT_RIGHT = 8.5f;
    public static final float CONTENT_TOP = 0f;
    public static final float CONTENT_BOTTOM = 22f;

    public static final float CONTENT_MARGIN = 1f;

    public static final float X_INC = (CONTENT_RIGHT - CONTENT_LEFT) / GRID_COUNT;
    public static final float Y_INC = (CONTENT_BOTTOM - CONTENT_TOP) / GRID_COUNT;
    public static final float COLOR_INC = 255f / GRID_COUNT;

    public static final float GRID_LINE_WIDTH = 6f;

    public ReaderWorld() {
        mDrawingHandlerThread = new HandlerThread("Async Tile Drawing Thread");
        mDrawingHandlerThread.start();
        mDrawingHandler = new Handler(mDrawingHandlerThread.getLooper());
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinX(RectF worldWindow) {
        return CONTENT_LEFT - CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinY(RectF worldWindow) {
        return CONTENT_TOP - CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMaxX(RectF worldWindow) {
        return CONTENT_RIGHT - (worldWindow.right - worldWindow.left) + CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMaxY(RectF worldWindow) {
        return CONTENT_BOTTOM - (worldWindow.bottom - worldWindow.top) + CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public RectF getWorldRect() {
        return new RectF(CONTENT_LEFT - CONTENT_MARGIN, CONTENT_TOP - CONTENT_MARGIN, CONTENT_RIGHT + CONTENT_MARGIN,
                CONTENT_BOTTOM + CONTENT_MARGIN);
    }

    private Handler mDrawingHandler;
    private HandlerThread mDrawingHandlerThread;
    

    public interface ReaderWorldDrawable {
        public void draw(Canvas canvas, Paint paint);
    }

    public interface TileDrawableCallback {
        public void onTileDrawableReady(ReaderTile tile);
    }

    public void requestDrawableForTile(final ReaderTile tile, final TileDrawableCallback callback) {
        if (tile.isReady())
            callback.onTileDrawableReady(tile);
        else
            mDrawingHandler.post(new Runnable() {
                @Override
                public void run() {
                    long procTime = (long)(Math.random() * 5000);
                    try {
                        Thread.sleep(procTime);
                    } catch (InterruptedException e) {};
                    
                    tile.setReady();
                    callback.onTileDrawableReady(tile);
                }
            });
    }

}
