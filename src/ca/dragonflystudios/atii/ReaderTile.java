package ca.dragonflystudios.atii;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderWorld.ReaderWorldDrawable;

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

    public ReaderTile(float scale, RectF rect, int c, int r) {
        mState = State.NEW;

        worldToViewScale = scale;
        tileRect = rect;
        columnIndex = c;
        rowIndex = r;

        // TODO: deal with snapping to grid
        mViewportWidth = (int) (scale * rect.width());
        mViewportHeight = (int) (scale * rect.height());
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
    }

    public static boolean inRange(int x, int y, int columnStart, int rowStart, int columnCount, int rowCount) {
        return (x >= columnStart && x < columnStart + columnCount && y >= rowStart && y < rowStart + rowCount);
    }

    public void render() {
        mBitmap = Bitmap.createBitmap(mViewportWidth, mViewportHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        canvas.scale(worldToViewScale, worldToViewScale);
        canvas.translate(-tileRect.left, -tileRect.top);

        Paint paint = new Paint();

        paint.setStrokeWidth(ReaderWorld.GRID_LINE_WIDTH);
        drawTile(canvas, paint, State.READY);
    }

    @Override
    // ReaderWorldDrawable implementation
    public void draw(Canvas canvas, Rect viewRect, Paint paint) {
        if (isReady()) {
//            canvas.save();
//            canvas.setMatrix(new Matrix());
            canvas.drawBitmap(mBitmap, null, viewRect, paint);
//            canvas.restore();
        } else if (isPending()) {
            final float strokeWidth = paint.getStrokeWidth();
            paint.setStrokeWidth(ReaderWorld.GRID_LINE_WIDTH);
            drawTile(canvas, paint, State.PENDING);
            paint.setStrokeWidth(strokeWidth);
        } else
            throw new RuntimeException("attempting to draw tile in bad state");
    }

    private void drawTile(Canvas canvas, Paint paint, State state) {
        int shade = 0;
        float top = (tileRect.top < ReaderWorld.CONTENT_TOP) ? ReaderWorld.CONTENT_TOP : tileRect.top;
        float bottom = (tileRect.bottom > ReaderWorld.CONTENT_BOTTOM) ? ReaderWorld.CONTENT_BOTTOM : tileRect.bottom;

        if (top <= bottom && top <= ReaderWorld.CONTENT_BOTTOM && bottom >= ReaderWorld.CONTENT_TOP)
            for (float lineX = ReaderWorld.CONTENT_LEFT; lineX <= ReaderWorld.CONTENT_RIGHT; lineX += ReaderWorld.X_INC, shade++) {
                if (lineX < this.tileRect.left)
                    continue;
                if (lineX > this.tileRect.right)
                    break;

                int s = (int) (shade * ReaderWorld.COLOR_INC);
                if (State.PENDING == state)
                    paint.setColor(Color.argb(255, s, s, s));
                else if (State.READY == state)
                    paint.setColor(Color.argb(255, 0, 0, s));
                canvas.drawLine(lineX, top, lineX, bottom, paint);
            }

        shade = 0;
        float left = (tileRect.left < ReaderWorld.CONTENT_LEFT) ? ReaderWorld.CONTENT_LEFT : tileRect.left;
        float right = (tileRect.right > ReaderWorld.CONTENT_RIGHT) ? ReaderWorld.CONTENT_RIGHT : tileRect.right;

        if (left <= right && left <= ReaderWorld.CONTENT_RIGHT && right >= ReaderWorld.CONTENT_LEFT)
            for (float lineY = ReaderWorld.CONTENT_TOP; lineY <= ReaderWorld.CONTENT_BOTTOM; lineY += ReaderWorld.Y_INC, shade++) {
                if (lineY < this.tileRect.top)
                    continue;
                if (lineY > this.tileRect.bottom)
                    break;

                int s = (int) (shade * ReaderWorld.COLOR_INC);
                if (State.PENDING == state)
                    paint.setColor(Color.argb(255, s, s, s));
                else if (State.READY == state)
                    paint.setColor(Color.argb(255, 0, s, 0));
                canvas.drawLine(left, lineY, right, lineY, paint);
            }
    }

    private State mState;
    private int mViewportWidth, mViewportHeight;
    private Bitmap mBitmap;
}
