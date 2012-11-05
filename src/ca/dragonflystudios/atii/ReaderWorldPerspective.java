package ca.dragonflystudios.atii;

import java.util.ArrayList;

import android.graphics.RectF;

// TODO: part of the data model; should be persisted

/*
 * (1) Shape of Viewport determines shape of WorldWindow, which, in the simple case, is the X/Y ratio
 * (2) Size of WorldWindow determines how much content is shown
 * (3) Origin of WorldWindow determines which content is shown
 * (4) Size of Viewport in terms of pixels is size of bitmap
 *     (4.1) Viewport shows and only shows everything in WorldWindow at a resolution (# of X & Y pixels) determined by size of Viewport
 * (5) This means elimination of "WorldToViewScale"
 * (6) This also means there needs to be an initial "handshaking" through which initial WorldWindow size is determined:
 *     (6.1) Call "Perspective" the combination of Viewport size and WorldWindow
 *     (6.2) Perspective is persisted
 *     (6.3) If Perspective is never initialized (persisted), then "fit-to-width" to the given Viewport size
 *     (6.4) If Perspective is persisted, then update Viewport to the newly given one
 */

public class ReaderWorldPerspective {

    public static final float MIN_SCALE_FACTOR = 10f;
    public static final float MAX_SCALE_FACTOR = 500f;

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

    public interface WorldWindowDelegate {
        public float getLimitMinX(RectF worldWindow);

        public float getLimitMinY(RectF worldWindow);

        public float getLimitMaxX(RectF worldWindow);

        public float getLimitMaxY(RectF worldWindow);

        public RectF getWorldRect();
    }

    public ReaderWorldPerspective(WorldWindowDelegate wdd) {
        mInitialized = false;
        mWorldWindowDelegate = wdd;
        mCurrentWorldTiles = new ArrayList<Tile>();
    }

    public boolean updateViewport(float left, float top, float right, float bottom) {
        if (!mInitialized) {
            mViewport = new RectF(left, top, right, bottom);

            // fit width
            mWorldWindow = new RectF(mWorldWindowDelegate.getWorldRect());
            mWorldToViewScale = (right - left + 1) / (mWorldWindow.right - mWorldWindow.left);
            mWorldToViewScale = Math.max(MIN_SCALE_FACTOR, Math.min(mWorldToViewScale, MAX_SCALE_FACTOR));
            mInitialized = true;
        } else {
            if (left == mViewport.left && top == mViewport.top && right == mViewport.right && bottom == mViewport.bottom)
                return false;

            mViewport.left = left;
            mViewport.top = top;
            mViewport.right = right;
            mViewport.bottom = bottom;
        }

        mWorldWindow.right = mWorldWindow.left + (right - left + 1) / mWorldToViewScale;
        mWorldWindow.bottom = mWorldWindow.top + (bottom - top + 1) / mWorldToViewScale;
        retile();

        return true;
    }

    public boolean panWorldWindow(float deltaX, float deltaY) {
        if (!mInitialized)
            throw new RuntimeException("ReaderWorldPerspective is used before initialization");

        float oldLeft = mWorldWindow.left, oldTop = mWorldWindow.top;
        mWorldWindow.offset(deltaX / mWorldToViewScale, deltaY / mWorldToViewScale);
        mWorldWindow.offsetTo(
                Math.max(mWorldWindowDelegate.getLimitMinX(mWorldWindow),
                        Math.min(mWorldWindow.left, mWorldWindowDelegate.getLimitMaxX(mWorldWindow))),
                Math.max(mWorldWindowDelegate.getLimitMinY(mWorldWindow),
                        Math.min(mWorldWindow.top, mWorldWindowDelegate.getLimitMaxY(mWorldWindow))));
        boolean panned = (oldLeft != mWorldWindow.left || oldTop != mWorldWindow.top);
        if (panned)
            updateCurrentTiles();

        return panned;
    }

