package ca.dragonflystudios.atii.play;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import ca.dragonflystudios.android.view.SeesawButton;
import ca.dragonflystudios.atii.BuildConfig;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.atii.play.Page.AudioPlaybackState;
import ca.dragonflystudios.atii.play.PlayManager.PlayChangeListener;
import ca.dragonflystudios.atii.play.PlayManager.PlayMode;
import ca.dragonflystudios.atii.play.PlayManager.PlayState;
import ca.dragonflystudios.atii.view.ReaderGestureView.ReaderGestureListener;

public class Player extends FragmentActivity implements ReaderGestureListener, PlayChangeListener {

    public static final String STORY_EXTRA_KEY = "STORY_FOLDER_NAME";

    protected final static int CAPTURE_PHOTO = 0;

    public interface PlayCommandHandler {

        public void startAudioReplay();

        public void pauseAudioReplay();

        public void stopAudioReplay();

        public void togglePlayMode();

        public void startAudioRecording();

        public void stopAudioRecording();

        public void capturePhoto(ViewGroup hostView);

        public void capturePhoto(Activity requestingActivity);

        public void stopPhotoCapture();

        public void addPageBefore();

        public void addPageAfter();

        public void deletePage();
    }

    // TODO: hide Playback buttons when there is no audio

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        String storyPath = getIntent().getExtras().getString(STORY_EXTRA_KEY);

        mPlayManager = new PlayManager(storyPath, this);
        mPlayManager.setAutoReplay(true);
        mPlayManager.setAutoAdvance(true);

        mAdapter = new AtiiPagerAdapter(getSupportFragmentManager(), mPlayManager);

        setContentView(R.layout.player);

