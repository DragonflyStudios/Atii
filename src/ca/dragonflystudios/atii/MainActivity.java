package ca.dragonflystudios.atii;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.widget.RelativeLayout;
import ca.dragonflystudios.android.storage.Storage;
import ca.dragonflystudios.atii.control.ReaderPerspective;
import ca.dragonflystudios.atii.model.story.Story;
import ca.dragonflystudios.atii.model.world.World;
import ca.dragonflystudios.atii.view.ReaderGestureView;
import ca.dragonflystudios.atii.view.ReaderView;
import ca.dragonflystudios.atii.view.ReaderViewDrawer;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        // WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Story story = null;
        if (Storage.isExternalStorageWriteable()) {
            File atiiDir = new File(Environment.getExternalStorageDirectory(), "Atii");
            File storyFolder = new File(atiiDir, "story1.atii");
            story = new Story(storyFolder);
        }

        World world = new World(story);
        ReaderView readerView = new ReaderView(this);

        ReaderPerspective readerPerspective = new ReaderPerspective(world, world, readerView);
        readerView.setOnLayoutListener(readerPerspective);

        ReaderViewDrawer viewDrawer = new ReaderViewDrawer(readerPerspective);
        readerView.setDrawingDelegate(viewDrawer);

        ReaderGestureView gestureView = new ReaderGestureView(this);
        gestureView.setReaderGestureListener(readerPerspective);

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