    public boolean scaleWorldWindow(float scaleBy, float focusX, float focusY) {
        if (!mInitialized)
            throw new RuntimeException("ReaderWorldPerspective is used before initialization");

        float oldLeft = mWorldWindow.left, oldTop = mWorldWindow.top, oldRight = mWorldWindow.right, oldBottom = mWorldWindow.bottom;

        final float WorldWindowFocusX = oldLeft + (focusX - mViewport.left) / mWorldToViewScale;
        final float WorldWindowFocusY = oldTop + (focusY - mViewport.top) / mWorldToViewScale;

        float previousScaleFactor = mWorldToViewScale;
        mWorldToViewScale *= scaleBy;
        mWorldToViewScale = Math.max(MIN_SCALE_FACTOR, Math.min(mWorldToViewScale, MAX_SCALE_FACTOR));

        // "zooming in", i.e. scaleBy > 1.0, means a smaller world WorldWindow
        final float scaling = previousScaleFactor / mWorldToViewScale;
        mWorldWindow.offset(-WorldWindowFocusX, -WorldWindowFocusY);
        mWorldWindow.set(mWorldWindow.left * scaling, mWorldWindow.top * scaling, mWorldWindow.right * scaling, mWorldWindow.bottom
                * scaling);
        mWorldWindow.offset(WorldWindowFocusX, WorldWindowFocusY);
        mWorldWindow.offsetTo(
                Math.max(mWorldWindowDelegate.getLimitMinX(mWorldWindow),
                        Math.min(mWorldWindow.left, mWorldWindowDelegate.getLimitMaxX(mWorldWindow))),
                Math.max(mWorldWindowDelegate.getLimitMinY(mWorldWindow),
                        Math.min(mWorldWindow.top, mWorldWindowDelegate.getLimitMaxY(mWorldWindow))));

        boolean scaled = (oldLeft != mWorldWindow.left || oldTop != mWorldWindow.top || oldRight != mWorldWindow.right || oldBottom != mWorldWindow.bottom);

        if (scaled)
            retile();

        return scaled;
    }

    public float getWorldToViewScale() {
        return mWorldToViewScale;
    }

    public RectF getWorldWindow() {
        return mWorldWindow;
    }

    public RectF getViewport() {
        return mViewport;
    }

    public ArrayList<Tile> getCurrentWorldTiles() {
        return mCurrentWorldTiles;
    }

    private void updateTilingParams() {
        mTileViewportWidth = (mViewport.width() + TILING_PADDING_X) / MIN_TILE_COLUMNS;
        mTileViewportHeight = (mViewport.height() + TILING_PADDING_Y) / MIN_TILE_ROWS;
        mTileWorldWidth = mTileViewportWidth / mWorldToViewScale;
        mTileWorldHeight = mTileViewportHeight / mWorldToViewScale;
    }

    private void updateCurrentTiles() {
        mCurrentWorldTiles.clear();
        RectF worldRect = mWorldWindowDelegate.getWorldRect();
        
        mColumnStart = (int) ((mWorldWindow.left - worldRect.left)/ mTileWorldWidth); // floor
        mRowStart = (int) ((mWorldWindow.top - worldRect.top) / mTileWorldHeight); // floor
        mTileStartX = worldRect.left + mColumnStart * mTileWorldWidth;
        mTileStartY = worldRect.top + mRowStart * mTileWorldHeight;
        mColumnCount = Math.round((mWorldWindow.right - mTileStartX) / mTileWorldWidth + 0.5f); // ceiling
        mRowCount = Math.round((mWorldWindow.bottom - mTileStartY) / mTileWorldHeight + 0.5f); // ceiling
        float currentTileLeft = mTileStartX, currentTileTop = mTileStartY;
        for (int i = 0; i < mRowCount; i++, currentTileLeft = mTileStartX, currentTileTop += mTileWorldHeight)
            for (int j = 0; j < mColumnCount; j++, currentTileLeft += mTileWorldWidth) {
                RectF rect = new RectF(currentTileLeft, currentTileTop, currentTileLeft + mTileWorldWidth, currentTileTop
                        + mTileWorldHeight);
                mCurrentWorldTiles.add(new Tile(rect, j, i));
            }
    }

    private void retile() {
        updateTilingParams();
        updateCurrentTiles();
    }

    private WorldWindowDelegate mWorldWindowDelegate;
    private RectF mViewport;
    private RectF mWorldWindow;
    private float mWorldToViewScale;
    private boolean mInitialized;

    private ArrayList<Tile> mCurrentWorldTiles;
    private float mTileViewportWidth, mTileViewportHeight;
    private float mTileWorldWidth, mTileWorldHeight;
    private int mColumnStart, mRowStart;
    private float mTileStartX, mTileStartY;
    private int mColumnCount, mRowCount;
}
