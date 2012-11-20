package ca.dragonflystudios.atii.player;

import java.io.File;
import java.util.Observable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import ca.dragonflystudios.atii.BookListActivity;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.utilities.Pathname;

/*
 * TODO:
 * [x] Playback a series of photos         1-day
 * [x] Page-based navigation               0.5-day
 * [x] Put stock stories in assets         0.5-day
 * [ ] Playback audios                     1-day
 * [ ] Basic control for playback in place 1-day
 * [ ] Replace audio with recording        0.5-day
 * [ ] Replace page image with photo       0.5-day
 * [ ] Make buttons translucent & pretty   1-day
 * [ ] Single tap toggle buttons, action bar & page number view  0.5-day
 * [ ] Playback a series of PDF pages      1-day
 * [ ] Double tap to zoom in               0.5-day
 * 
 * ~~ The following breaks 1-audio-per-page correspondence ~~
 * [ ] Audio for zoomed-in state           1-day
 * 
 * ~~ The following starts the sharing business ~~
 * [ ] Google Drive integration
 */

public class Player extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        mStoryPath = getIntent().getExtras().getString(BookListActivity.STORY_EXTRA_KEY);
        File storyDir = new File(mStoryPath);
        mStoryTitle = Pathname.extractStem(storyDir.getName());

        getActionBar().setTitle(mStoryTitle);
        getActionBar().hide();

        // TODO: get the directory from intent and use that to initialize
        // PlayerAdapter ...
        mAdapter = new PlayerAdapter(getSupportFragmentManager(), storyDir);

        setContentView(R.layout.player);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(mAdapter);

        // Watch for button clicks.
        Button button = (Button) findViewById(R.id.goto_first);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(0);
            }
        });

        button = (Button) findViewById(R.id.goto_last);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(mAdapter.getCount() - 1);
            }
        });
}

    public String getStoryTitle() {
        return mStoryTitle;
    }

    private String mStoryPath;
    private String mStoryTitle;
    private PlayerAdapter mAdapter;
    private ViewPager mPager;
}
