package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    @Override
    // ReaderWorldDrawable implementation
    public void draw(Canvas canvas, Paint paint) {

        // TODO: smarter clipping is necessary

        final float strokeWidth = paint.getStrokeWidth();
        paint.setStrokeWidth(1 / this.worldToViewScale * ReaderWorld.GRID_LINE_WIDTH);

        int shade = 0;
        for (float lineX = ReaderWorld.CONTENT_LEFT; lineX <= ReaderWorld.CONTENT_RIGHT; lineX += ReaderWorld.X_INC, shade++) {
            if (lineX < this.tileRect.left)
                continue;
            if (lineX > this.tileRect.right)
                break;

            int s = (int) (shade * ReaderWorld.COLOR_INC);
            if (isPending())
                paint.setColor(Color.argb(255, s, s, s));
            else if (isReady())
                paint.setColor(Color.argb(255, 0, 0, s));
            else
                throw new RuntimeException("attempting to draw tile in bad state");
            canvas.drawLine(lineX, ReaderWorld.CONTENT_TOP, lineX, ReaderWorld.CONTENT_BOTTOM, paint);
        }

        shade = 0;
        for (float lineY = ReaderWorld.CONTENT_TOP; lineY <= ReaderWorld.CONTENT_BOTTOM; lineY += ReaderWorld.Y_INC, shade++) {
            if (lineY < this.tileRect.top)
                continue;
            if (lineY > this.tileRect.bottom)
                break;

            int s = (int) (shade * ReaderWorld.COLOR_INC);
            if (isPending())
                paint.setColor(Color.argb(255, s, s, s));
            else if (isReady())
                paint.setColor(Color.argb(255, 0, s, 0));
            else
                throw new RuntimeException("attempting to draw tile in bad state");
            canvas.drawLine(ReaderWorld.CONTENT_LEFT, lineY, ReaderWorld.CONTENT_RIGHT, lineY, paint);
        }

        paint.setStrokeWidth(strokeWidth);
    }

    private State mState;
}
