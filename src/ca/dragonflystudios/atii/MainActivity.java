package ca.dragonflystudios.atii;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ReaderWorld world = new ReaderWorld();
        ReaderView readerView = new ReaderView(this);

        ReaderPerspective readerPerspective = new ReaderPerspective(world, world, readerView);
        readerView.setOnLayoutListener(readerPerspective);

        ReaderViewDrawer viewDrawer = new ReaderViewDrawer(readerPerspective);
        readerView.setDrawingDelegate(viewDrawer);

        ReaderGestureView gestureView = new ReaderGestureView(this, readerPerspective);

        RelativeLayout mainView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        mainView.addView(readerView);
        mainView.addView(gestureView);
        setContentView(mainView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
