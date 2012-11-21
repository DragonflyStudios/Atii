package ca.dragonflystudios.atii.player;

import java.io.File;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import ca.dragonflystudios.atii.BookListActivity;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.utilities.Pathname;

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
        ImageButton button = (ImageButton) findViewById(R.id.goto_first);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(0);
            }
        });

        button = (ImageButton) findViewById(R.id.goto_last);
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
