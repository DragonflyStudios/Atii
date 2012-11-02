package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderWorldDrawer.DrawableWorld;
import ca.dragonflystudios.atii.ReaderWorldDrawer.WorldWindowDelegate;

public class ReaderWorld implements WorldWindowDelegate, DrawableWorld {

    // the "world" is made of GRID_COUNT horizontal and GRID_COUNT vertical grid
    // lines of shades of, respectively, blue and green
    private static final float GRID_COUNT = 64f;

    private static final float CONTENT_LEFT = 0f;
    private static final float CONTENT_RIGHT = 8.5f;
    private static final float CONTENT_TOP = 0f;
    private static final float CONTENT_BOTTOM = 11f;

    private static final float CONTENT_MARGIN = 1f;

    private static final float X_INC = (CONTENT_RIGHT - CONTENT_LEFT) / GRID_COUNT;
    private static final float Y_INC = (CONTENT_BOTTOM - CONTENT_TOP) / GRID_COUNT;
    private static final float COLOR_INC = 255f / GRID_COUNT;

    private static final float GRID_LINE_WIDTH = 2f;

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinX(RectF worldWindow) {
        return CONTENT_LEFT - CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinY(RectF worldWindow) {
        return CONTENT_TOP - CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMaxX(RectF worldWindow) {
        return CONTENT_RIGHT - (worldWindow.right - worldWindow.left) + 2 * CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMaxY(RectF worldWindow) {
        return CONTENT_BOTTOM - (worldWindow.bottom - worldWindow.top) + 2 * CONTENT_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public RectF getWorldRect() {
        return new RectF(CONTENT_LEFT - CONTENT_MARGIN, CONTENT_TOP - CONTENT_MARGIN, CONTENT_RIGHT + CONTENT_MARGIN,
                CONTENT_BOTTOM + CONTENT_MARGIN);
    }

    // DrawableWorld implementation
    @Override
    public void draw(Canvas canvas, RectF worldWindow, Paint paint) {

        // TODO: smarter clipping is necessary

        final float strokeWidth = paint.getStrokeWidth();
        paint.setStrokeWidth(strokeWidth * GRID_LINE_WIDTH);
        paint.setColor(Color.GRAY);
        canvas.drawRect(worldWindow, paint);

        int shade = 0;
        for (float lineX = CONTENT_LEFT; lineX <= CONTENT_RIGHT; lineX += X_INC, shade++) {
            if (lineX < worldWindow.left)
                continue;
            if (lineX > worldWindow.right)
                break;

            int s = (int)(shade * COLOR_INC);
            paint.setColor(Color.argb(255, 0, 0, s));
            canvas.drawLine(lineX, CONTENT_TOP, lineX, CONTENT_BOTTOM, paint);
        }

        shade = 0;
        for (float lineY = CONTENT_TOP; lineY <= CONTENT_BOTTOM; lineY += Y_INC, shade++) {
            if (lineY < worldWindow.top)
                continue;
            if (lineY > worldWindow.bottom)
                break;

            int s = (int)(shade * COLOR_INC);
            paint.setColor(Color.argb(255, 0, s, 0));
            canvas.drawLine(CONTENT_LEFT, lineY, CONTENT_RIGHT, lineY, paint);
        }

        paint.setStrokeWidth(strokeWidth);
    }
}
