package ca.dragonflystudios.atii;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

    private ReaderWorld mWorld;
    private ReaderWorldDrawer mWorldDrawer;
    private ReaderWorldView mWorldView;
    private ReaderGestureView mGestureView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWorld = new ReaderWorld();
        ReaderWorldPerspective rwp = new ReaderWorldPerspective(mWorld);
        mWorldView = new ReaderWorldView(this, rwp);
        mWorldDrawer = new ReaderWorldDrawer(mWorld, mWorldView, rwp);
        mWorldView.setWorldDrawingDelegate(mWorldDrawer);
        mGestureView = new ReaderGestureView(this, mWorldView);

        RelativeLayout mainView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        mainView.addView(mWorldView);
        mainView.addView(mGestureView);
        setContentView(mainView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
