package ca.dragonflystudios.atii.control.tiling;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import ca.dragonflystudios.atii.control.ReaderPerspective.WorldDimensionDelegate;
import ca.dragonflystudios.atii.model.world.World.ReaderWorldDrawable;

public class ReaderTile implements ReaderWorldDrawable {
    public enum State {
        NEW, PENDING, READY, FREE
    };

    public float worldToViewScale;
    public RectF tileRect;
    public int columnIndex;
    public int rowIndex;

    // These two are for debugging, otherwise unnecessary
    public int totalColumns;
    public int totalRows;

    public ReaderTile(WorldDimensionDelegate wdd, float scale, RectF rect, int c, int r) {
        mWorldDimensionDelegate = wdd;
        mIncX = wdd.getContentWidth() / GRID_COUNT;
        mIncY = wdd.getContentHeight() / GRID_COUNT;

        mGridLineWidth = mIncX / 5f; // Grid line width is 20% of grid size

        mState = State.NEW;

        worldToViewScale = scale;
        tileRect = rect;
        columnIndex = c;
        rowIndex = r;

        // TODO: deal with snapping to grid
        mViewportWidth = (int) (scale * rect.width());
        mViewportHeight = (int) (scale * rect.height());
    }

    public float getViewportWidth() {
        return mViewportWidth;
    }

    public float getViewportHeight() {
        return mViewportHeight;
    }

    public void setBitmap(Bitmap bitmap) {
        if (null != mBitmap)
            mBitmap.recycle();
        mBitmap = bitmap;
    }

    public boolean isPending() {
        return (mState == State.PENDING);
    }

    public void setPending() {
        mState = State.PENDING;
    }

    public boolean isReady() {
        return (mState == State.READY);
    }

    public void setReady() {
        mState = State.READY;
    }

    public boolean isNew() {
        return (mState == State.NEW);
    }

    public void setFree() {
        mState = State.FREE;
        if (null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public static boolean inRange(int x, int y, int columnStart, int rowStart, int columnCount, int rowCount) {
        return (x >= columnStart && x < columnStart + columnCount && y >= rowStart && y < rowStart + rowCount);
    }

    public void render(Bitmap contentBitmap, Rect contentBitmapRect) {
        mBitmap = Bitmap.createBitmap(mViewportWidth, mViewportHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        canvas.scale(worldToViewScale, worldToViewScale);
        canvas.translate(-tileRect.left, -tileRect.top);

        canvas.drawBitmap(contentBitmap, null, contentBitmapRect, null);
        /*
         * Paint paint = new Paint(); paint.setStrokeWidth(mGridLineWidth);
         * drawTile(canvas, paint, State.READY);
         */
    }

    @Override
    // ReaderWorldDrawable implementation
    public void draw(Canvas canvas, Rect viewRect, Paint paint) {
        if (isReady())
            canvas.drawBitmap(mBitmap, null, viewRect, paint);
        else if (isPending()) {
            final float strokeWidth = paint.getStrokeWidth();
            paint.setStrokeWidth(mGridLineWidth);
            drawTile(canvas, paint, State.PENDING);
            paint.setStrokeWidth(strokeWidth);
        } else
            throw new RuntimeException("attempting to draw tile in bad state");
    }

    private void drawTile(Canvas canvas, Paint paint, State state) {
        int shade = 0;
        float top = (tileRect.top < mWorldDimensionDelegate.getContentTop()) ? mWorldDimensionDelegate.getContentTop()
                : tileRect.top;
        float bottom = (tileRect.bottom > mWorldDimensionDelegate.getContentBottom()) ? mWorldDimensionDelegate.getContentBottom()
                : tileRect.bottom;

        if (top <= bottom && top <= mWorldDimensionDelegate.getContentBottom() && bottom >= mWorldDimensionDelegate.getContentTop())
            for (float lineX = mWorldDimensionDelegate.getContentLeft(); lineX <= mWorldDimensionDelegate.getContentRight(); lineX += mIncX, shade++) {
                if (lineX < this.tileRect.left)
                    continue;
                if (lineX > this.tileRect.right)
                    break;

                int s = (int) (shade * COLOR_INC);
                if (State.PENDING == state)
                    paint.setColor(Color.argb(255, s, s, s));
                else if (State.READY == state)
                    paint.setColor(Color.argb(255, 0, 0, s));
                canvas.drawLine(lineX, top, lineX, bottom, paint);
            }

        shade = 0;
        float left = (tileRect.left < mWorldDimensionDelegate.getContentLeft()) ? mWorldDimensionDelegate.getContentLeft()
                : tileRect.left;
        float right = (tileRect.right > mWorldDimensionDelegate.getContentRight()) ? mWorldDimensionDelegate.getContentRight()
                : tileRect.right;

        if (left <= right && left <= mWorldDimensionDelegate.getContentRight() && right >= mWorldDimensionDelegate.getContentLeft())
            for (float lineY = mWorldDimensionDelegate.getContentTop(); lineY <= mWorldDimensionDelegate.getContentBottom(); lineY += mIncY, shade++) {
                if (lineY < this.tileRect.top)
                    continue;
                if (lineY > this.tileRect.bottom)
                    break;

                int s = (int) (shade * COLOR_INC);
                if (State.PENDING == state)
                    paint.setColor(Color.argb(255, s, s, s));
                else if (State.READY == state)
                    paint.setColor(Color.argb(255, 0, s, 0));
                canvas.drawLine(left, lineY, right, lineY, paint);
            }
    }

    // the "world" is made of GRID_COUNT horizontal and GRID_COUNT vertical grid
    // lines of shades of, respectively, blue and green
    private static final float GRID_COUNT = 64f;
    private static final float COLOR_INC = 255f / GRID_COUNT;

    private float mIncX;
    private float mIncY;
    private float mGridLineWidth;

    private WorldDimensionDelegate mWorldDimensionDelegate;
    private State mState;
    private int mViewportWidth, mViewportHeight;
    private Bitmap mBitmap;
}
