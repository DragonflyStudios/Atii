package ca.dragonflystudios.atii.play;

import java.io.File;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import ca.dragonflystudios.atii.BookListActivity;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.atii.play.PlayerState.OnModeChangeListener;
import ca.dragonflystudios.atii.play.PlayerState.OnPageChangeListener;
import ca.dragonflystudios.atii.play.PlayerState.OnReplayChangeListener;
import ca.dragonflystudios.atii.play.PlayerState.PlayerMode;
import ca.dragonflystudios.atii.play.PlayerState.ReplayState;
import ca.dragonflystudios.atii.view.ReaderGestureView.ReaderGestureListener;
import ca.dragonflystudios.utilities.Pathname;

public class Player extends FragmentActivity implements ReaderGestureListener, OnModeChangeListener, OnReplayChangeListener,
        OnPageChangeListener {

    // TODO: hide Playback buttons when there is no audio

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        mStoryPath = getIntent().getExtras().getString(BookListActivity.STORY_EXTRA_KEY);
        File storyDir = new File(mStoryPath);
        mStoryTitle = Pathname.extractStem(storyDir.getName());

        getActionBar().setTitle(mStoryTitle);

        // TODO: get the directory from intent and use that to initialize
        // PlayerAdapter ...
        mAdapter = new PlayerAdapter(getSupportFragmentManager(), storyDir);
        mPlayerState = new PlayerState(mAdapter.getCount(), this, this, this);
        mPlayerState.setAutoReplay(true);
        mAdapter.setPlayerState(mPlayerState);

        setContentView(R.layout.player);
        mPager = (AtiiViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(mAdapter);
        mPager.setReaderGestureListener(this);

        mPlayButton = (ImageButton) findViewById(R.id.play);
        mPlayButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayerState.startPlaying();
            }
        });

        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayerState.pausePlaying();
            }
        });

        mRepeatButton = (ImageButton) findViewById(R.id.repeat);
        mRepeatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayerState.startPlaying();
            }
        });

        mCurrentPlaybackButton = mPlayButton;

        mRecordButton = (ImageButton) findViewById(R.id.record);
        mRecordButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayerState.startRecording();
            }
        });
        mStopButton = (ImageButton) findViewById(R.id.stop);
        mStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayerState.stopRecording();
            }
        });
        mCurrentRecordButton = mRecordButton;

        // Watch for button clicks.
        mFirstButton = (ImageButton) findViewById(R.id.goto_first);
        mFirstButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(0);
            }
        });

        mLastButton = (ImageButton) findViewById(R.id.goto_last);
        mLastButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(mAdapter.getCount() - 1);
            }
        });

        mPageNumView = (TextView) findViewById(R.id.page_num);

        hideAllControls();
    }

    public String getStoryTitle() {
        return mStoryTitle;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player, menu);
        return true;
    }

    @Override
    // implementation for ReaderGestureListener
    public void onPanning(float deltaX, float deltaY) {
    }

    @Override
    // implementation for ReaderGestureListener
    public void onScaling(float scaling, float focusX, float focusY) {

    }

    @Override
    // implementation for ReaderGestureListener
    public void onSingleTap(float x, float y) {
        toggleControls();
    }

    @Override
    // implementation for OnReplayChangeListener
    public void onReplayStateChanged(ReplayState newState) {
        switch (newState) {
        case NOT_STARTED:
            switchPlaybackButton(mPlayButton);
            break;
        case PLAYING:
            switchPlaybackButton(mPauseButton);
            break;
        case PAUSED:
            switchPlaybackButton(mPlayButton);
            break;
        case FINISHED:
            switchPlaybackButton(mRepeatButton);
            break;
        case RECORDING:
            switchRecordButton(mStopButton);
        default:
            break;
        }
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageChanged(int newPage) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < newPage; i++)
            sb.append(" � ");
        sb.append(newPage + 1);
        for (int i = newPage + 1; i < mPlayerState.getNumPages(); i++)
            sb.append(" � ");

        mPageNumView.setText(sb);
    }

    @Override
    // implementation for OnModeChangeListener
    public void onModeChanged(PlayerMode newMode) {
        switch (newMode) {
        case PLAYBACK:
            mCurrentRecordButton.setVisibility(View.INVISIBLE);
            mCurrentRecordButton = mRecordButton;
            mCurrentRecordButton.setVisibility(View.INVISIBLE);
            if (getActionBar().isShowing() && mPlayerState.hasAudioOnCurrentPage())
                mCurrentPlaybackButton.setVisibility(View.VISIBLE);
            break;
        case RECORD:
            mCurrentPlaybackButton.setVisibility(View.INVISIBLE);
            if (getActionBar().isShowing())
                mCurrentRecordButton.setVisibility(View.VISIBLE);
            break;
        default:
            break;
        }
    }

    private void switchPlaybackButton(ImageButton button) {
        if (mCurrentPlaybackButton != button) {
            mCurrentPlaybackButton.setVisibility(View.INVISIBLE);
            mCurrentPlaybackButton = button;
        }

        if (getActionBar().isShowing() && mPlayerState.hasAudioOnCurrentPage())
            mCurrentPlaybackButton.setVisibility(View.VISIBLE);
    }

    private void switchRecordButton(ImageButton button) {
        if (mCurrentRecordButton != button) {
            mCurrentRecordButton.setVisibility(View.INVISIBLE);
            mCurrentRecordButton = button;
        }

        if (getActionBar().isShowing())
            mCurrentRecordButton.setVisibility(View.VISIBLE);
    }

    private void hideAllControls() {
        getActionBar().hide();
        mPlayButton.setVisibility(View.INVISIBLE);
        mPauseButton.setVisibility(View.INVISIBLE);
        mRepeatButton.setVisibility(View.INVISIBLE);
        mRecordButton.setVisibility(View.INVISIBLE);
        mStopButton.setVisibility(View.INVISIBLE);
        mFirstButton.setVisibility(View.INVISIBLE);
        mLastButton.setVisibility(View.INVISIBLE);
        mPageNumView.setVisibility(View.INVISIBLE);
    }

    private void toggleControls() {
        ActionBar actionBar = getActionBar();
        if (actionBar.isShowing())
            actionBar.hide();
        else
            actionBar.show();

        toggleView(mFirstButton);
        toggleView(mLastButton);
        switch (mPlayerState.getCurrentMode()) {
        case PLAYBACK:
            toggleView(mCurrentPlaybackButton);
            break;
        case RECORD:
            toggleView(mCurrentRecordButton);
            break;
        default:
            break;
        }

        toggleView(mPageNumView);
    }

    private void toggleView(View v) {
        if (View.VISIBLE == v.getVisibility())
            v.setVisibility(View.INVISIBLE);
        else
            v.setVisibility(View.VISIBLE);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_record_audio:
            mPlayerState.toggleModeWithPlayback(PlayerMode.RECORD);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private String mStoryPath;
    private String mStoryTitle;
    private PlayerAdapter mAdapter;
    private AtiiViewPager mPager;
    private ImageButton mCurrentPlaybackButton, mPlayButton, mPauseButton, mRepeatButton;
    private ImageButton mCurrentRecordButton, mRecordButton, mStopButton;
    private ImageButton mFirstButton, mLastButton;
    private TextView mPageNumView;

    private PlayerState mPlayerState;

}