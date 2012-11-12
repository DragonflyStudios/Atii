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
 *      [x] Asynchronous Tile management
 *      - request; random wait; ready; call draw(Canvas) on UI thread
 *        - Note that the drawing here should be tile specific ... 
 *      
 *      [3] Zooming use existing data ... 
 *      - Zoom-in use current tiles ...
 *      - Zoom-out use a combination of fit-to-width image and current tiles
 *      - Use the canvas' scale transformation
 *      
 *      [4] Predictive fetching (the 16 tiles)
 *      - prefetch the extra 7
 *          - extra 3 in 1st direction of travel
 *          - extra 4 in 2nd direction of travel
 *          - post requests to the end of queue (rather than the front)
 *      - cancelling prefetching if direction changes
 *      
 *      [5] Bitmaps as drawables [precise rendering]
 *      
 *      [6] Blend images and drawings ...
 *      - Two kinds of drawings: part of scene (sprite?) and part of screen.
 */

public class ReaderPerspectiveTiler implements TilingDelegate,
        TileDrawableCallback
{

    public interface OnTileReadyListener
    {
        public void onTileReady(ReaderTile tile);
    }

    public interface TileDrawableSource
    {
        public void requestDrawableForTile(ReaderTile tile,
                TileDrawableCallback callback);
        public void requestPrefetchDrawableForTile(ReaderTile tile,
                TileDrawableCallback callback);
        public void cancelPendingRequests();
    }

    public static final float TILING_PADDING_X = 50f;
    public static final float TILING_PADDING_Y = 50f;
    public static final int   MIN_TILE_COLUMNS = 2;
    public static final int   MIN_TILE_ROWS    = 2;

    public ReaderPerspectiveTiler(TileDrawableSource tds,
            OnTileReadyListener listener)
    {
        mTileDrawableSource = tds;
        mOnTileReadyListener = listener;
        mCurrentTiles = new ArrayList<ReaderTile>();
        mNewTiles = new ArrayList<ReaderTile>();
        mPrefetchedTiles = new ArrayList<ReaderTile>();
        mNewPrefetchedTiles = new ArrayList<ReaderTile>();
    }

    @Override
    // implements TilingDelegate
    public ArrayList<ReaderTile> getCurrentTiles()
    {
        return mCurrentTiles;
    }

    @Override
    // implements TilingDelegate
    public void updateCurrentTiles(RectF worldRect, RectF worldWindow,
            float scale, int columnDirection, int rowDirection)
    {
        int oldColumnStart = mColumnStart;
        int oldRowStart = mRowStart;
        int oldColumnCount = mColumnCount;
        int oldRowCount = mRowCount;

        mColumnStart = (int) ((worldWindow.left - worldRect.left) / mTileWorldWidth); // floor
        mRowStart = (int) ((worldWindow.top - worldRect.top) / mTileWorldHeight); // floor

        mColumnCount = Math.round((worldWindow.right - worldRect.left)
                / mTileWorldWidth - mColumnStart + 0.5f); // ceiling
        mRowCount = Math.round((worldWindow.bottom - worldRect.top)
                / mTileWorldHeight - mRowStart + 0.5f); // ceiling

        if (mCouldReuse && mColumnStart == oldColumnStart
                && mRowStart == oldRowStart && mColumnCount == oldColumnCount
                && mRowCount == oldRowCount)
            return;

        mTotalColumns = Math.round((worldRect.right - worldRect.left)
                / mTileWorldWidth + 0.5f); // ceiling
        mTotalRows = Math.round((worldRect.bottom - worldRect.top)
                / mTileWorldHeight + 0.5f); // ceiling

        for (ReaderTile tile : mCurrentTiles)
            if (ReaderTile.inRange(tile.columnIndex, tile.rowIndex,
                    mColumnStart, mRowStart, mColumnCount, mRowCount))
                mNewTiles.add(tile);
            else
                tile.setFree();

        int columnStartWithPrefetch = mColumnStart;
        int rowStartWithPrefetch = mRowStart;
        int columnCountWithPrefetch = mColumnCount;
        int rowCountWithPrefetch = mRowCount;

        if (mColumnCount <= MIN_TILE_COLUMNS) {
            if (mColumnStart > 0) {
                columnStartWithPrefetch--;
                columnCountWithPrefetch++;
            }
            if (columnCountWithPrefetch < mTotalColumns)
                columnCountWithPrefetch++;
        } else if (-1 == columnDirection && mColumnStart > 0) {
            columnStartWithPrefetch--;
            columnCountWithPrefetch++;
        } else if (1 == columnDirection
                && columnCountWithPrefetch < mTotalColumns)
            columnCountWithPrefetch++;

        if (mRowCount <= MIN_TILE_ROWS) {
            if (mRowStart > 0) {
                rowStartWithPrefetch--;
                rowCountWithPrefetch++;
            }
            if (rowCountWithPrefetch < mTotalRows)
                rowCountWithPrefetch++;
        } else if (-1 == rowDirection && mRowStart > 0) {
            rowStartWithPrefetch--;
            rowCountWithPrefetch++;
        } else if (1 == rowDirection && rowCountWithPrefetch < mTotalRows)
            rowCountWithPrefetch++;

        float tileStartX = worldRect.left + columnStartWithPrefetch
                * mTileWorldWidth;
        float tileStartY = worldRect.top + rowStartWithPrefetch
                * mTileWorldHeight;

        float currentTileLeft = tileStartX, currentTileTop = tileStartY;
        for (int i = 0; i < rowCountWithPrefetch; i++, currentTileLeft = tileStartX, currentTileTop += mTileWorldHeight)
            for (int j = 0; j < columnCountWithPrefetch; j++, currentTileLeft += mTileWorldWidth) {
                int tileColumn = columnStartWithPrefetch + j;
                int tileRow = rowStartWithPrefetch + i;
                ReaderTile tile;

                boolean currentlyVisible = ReaderTile.inRange(tileColumn, tileRow,
                        mColumnStart, mRowStart, mColumnCount, mRowCount);

                if (currentlyVisible
                        && mCouldReuse
                        && ReaderTile.inRange(tileColumn, tileRow,
                                oldColumnStart, oldRowStart, oldColumnCount,
                                oldRowCount))
                    continue;

                tile = retrieveFromPrefetched(scale, tileColumn, tileRow);

                if (null == tile) {
                    RectF rect = new RectF(currentTileLeft, currentTileTop,
                            currentTileLeft + mTileWorldWidth, currentTileTop
                                    + mTileWorldHeight);

                    tile = new ReaderTile(scale, rect, tileColumn, tileRow);
                    // These two are for debugging, otherwise unnecessary
                    tile.totalColumns = mTotalColumns;
                    tile.totalRows = mTotalRows;
                }

                if (currentlyVisible)
                    mNewTiles.add(tile);
                else
                    mNewPrefetchedTiles.add(tile);
            }

        // "double buffer"
        mCurrentTiles.clear();
        ArrayList<ReaderTile> tmp = mCurrentTiles;
        mCurrentTiles = mNewTiles;
        mNewTiles = tmp;
        mPrefetchedTiles.clear();
        tmp = mPrefetchedTiles;
        mPrefetchedTiles = mNewPrefetchedTiles;
        mNewPrefetchedTiles = tmp;
        mCouldReuse = true;

        for (ReaderTile tile : mCurrentTiles)
            if (tile.isNew())
                mTileDrawableSource.requestDrawableForTile(tile, this);

        for (ReaderTile tile : mPrefetchedTiles)
            if (tile.isNew())
                mTileDrawableSource.requestPrefetchDrawableForTile(tile, this);
    }

    @Override
    // implements TilingDelegate
    public void retile(RectF worldRect, RectF worldWindow, RectF viewport,
            float worldToViewScale)
    {
        mTileDrawableSource.cancelPendingRequests();
        mCurrentTiles.clear();
        mPrefetchedTiles.clear();
        mCouldReuse = false;
        updateTilingParams(viewport, worldToViewScale);
        updateCurrentTiles(worldRect, worldWindow, worldToViewScale, 0, 0);
    }

    @Override
    // TileDrawableCallback implementatino
    public void onTileDrawableReady(ReaderTile tile)
    {
        if (mCurrentTiles.contains(tile))
            mOnTileReadyListener.onTileReady(tile);
    }

    @Override
    // TileDrawableCallback implementatino
    public void onPrefetchedTileDrawableReady(ReaderTile tile) {
        // do nothing
    }

    private void updateTilingParams(RectF viewport, float worldToViewScale)
    {
        // TODO: snap to pixel grids
        mTileViewportWidth = (viewport.width() + TILING_PADDING_X)
                / MIN_TILE_COLUMNS;
        mTileViewportHeight = (viewport.height() + TILING_PADDING_Y)
                / MIN_TILE_ROWS;
        mTileWorldWidth = mTileViewportWidth / worldToViewScale;
        mTileWorldHeight = mTileViewportHeight / worldToViewScale;
    }

    private ReaderTile retrieveFromPrefetched(float scale, int column, int row)
    {
        ReaderTile foundTile = null;
        for (ReaderTile tile : mPrefetchedTiles)
            if (tile.worldToViewScale == scale && tile.columnIndex == column
                    && tile.rowIndex == row) {
                foundTile = tile;
                break;
            }
        if (null != foundTile)
            mPrefetchedTiles.remove(foundTile);

        return foundTile;
    }

    private OnTileReadyListener   mOnTileReadyListener;
    private TileDrawableSource    mTileDrawableSource;
    private boolean               mCouldReuse;
    private ArrayList<ReaderTile> mCurrentTiles;
    private ArrayList<ReaderTile> mNewTiles;
    private ArrayList<ReaderTile> mPrefetchedTiles;
    private ArrayList<ReaderTile> mNewPrefetchedTiles;
    private float                 mTileViewportWidth, mTileViewportHeight;
    private float                 mTileWorldWidth, mTileWorldHeight;
    private int                   mColumnStart, mRowStart;
    private int                   mColumnCount, mRowCount;
    private int                   mTotalColumns, mTotalRows;
}
