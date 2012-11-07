package ca.dragonflystudios.atii;

import java.util.ArrayList;

import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderPerspective.TilingDelegate;
import ca.dragonflystudios.atii.ReaderWorld.TileDrawableCallback;

/*
 * TODOs:
 *      [x] Tiled drawing
 *          1. Given WorldWindow and Viewport ... return a series of tile specs
 *          2. Iterate through tile speces and draw them
 *      [x] test on very lengthy content ...
 *      [x] Factor out the Tiling algorithm stuff
 *      
 *      [x] Reuse old tiles
 *      
 *      [x] TileState: pending & ready
 *          [x] Default drawable
 *          [x] used if pending: draw gray ... *
 *
 *      [2] Asynchronous Tile management
 *      - request; random wait; ready; call draw(Canvas) on UI thread
 *        - Note that the drawing here should be tile specific ... 
 *      
 *      [4] Predictive fetching (the 16 tiles), cancelling, and cacheing
 *      
 *      [5] Bitmaps as drawables [precise rendering]
 *      
 *      [6] Blend images and drawings ...
 *      - Two kinds of drawings: part of scene (sprite?) and part of screen.
 */

public class ReaderPerspectiveTiler implements TilingDelegate, TileDrawableCallback {

    public interface OnTileReadyListener {
        public void onTileReady(ReaderTile tile);
    }

    public interface TileDrawableSource {
        public void requestDrawableForTile(ReaderTile tile, TileDrawableCallback callback);
    }

    public static final float TILING_PADDING_X = 50f;
    public static final float TILING_PADDING_Y = 50f;
    public static final int MIN_TILE_COLUMNS = 2;
    public static final int MIN_TILE_ROWS = 2;

    public ReaderPerspectiveTiler(TileDrawableSource tds, OnTileReadyListener listener) {
        mTileDrawableSource = tds;
        mOnTileReadyListener = listener;
        mCurrentTiles = new ArrayList<ReaderTile>();
        mNewTiles = new ArrayList<ReaderTile>();
    }

    @Override
    // implements TilingDelegate
    public ArrayList<ReaderTile> getCurrentTiles() {
        return mCurrentTiles;
    }

    @Override
    // implements TilingDelegate
    public void updateCurrentTiles(RectF worldRect, RectF worldWindow, float scale) {
        int oldColumnStart = mColumnStart;
        int oldRowStart = mRowStart;
        int oldColumnCount = mColumnCount;
        int oldRowCount = mRowCount;

        mColumnStart = (int) ((worldWindow.left - worldRect.left) / mTileWorldWidth); // floor
        mRowStart = (int) ((worldWindow.top - worldRect.top) / mTileWorldHeight); // floor

        mTileStartX = worldRect.left + mColumnStart * mTileWorldWidth;
        mTileStartY = worldRect.top + mRowStart * mTileWorldHeight;
        mColumnCount = Math.round((worldWindow.right - mTileStartX) / mTileWorldWidth + 0.5f); // ceiling
        mRowCount = Math.round((worldWindow.bottom - mTileStartY) / mTileWorldHeight + 0.5f); // ceiling

        if (!mCurrentTiles.isEmpty() && mColumnStart == oldColumnStart && mRowStart == oldRowStart
                && mColumnCount == oldColumnCount && mRowCount == oldRowCount)
            return;

        // These two are for debugging, otherwise unnecessary
        int totalColumns = Math.round((worldRect.right - worldRect.left) / mTileWorldWidth + 0.5f); // ceiling
        int totalRows = Math.round((worldRect.bottom - worldRect.top) / mTileWorldHeight + 0.5f); // ceiling

        for (ReaderTile tile : mCurrentTiles)
            if (ReaderTile.inRange(tile.columnIndex, tile.rowIndex, mColumnStart, mRowStart, mColumnCount, mRowCount))
                mNewTiles.add(tile);
            else
                tile.setFree();

        float currentTileLeft = mTileStartX, currentTileTop = mTileStartY;
        for (int i = 0; i < mRowCount; i++, currentTileLeft = mTileStartX, currentTileTop += mTileWorldHeight)
            for (int j = 0; j < mColumnCount; j++, currentTileLeft += mTileWorldWidth) {
                if (!mCurrentTiles.isEmpty()
                        && ReaderTile.inRange(mColumnStart + j, mRowStart + i, oldColumnStart, oldRowStart, oldColumnCount,
                                oldRowCount))
                    continue;

                RectF rect = new RectF(currentTileLeft, currentTileTop, currentTileLeft + mTileWorldWidth, currentTileTop
                        + mTileWorldHeight);

                ReaderTile tile = new ReaderTile(scale, rect, mColumnStart + j, mRowStart + i);
                // These two are for debugging, otherwise unnecessary
                tile.totalColumns = totalColumns;
                tile.totalRows = totalRows;
                mNewTiles.add(tile);

            }

        // "double buffer"
        mCurrentTiles.clear();
        ArrayList<ReaderTile> tmp = mCurrentTiles;
        mCurrentTiles = mNewTiles;
        mNewTiles = tmp;

        for (ReaderTile tile : mCurrentTiles) {
            if (tile.isNew())
                mTileDrawableSource.requestDrawableForTile(tile, this);
        }
    }

    @Override
    // implements TilingDelegate
    public void retile(RectF worldRect, RectF worldWindow, RectF viewport, float worldToViewScale) {
        mCurrentTiles.clear();
        updateTilingParams(viewport, worldToViewScale);
        updateCurrentTiles(worldRect, worldWindow, worldToViewScale);
    }

    @Override
    // implements TileDrawableCallback
    public void onTileDrawableReady(ReaderTile tile) {
        if (mCurrentTiles.contains(tile))
            mOnTileReadyListener.onTileReady(tile);
    }

    private void updateTilingParams(RectF viewport, float worldToViewScale) {
        mTileViewportWidth = (viewport.width() + TILING_PADDING_X) / MIN_TILE_COLUMNS;
        mTileViewportHeight = (viewport.height() + TILING_PADDING_Y) / MIN_TILE_ROWS;
        mTileWorldWidth = mTileViewportWidth / worldToViewScale;
        mTileWorldHeight = mTileViewportHeight / worldToViewScale;
    }

    private OnTileReadyListener mOnTileReadyListener;
    private TileDrawableSource mTileDrawableSource;
    private ArrayList<ReaderTile> mCurrentTiles;
    private ArrayList<ReaderTile> mNewTiles;
    private float mTileViewportWidth, mTileViewportHeight;
    private float mTileWorldWidth, mTileWorldHeight;
    private int mColumnStart, mRowStart;
    private float mTileStartX, mTileStartY;
    private int mColumnCount, mRowCount;
}
