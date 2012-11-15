package ca.dragonflystudios.atii.model.world;

import java.io.File;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import ca.dragonflystudios.atii.control.ReaderPerspective.WorldDimensionDelegate;
import ca.dragonflystudios.atii.control.tiling.ReaderPerspectiveTiler.TileDrawableSource;
import ca.dragonflystudios.atii.control.tiling.ReaderTile;
import ca.dragonflystudios.atii.model.story.Clip;
import ca.dragonflystudios.atii.model.story.Story;

public class World implements WorldDimensionDelegate, TileDrawableSource {
    // TODO:
    // - for now just confuse story and world ...
    // - let story.xml specify dimension of world ...
    // -
    public World(Story story) {
        mapStory(story);

        mDrawingHandlerThread = new HandlerThread("Async Tile Drawing Thread");
        mDrawingHandlerThread.start();
        mDrawingHandler = new Handler(mDrawingHandlerThread.getLooper());
    }

    private void mapStory(Story story) {
        if (story.getClips().size() != 1)
            throw new RuntimeException("wrong number of clips");

        Clip clip = story.getClips().get(0);

        File photosFolder = new File(story.getStoryFolder(), "photos");
        File photoFile = new File(photosFolder, clip.getLook().getPictureFileName());
        try {
            mWorldBitmapDecoder = BitmapRegionDecoder.newInstance(photoFile.getAbsolutePath(), false);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("problem opening world image file");
        }

        mContentLeft = 0;
        mContentTop = 0;
        mContentRight = mWorldBitmapDecoder.getWidth();
        mContentBottom = mWorldBitmapDecoder.getHeight();
        mContentMargin = mContentRight / 20f; // 5% margin
    }

    // WorldWindowDelegate implementation
    @Override
    public float getContentWidth() {
        return mContentRight - mContentLeft;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getContentHeight() {
        return mContentBottom - mContentTop;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getContentLeft() {
        return mContentLeft;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getContentTop() {
        return mContentTop;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getContentRight() {
        return mContentLeft;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getContentBottom() {
        return mContentLeft;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinX(RectF worldWindow) {
        return mContentLeft - mContentMargin;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinY(RectF worldWindow) {
        return mContentTop - mContentMargin;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMaxX(RectF worldWindow) {
        return mContentRight - (worldWindow.right - worldWindow.left) + mContentMargin;
    }

    @Override
    // WorldWindowDelegate implementation
    public float getLimitMaxY(RectF worldWindow) {
        return mContentBottom - (worldWindow.bottom - worldWindow.top) + mContentMargin;
    }

    @Override
    // WorldWindowDelegate implementation
    public RectF getWorldRect() {
        return new RectF(mContentLeft - mContentMargin, mContentTop - mContentMargin, mContentRight + mContentMargin,
                mContentBottom + mContentMargin);
    }

    private Handler mDrawingHandler;
    private HandlerThread mDrawingHandlerThread;

    public interface ReaderWorldDrawable {
        public void draw(Canvas canvas, Rect viewRect, Paint paint);
    }

    public interface TileDrawableCallback {
        public void onTileDrawableReady(ReaderTile tile);

        public void onPrefetchedTileDrawableReady(ReaderTile tile);
    }

    @Override
    // TileDrawableSource implementation
    public void requestDrawableForTile(final ReaderTile tile, final TileDrawableCallback callback) {
        request(tile, callback, true);
    }

    @Override
    // TileDrawableSource implementation
    public void requestPrefetchDrawableForTile(final ReaderTile tile, final TileDrawableCallback callback) {
        request(tile, callback, false);
    }

    private void request(final ReaderTile tile, final TileDrawableCallback callback, final boolean front) {
        if (tile.isReady())
            callback.onTileDrawableReady(tile);
        else {
            tile.setPending();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    RectF contentRectF = new RectF(mContentLeft, mContentTop, mContentRight, mContentBottom);
                    contentRectF.intersect(tile.tileRect);
                    Rect contentRect = new Rect(Math.round(contentRectF.left + .5f),
                            Math.round(contentRectF.top + .5f), (int)(contentRectF.right), (int)(contentRectF.bottom));
                    Bitmap contentBitmap = mWorldBitmapDecoder.decodeRegion(contentRect, null);
                    tile.render(contentBitmap, contentRect);
                    tile.setReady();
                    callback.onTileDrawableReady(tile);
                }
            };

            if (front)
                mDrawingHandler.postAtFrontOfQueue(r);
            else
                mDrawingHandler.post(r);
        }
    }

    @Override
    // TileDrawableSource implementation
    public void cancelPendingRequests() {
        mDrawingHandler.removeCallbacks(null, null);
    }

    private float mContentLeft;
    private float mContentRight;
    private float mContentTop;
    private float mContentBottom;
    private float mContentMargin;

    private BitmapRegionDecoder mWorldBitmapDecoder;
}
