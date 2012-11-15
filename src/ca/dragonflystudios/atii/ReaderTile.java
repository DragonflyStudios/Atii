package ca.dragonflystudios.atii;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import ca.dragonflystudios.atii.world.World;
import ca.dragonflystudios.atii.world.World.ReaderWorldDrawable;

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

        paint.setStrokeWidth(World.GRID_LINE_WIDTH);
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
            paint.setStrokeWidth(World.GRID_LINE_WIDTH);
            drawTile(canvas, paint, State.PENDING);
            paint.setStrokeWidth(strokeWidth);
        } else
            throw new RuntimeException("attempting to draw tile in bad state");
    }

    private void drawTile(Canvas canvas, Paint paint, State state) {
        int shade = 0;
        float top = (tileRect.top < World.CONTENT_TOP) ? World.CONTENT_TOP : tileRect.top;
        float bottom = (tileRect.bottom > World.CONTENT_BOTTOM) ? World.CONTENT_BOTTOM : tileRect.bottom;

        if (top <= bottom && top <= World.CONTENT_BOTTOM && bottom >= World.CONTENT_TOP)
            for (float lineX = World.CONTENT_LEFT; lineX <= World.CONTENT_RIGHT; lineX += World.X_INC, shade++) {
                if (lineX < this.tileRect.left)
                    continue;
                if (lineX > this.tileRect.right)
                    break;

                int s = (int) (shade * World.COLOR_INC);
                if (State.PENDING == state)
                    paint.setColor(Color.argb(255, s, s, s));
                else if (State.READY == state)
                    paint.setColor(Color.argb(255, 0, 0, s));
                canvas.drawLine(lineX, top, lineX, bottom, paint);
            }

        shade = 0;
        float left = (tileRect.left < World.CONTENT_LEFT) ? World.CONTENT_LEFT : tileRect.left;
        float right = (tileRect.right > World.CONTENT_RIGHT) ? World.CONTENT_RIGHT : tileRect.right;

        if (left <= right && left <= World.CONTENT_RIGHT && right >= World.CONTENT_LEFT)
            for (float lineY = World.CONTENT_TOP; lineY <= World.CONTENT_BOTTOM; lineY += World.Y_INC, shade++) {
                if (lineY < this.tileRect.top)
                    continue;
                if (lineY > this.tileRect.bottom)
                    break;

                int s = (int) (shade * World.COLOR_INC);
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
