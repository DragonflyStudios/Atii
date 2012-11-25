package ca.dragonflystudios.atii.play;

import java.io.File;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
        OnPageChangeListener
{

    // TODO: hide Playback buttons when there is no audio

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mStoryPath = getIntent().getExtras().getString(BookListActivity.STORY_EXTRA_KEY);
        File storyDir = new File(mStoryPath);
        mStoryTitle = Pathname.extractStem(storyDir.getName());

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

        mControlsView = (ViewGroup) findViewById(R.id.controls);

        mPlayButton = (ImageButton) mControlsView.findViewById(R.id.play);
        mPlayButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.startPlaying();
            }
        });

        mPauseButton = (ImageButton) mControlsView.findViewById(R.id.pause);
        mPauseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.pausePlaying();
            }
        });

        mRepeatButton = (ImageButton) mControlsView.findViewById(R.id.repeat);
        mRepeatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.startPlaying();
            }
        });

        mCurrentPlaybackButton = mPlayButton;

        mRecordButton = (ImageButton) mControlsView.findViewById(R.id.record);
        mRecordButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.startRecording();
            }
        });
        mStopButton = (ImageButton) mControlsView.findViewById(R.id.stop);
        mStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.stopRecording();
            }
        });

        mCaptureButton = (ImageButton) mControlsView.findViewById(R.id.capture);
        mCaptureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.stopRecording();
            }
        });

        mAddBeforeButton = (ImageButton) mControlsView.findViewById(R.id.add_before);
        mAddBeforeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.stopRecording();
            }
        });

        mAddAfterButton = (ImageButton) mControlsView.findViewById(R.id.add_after);
        mAddAfterButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.stopRecording();
            }
        });

        mDeleteButton = (ImageButton) mControlsView.findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                mPlayerState.stopRecording();
            }
        });

        mCurrentRecordButton = mRecordButton;
        mPageNumView = (TextView) mControlsView.findViewById(R.id.page_num);

        hidePlayupControls();
        hideAllControls();
    }

    public String getStoryTitle()
    {
        return mStoryTitle;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.player, menu);
        return true;
    }

    @Override
    // implementation for ReaderGestureListener
    public void onPanning(float deltaX, float deltaY)
    {
    }

    @Override
    // implementation for ReaderGestureListener
    public void onScaling(float scaling, float focusX, float focusY)
    {

    }

    @Override
    // implementation for ReaderGestureListener
    public void onSingleTap(float x, float y)
    {
        toggleAllControls();
    }

    @Override
    // implementation for OnReplayChangeListener
    public void onReplayStateChanged(ReplayState newState)
    {
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
    public void onPageChanged(int newPage)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < newPage; i++)
            sb.append(" ¥ ");
        sb.append(newPage + 1);
        for (int i = newPage + 1; i < mPlayerState.getNumPages(); i++)
            sb.append(" ¥ ");

        mPageNumView.setText(sb);
    }

    @Override
    // implementation for OnModeChangeListener
    public void onModeChanged(PlayerMode newMode)
    {
        switch (newMode) {
        case PLAYBACK:
            mCurrentRecordButton.setVisibility(View.INVISIBLE);
            mCurrentRecordButton = mRecordButton;
            mCurrentRecordButton.setVisibility(View.INVISIBLE);
            if (mPlayerState.hasAudioOnCurrentPage())
                mCurrentPlaybackButton.setVisibility(View.VISIBLE);
            break;
        case PLAYUP:
            mCurrentPlaybackButton.setVisibility(View.INVISIBLE);
            mCurrentRecordButton.setVisibility(View.VISIBLE);
            break;
        default:
            break;
        }
    }

    private void switchPlaybackButton(ImageButton button)
    {
        if (mCurrentPlaybackButton != button) {
            mCurrentPlaybackButton.setVisibility(View.INVISIBLE);
            mCurrentPlaybackButton = button;
        }

        if (mPlayerState.hasAudioOnCurrentPage())
            mCurrentPlaybackButton.setVisibility(View.VISIBLE);
    }

    private void switchRecordButton(ImageButton button)
    {
        if (mCurrentRecordButton != button) {
            mCurrentRecordButton.setVisibility(View.INVISIBLE);
            mCurrentRecordButton = button;
        }

            mCurrentRecordButton.setVisibility(View.VISIBLE);
    }

    private void hideAllControls()
    {
        mControlsView.setVisibility(View.INVISIBLE);
    }

    private void toggleAllControls()
    {
        toggleView(mControlsView);
    }

    private void hidePlayupControls()
    {
        mRecordButton.setVisibility(View.INVISIBLE);
        mStopButton.setVisibility(View.INVISIBLE);
        mCaptureButton.setVisibility(View.INVISIBLE);
        mAddBeforeButton.setVisibility(View.INVISIBLE);
        mAddAfterButton.setVisibility(View.INVISIBLE);
        mDeleteButton.setVisibility(View.INVISIBLE);
    }

    private void togglePlayupControls()
    {
        toggleView(mCurrentRecordButton);
        toggleView(mCaptureButton);
        toggleView(mAddBeforeButton);
        toggleView(mAddAfterButton);
        toggleView(mDeleteButton);
    }

    private void toggleView(View v)
    {
        if (View.VISIBLE == v.getVisibility())
            v.setVisibility(View.INVISIBLE);
        else
            v.setVisibility(View.VISIBLE);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_record_audio:
            mPlayerState.toggleModeWithPlayback(PlayerMode.RECORD);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private String        mStoryPath;
    private String        mStoryTitle;
    private PlayerAdapter mAdapter;
    private AtiiViewPager mPager;
    private ImageButton   mCurrentPlaybackButton, mPlayButton, mPauseButton, mRepeatButton;
    private ImageButton   mCurrentRecordButton, mRecordButton, mStopButton;
    private ImageButton   mCaptureButton;
    private ImageButton   mAddBeforeButton, mAddAfterButton, mDeleteButton;
    private TextView      mPageNumView;

    private ViewGroup     mControlsView;

    private PlayerState   mPlayerState;

}
