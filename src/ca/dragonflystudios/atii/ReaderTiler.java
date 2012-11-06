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

        public Tile(RectF rect, int c, int r) {
            tileRect = rect;
            columnIndex = c;
            rowIndex = r;
        }
    }

    public ReaderTiler() {
        mCurrentTiles = new ArrayList<Tile>();
    }

    @Override
    // implements TilingDelegate
    public ArrayList<Tile> getCurrentTiles() {
        return mCurrentTiles;
    }

    @Override
    // implements TilingDelegate
    public void updateCurrentTiles(RectF worldRect, RectF worldWindow) {
        mCurrentTiles.clear();

        mColumnStart = (int) ((worldWindow.left - worldRect.left) / mTileWorldWidth); // floor
        mRowStart = (int) ((worldWindow.top - worldRect.top) / mTileWorldHeight); // floor
        mTileStartX = worldRect.left + mColumnStart * mTileWorldWidth;
        mTileStartY = worldRect.top + mRowStart * mTileWorldHeight;
        mColumnCount = Math.round((worldWindow.right - mTileStartX) / mTileWorldWidth + 0.5f); // ceiling
        mRowCount = Math.round((worldWindow.bottom - mTileStartY) / mTileWorldHeight + 0.5f); // ceiling
        float currentTileLeft = mTileStartX, currentTileTop = mTileStartY;
        for (int i = 0; i < mRowCount; i++, currentTileLeft = mTileStartX, currentTileTop += mTileWorldHeight)
            for (int j = 0; j < mColumnCount; j++, currentTileLeft += mTileWorldWidth) {
                RectF rect = new RectF(currentTileLeft, currentTileTop, currentTileLeft + mTileWorldWidth, currentTileTop
                        + mTileWorldHeight);
                mCurrentTiles.add(new Tile(rect, j, i));
            }
    }

    @Override
    // implements TilingDelegate
    public void retile(RectF worldRect, RectF worldWindow, RectF viewport, float worldToViewScale) {
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
    private float mTileViewportWidth, mTileViewportHeight;
    private float mTileWorldWidth, mTileWorldHeight;
    private int mColumnStart, mRowStart;
    private float mTileStartX, mTileStartY;
    private int mColumnCount, mRowCount;
}
