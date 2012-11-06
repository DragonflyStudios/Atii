package ca.dragonflystudios.atii;

import java.util.ArrayList;

import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderWorldPerspective.TilingDelegate;

public class ReaderTiler implements TilingDelegate {

    public static final float TILING_PADDING_X = 50f;
    public static final float TILING_PADDING_Y = 50f;
    public static final int MIN_TILE_COLUMNS = 2;
    public static final int MIN_TILE_ROWS = 2;

    public static class Tile {
        public RectF tileRect;
        public int columnIndex;
        public int rowIndex;

        // These two are for debugging, otherwise unnecessary
        public int totalColumns;
        public int totalRows;

        public Tile(RectF rect, int c, int r) {
            tileRect = rect;
            columnIndex = c;
            rowIndex = r;
        }

        public static boolean inRange(int x, int y, int columnStart, int rowStart, int columnCount, int rowCount) {
            return (x >= columnStart && x < columnStart + columnCount && y >= rowStart && y < rowStart + rowCount);
        }
    }

    public ReaderTiler() {
        mCurrentTiles = new ArrayList<Tile>();
        mNewTiles = new ArrayList<Tile>();
    }

    @Override
    // implements TilingDelegate
    public ArrayList<Tile> getCurrentTiles() {
        return mCurrentTiles;
    }

    @Override
    // implements TilingDelegate
    public void updateCurrentTiles(RectF worldRect, RectF worldWindow) {
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

        for (Tile tile : mCurrentTiles)
            if (Tile.inRange(tile.columnIndex, tile.rowIndex, mColumnStart, mRowStart, mColumnCount, mRowCount))
                mNewTiles.add(tile);

        float currentTileLeft = mTileStartX, currentTileTop = mTileStartY;
        for (int i = 0; i < mRowCount; i++, currentTileLeft = mTileStartX, currentTileTop += mTileWorldHeight)
            for (int j = 0; j < mColumnCount; j++, currentTileLeft += mTileWorldWidth) {
                if (!mCurrentTiles.isEmpty()
                        && Tile.inRange(mColumnStart + j, mRowStart + i, oldColumnStart, oldRowStart, oldColumnCount, oldRowCount))
                    continue;

                RectF rect = new RectF(currentTileLeft, currentTileTop, currentTileLeft + mTileWorldWidth, currentTileTop
                        + mTileWorldHeight);

                Tile tile = new Tile(rect, mColumnStart + j, mRowStart + i);
                // These two are for debugging, otherwise unnecessary
                tile.totalColumns = totalColumns;
                tile.totalRows = totalRows;
                mNewTiles.add(tile);
            }

        // "double buffer"
        mCurrentTiles.clear();
        ArrayList<Tile> temp = mCurrentTiles;
        mCurrentTiles = mNewTiles;
        mNewTiles = temp;
    }

    @Override
    // implements TilingDelegate
    public void retile(RectF worldRect, RectF worldWindow, RectF viewport, float worldToViewScale) {
        mCurrentTiles.clear();
        updateTilingParams(viewport, worldToViewScale);
        updateCurrentTiles(worldRect, worldWindow);
    }

    private void updateTilingParams(RectF viewport, float worldToViewScale) {
        mTileViewportWidth = (viewport.width() + TILING_PADDING_X) / MIN_TILE_COLUMNS;
        mTileViewportHeight = (viewport.height() + TILING_PADDING_Y) / MIN_TILE_ROWS;
        mTileWorldWidth = mTileViewportWidth / worldToViewScale;
        mTileWorldHeight = mTileViewportHeight / worldToViewScale;
    }

    private ArrayList<Tile> mCurrentTiles;
    private ArrayList<Tile> mNewTiles;
    private float mTileViewportWidth, mTileViewportHeight;
    private float mTileWorldWidth, mTileWorldHeight;
    private int mColumnStart, mRowStart;
    private float mTileStartX, mTileStartY;
    private int mColumnCount, mRowCount;
}