        mPager = (AtiiPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(mPlayManager);
        mPager.setReaderGestureListener(this);
        mPager.setCurrentItem(mPlayManager.getInitialPage());

        mControlsView = (ViewGroup) findViewById(R.id.controls);

        mModeButton = (SeesawButton) mControlsView.findViewById(R.id.mode);
        mModeButton.setSaw(mPlayManager.getPlayMode() == PlayMode.PLAYBACK);
        mModeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mModeButton.seesaw();
                mPlayManager.togglePlayMode();
            }
        });

        mPlayButton = (ImageButton) mControlsView.findViewById(R.id.play);
        mPlayButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.startAudioReplay();
            }
        });

        mPauseButton = (ImageButton) mControlsView.findViewById(R.id.pause);
        mPauseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.pauseAudioReplay();
            }
        });

        mRepeatButton = (ImageButton) mControlsView.findViewById(R.id.repeat);
        mRepeatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.startAudioReplay();
            }
        });

        mRecordButton = (ImageButton) mControlsView.findViewById(R.id.record);
        mRecordButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mControlsToggleAllowed = false;
                mPlayManager.startAudioRecording();
            }
        });

        mCaptureButton = (ImageButton) mControlsView.findViewById(R.id.capture);
        mCaptureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.capturePhoto(Player.this);
            }
        });

        mAddBeforeButton = (ImageButton) mControlsView.findViewById(R.id.add_before);
        mAddBeforeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.addPageBefore();
            }
        });

        mAddAfterButton = (ImageButton) mControlsView.findViewById(R.id.add_after);
        mAddAfterButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.addPageAfter();
            }
        });

        mDeleteButton = (ImageButton) mControlsView.findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.deletePage();
            }
        });

        mPageNumView = (TextView) mControlsView.findViewById(R.id.page_num);
        updatePageNumView(mPlayManager.getInitialPage());

        mStopButton = (ImageButton) findViewById(R.id.stop);
        mStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.stopAudioRecording();
                mControlsToggleAllowed = true;
            }
        });

        updateControls();
        mControlsToggleAllowed = true;
        hideAllControls();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPlayManager.isAutoReplay())
            mPlayManager.startAudioReplay();
    }

    @Override
    public void onPause() {
        super.onPause();

        mPlayManager.stopAudioReplay();
        mPlayManager.stopAudioRecording();
        mPlayManager.stopPhotoCapture();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

        case CAPTURE_PHOTO:
            if (resultCode == RESULT_OK) {
                onPageImageChanged(mPlayManager.getCurrentPageNum());
            }
        }
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
        if (mControlsToggleAllowed)
            toggleAllControls();
    }

    @Override
    // implementation for PlayChangeListener
    public void onAudioPlaybackStateChanged(AudioPlaybackState newState) {
        updateControls();
    }

    @Override
    // implementation for PlayChangeListener
    public void onPageChanged(int newPage) {
        updatePageNumView(newPage);
    }

    private void updatePageNumView(int newPage) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < newPage; i++)
            sb.append(" ¥ ");
        sb.append(newPage + 1);
        for (int i = newPage + 1; i < mPlayManager.getNumPages(); i++)
            sb.append(" ¥ ");

        mPageNumView.setText(sb);
    }

    @Override
    // implementation for PlayChangeListener
    public void onModeChanged(PlayMode newMode) {
        updateControls();
    }

    @Override
    // implementation for PlayChangeListener
    public void onPlayStateChanged(PlayState newState) {
        updateControls();
    }

    @Override
    // implementation for PlayChangeListener
    public void onPageImageChanged(int pageNum) {
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(pageNum);

        // the following line doesn't work!
        // mAdapter.notifyDataSetChanged();
    }

    @Override
    // implementation for PlayChangeListener
    public void requestPageChange(int newPage) {
        mPager.setCurrentItem(newPage);
    }

    private void updateControls() {
        mPager.setPageChangeEnabled(true);

        switch (mPlayManager.getPlayMode()) {
        case PLAYBACK:
            mModeButton.setSaw(true);
            setPlayoutControlsVisibility(View.INVISIBLE);
            mStopButton.setVisibility(View.INVISIBLE);
            switch (mPlayManager.getPlayState()) {
            case IDLE:
            case PLAYING_BACK_AUDIO:
                updateAudioPlaybackButtons();
                break;
            default:
                if (BuildConfig.DEBUG)
                    throw new IllegalStateException("illegal play state in playback mode");
                else
                    return;
            }
            break;
        case PLAYOUT:
            mModeButton.setSaw(false);
            switch (mPlayManager.getPlayState()) {
            case IDLE:
            case PLAYING_BACK_AUDIO:
                setPlayoutControlsVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.INVISIBLE);
                updateAudioPlaybackButtons();
                break;
            case RECORDING_AUDIO:
                mPager.setPageChangeEnabled(false);
                setPlayoutControlsVisibility(View.INVISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
                break;
            case CAPTURING_PHOTO:
                break;
            }
            break;
        default:
            break;
        }
    }

    private void updateAudioPlaybackButtons() {
        switch (mPlayManager.getAudioPlaybackState()) {
        case NO_AUDIO:
            switchPlaybackButton(null);
            break;
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
        default:
            if (BuildConfig.DEBUG)
                throw new IllegalStateException("invalid audio playback state");
            else
                return;
        }
    }

    private void switchPlaybackButton(ImageButton button) {
        mPlayButton.setVisibility(View.INVISIBLE);
        mPauseButton.setVisibility(View.INVISIBLE);
        mRepeatButton.setVisibility(View.INVISIBLE);

        if (null != button)
            button.setVisibility(View.VISIBLE);
    }

    private void setPlayoutControlsVisibility(int visibility) {
        mRecordButton.setVisibility(visibility);
        mCaptureButton.setVisibility(visibility);
        mAddBeforeButton.setVisibility(visibility);
        mAddAfterButton.setVisibility(visibility);
        mDeleteButton.setVisibility(visibility);
    }

    private void hideAllControls() {
        mControlsView.setVisibility(View.INVISIBLE);
    }

    private void toggleAllControls() {
        toggleViewVisibility(mControlsView);
    }

    private void toggleViewVisibility(View v) {
        if (View.VISIBLE == v.getVisibility())
            v.setVisibility(View.INVISIBLE);
        else
            v.setVisibility(View.VISIBLE);
    }

    private PlayManager mPlayManager;
    private AtiiPagerAdapter mAdapter;
    private AtiiPager mPager;

    private ViewGroup mControlsView;
    private SeesawButton mModeButton;
    private ImageButton mPlayButton, mPauseButton, mRepeatButton;
    private ImageButton mRecordButton, mStopButton;
    private ImageButton mCaptureButton;
    private ImageButton mAddBeforeButton, mAddAfterButton, mDeleteButton;
    private TextView mPageNumView;

    private boolean mControlsToggleAllowed;

}
