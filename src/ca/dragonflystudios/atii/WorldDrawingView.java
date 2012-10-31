package ca.dragonflystudios.atii;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public class WorldDrawingView extends View {

    public interface WorldDrawingDelegate {
        public void draw(Canvas canvas);
    }

    WorldDrawingDelegate mWorldDrawingDelegate;

    public WorldDrawingView(WorldDrawingDelegate wdd, Context context) {
        super(context);
        mWorldDrawingDelegate = wdd;
    }

    @Override
    public void onDraw(Canvas canvas) {
        mWorldDrawingDelegate.draw(canvas);

        // Draw other things
    }
}
