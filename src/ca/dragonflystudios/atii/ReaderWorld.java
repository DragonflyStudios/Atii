package ca.dragonflystudios.atii;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.dragonflystudios.atii.ReaderWorldDrawer.DrawableWorld;
import ca.dragonflystudios.atii.ReaderWorldDrawer.WorldWindowDelegate;

public class ReaderWorld implements WorldWindowDelegate, DrawableWorld {

    // the "world" is made of 256 horizontal and 256 vertical grid lines of shades of, respectively, blue and green 
    private static final float WORLD_LEFT = 0f;
    private static final float WORLD_RIGHT = 8.5f;
    private static final float WORLD_TOP = 0f;
    private static final float WORLD_BOTTOM = 11f;

    private static final float WORLD_MARGIN = 1f;

    private static final float X_INC = WORLD_RIGHT / 255;
    private static final float Y_INC = WORLD_BOTTOM / 255;

    public ReaderWorld()
    {
        mPaint = new Paint();
    }
    
    private Paint mPaint;

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinX(RectF worldWindow) {
        return WORLD_LEFT - WORLD_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMinY(RectF worldWindow) {
        return WORLD_TOP - WORLD_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMaxX(RectF worldWindow) {
        return WORLD_RIGHT - (worldWindow.right - worldWindow.left) + WORLD_MARGIN;
    }

    // WorldWindowDelegate implementation
    @Override
    public float getLimitMaxY(RectF worldWindow) {
        return WORLD_BOTTOM - (worldWindow.bottom - worldWindow.top) + WORLD_MARGIN;
    }

    // DrawableWorld implementation
    @Override
    public void draw(Canvas canvas, RectF worldWindow) {
        int shadeX = (int)((worldWindow.left - WORLD_LEFT) / X_INC);
        for (float lineX = worldWindow.left; lineX <= worldWindow.right; lineX += X_INC, shadeX++) {
            mPaint.setColor(Color.argb(255, shadeX, shadeX, 255));
            canvas.drawLine(lineX, worldWindow.top, lineX, worldWindow.bottom, mPaint);
        }

        int shadeY = (int)((worldWindow.left - WORLD_LEFT) / Y_INC);
        for (float lineY = worldWindow.left; lineY <= worldWindow.right; lineY += Y_INC, shadeY++) {
            mPaint.setColor(Color.argb(255, shadeY, 255, shadeY));
            canvas.drawLine(worldWindow.left, lineY, worldWindow.right, lineY, mPaint);
        }
    }
}
