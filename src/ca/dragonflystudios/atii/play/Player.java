package ca.dragonflystudios.atii.play;

import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import ca.dragonflystudios.android.dialog.WarningDialogFragment;
import ca.dragonflystudios.android.dialog.WarningDialogFragment.WarningDialogListener;
import ca.dragonflystudios.android.view.SeesawButton;
import ca.dragonflystudios.atii.BuildConfig;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.atii.model.book.Page;
import ca.dragonflystudios.atii.model.book.Page.AudioPlaybackState;
import ca.dragonflystudios.atii.play.PageFragment.OnPageImageChoice;
import ca.dragonflystudios.atii.play.PlayManager.PlayChangeListener;
import ca.dragonflystudios.atii.play.PlayManager.PlayMode;
import ca.dragonflystudios.atii.play.PlayManager.PlayState;
import ca.dragonflystudios.atii.view.ReaderGestureView.ReaderGestureListener;

public class Player extends FragmentActivity implements ReaderGestureListener, PlayChangeListener, WarningDialogListener,
        OnPageImageChoice, OnSeekBarChangeListener {

    public static final String STORY_EXTRA_KEY = "STORY_FOLDER_NAME";
    public static final String PLAY_MODE_EXTRA_KEY = "PLAY_MODE";

    private static final String PAGE_DELETION_DIALOG_TAG = "PageDeletionDialogFragment";

    protected final static int CAPTURE_IMAGE = 0;
    protected final static int PICK_IMAGE = 1;

    public interface PlayCommandHandler {

        public void startAudioReplay();

        public void pauseAudioReplay();

        public void seekTo(int position);

        public void stopAudioReplay();

        public void togglePlayMode();

        public void startAudioRecording();

        public void stopAudioRecording();

        public void captureImage(Activity requestingActivity);

        public void pickImage(Activity requestingActivity);

        public void keepNewPageImage();

        public void discardNewPageImage();

        public void addPageBefore();

        public void addPageAfter();

        public void deletePage();
    }

    public interface PlayerState {

        public PlayMode getPlayMode();

        public PlayState getPlayState();

        public int getPageNum();

        public int getNumPages();

        public Page getPage(int pageNum);

        public AudioPlaybackState getAudioPlaybackState();

        public int getTrackDuration();

        public int getProgress();

        public boolean isAutoReplay();

        public void setAutoReplay(boolean auto);

        public boolean isAutoAdvance();

        public void setAutoAdvance(boolean auto);
    }

    /**
     * For passing results from startActivityForResult
     * 
     * @author Luo, Jun
     * 
     */
    public interface ImageRequestResultListener {

        public void onGettingImageFailure();

        public void onImageCaptured();

        public void onImagePicked(Uri data, ContentResolver resolver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        String storyPath = getIntent().getExtras().getString(STORY_EXTRA_KEY);
        PlayMode playMode = PlayMode.valueOf(getIntent().getExtras().getString(PLAY_MODE_EXTRA_KEY));

        mPlayManager = new PlayManager(storyPath, this, playMode);
        mPlayManager.setAutoReplay(true);
        mPlayManager.setAutoAdvance(false);

        mAdapter = new AtiiPagerAdapter(getSupportFragmentManager(), mPlayManager);

        setContentView(R.layout.player);

        mPager = (AtiiPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(mPlayManager);
        mPager.setReaderGestureListener(this);
        // TODO: refactor: deal with this
        mPager.setCurrentItem(mPlayManager.getInitialPage());

        mControlsView = (ViewGroup) findViewById(R.id.controls);

        mModeButton = (SeesawButton) mControlsView.findViewById(R.id.mode);
        mModeButton.setSaw(mPlayManager.getPlayMode() == PlayMode.READER);
        mModeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mModeButton.seesaw();
                mPlayManager.togglePlayMode();
            }
        });
        mModeButton.setEnabled(false);
        mModeButton.setVisibility(View.GONE);

        mTrackInfoView = (TextView) mControlsView.findViewById(R.id.track_info);
        mAudioStatusView = (TextView) mControlsView.findViewById(R.id.audio_status);
        mReplaySeekBar = (SeekBar) mControlsView.findViewById(R.id.replay_seek_bar);
        mReplaySeekBar.setOnSeekBarChangeListener(this);

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

        mRecordButton = (ImageButton) mControlsView.findViewById(R.id.record);
        mRecordButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.startAudioRecording();
            }
        });

        mPickPictureButton = (ImageButton) mControlsView.findViewById(R.id.pick_picture);
        mPickPictureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.pickImage(Player.this);
            }
        });

        mCaptureButton = (ImageButton) mControlsView.findViewById(R.id.capture);
        mCaptureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.captureImage(Player.this);
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
                DialogFragment dialog = WarningDialogFragment.newInstance(R.string.page_deletion_dialog_title,
                        R.string.deletion_no_undo_warning);
                dialog.show(getFragmentManager(), PAGE_DELETION_DIALOG_TAG);
            }
        });

        mPageNumView = (TextView) mControlsView.findViewById(R.id.page_num);

        mStopButton = (ImageButton) findViewById(R.id.stop);
        mStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.stopAudioRecording();
            }
        });

        refreshStatusDisplays();
        refreshControls();
        mControlsToggleAllowed = true;
        hideAllControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPlayManager.onResume();
    }

    @Override
    public void onPause() {
        mPlayManager.onPause();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (RESULT_OK != resultCode) {
            mPlayManager.onGettingImageFailure();
            return;
        }

        switch (requestCode) {
        case CAPTURE_IMAGE:
            mPlayManager.onImageCaptured();
            break;
        case PICK_IMAGE:
            mPlayManager.onImagePicked(data.getData(), getContentResolver());
            break;
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
            runOnUiThread(new Runnable() {
                public void run() {
                    toggleAllControls();
                }
            });
    }

    @Override
    // implementation for PlayChangeListener
    public void onAudioPlaybackStateChanged(AudioPlaybackState oldState, final AudioPlaybackState newState) {
        runOnUiThread(new Runnable() {
            public void run() {
                switch (newState) {
                case PLAYING:
                    mPlayingTimer.start();
                    break;
                default:
                    mPlayingTimer.cancel();
                    break;
                }

                refreshControls();
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPageChanged(final int newPage) {
        runOnUiThread(new Runnable() {
            public void run() {
                refreshPageNumView();
                refreshProgress();
            }
        });
    }

    private void refreshStatusDisplays() {
        refreshPageNumView();
        refreshProgress();
    }

    private void refreshPageNumView() {
        int pageNum = mPlayManager.getPageNum();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < pageNum; i++)
            sb.append(" ¥ ");
        sb.append(pageNum + 1);
        for (int i = pageNum + 1; i < mPlayManager.getNumPages(); i++)
            sb.append(" ¥ ");

        mPageNumView.setText(sb);
    }

    @Override
    // implementation for PlayChangeListener
    public void onModeChanged(PlayMode oldMode, PlayMode newMode) {
        runOnUiThread(new Runnable() {
            public void run() {
                refreshControls();
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPlayStateChanged(PlayState oldState, PlayState newState) {
        runOnUiThread(new Runnable() {
            public void run() {
                refreshControls();
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPageImageChanged(int pageNum) {
        refreshPageImage(pageNum);
    }

    private void refreshPageImage(int pageNum) {
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(pageNum);

        // Had to use the above because the following line does not work!
        // mAdapter.notifyDataSetChanged();
    }

    public void refreshProgress() {
        int progress = mPlayManager.getProgress();
        int duration = mPlayManager.getTrackDuration();

        Log.w("refreshProgress", progress + " of " + duration);

        if (duration > 0) {
            mReplaySeekBar.setVisibility(View.VISIBLE);
            mTrackInfoView.setVisibility(View.VISIBLE);
            mAudioStatusView.setVisibility(View.INVISIBLE);
        } else if (duration <= 0) {
            mReplaySeekBar.setVisibility(View.INVISIBLE);
            mTrackInfoView.setVisibility(View.INVISIBLE);
            mAudioStatusView.setVisibility(View.VISIBLE);
            mAudioStatusView.setText(R.string.no_audio);
        }

        if (mDuration != duration) {
            if (duration > 0)
                mReplaySeekBar.setMax(duration);
            mDuration = duration;
        }

        if (mDuration > 0 & progress >= 0) {
            SimpleDateFormat df = new SimpleDateFormat("mm:ss");
            String trackInfoText = df.format(progress) + " / " + df.format(duration);
            mTrackInfoView.setText(trackInfoText);
            mReplaySeekBar.setProgress(progress);
        }
    }

    @Override
    // implementation for PlayChangeListener
    public void requestMoveToPage(int newPage) {
        mPager.setCurrentItem(newPage);
        refreshPageNumView();
        refreshProgress();
    }

    @Override
    // implementation for PlayChangeListener
    public void onPagesEdited(int newPage) {
        refreshPageImage(newPage);
        refreshPageNumView();
        refreshProgress();
    }

    @Override
    // implementation for WarningDialogListener
    public void onPositive(WarningDialogFragment wdf) {
        if (wdf.getTag().equals(PAGE_DELETION_DIALOG_TAG))
            mPlayManager.deletePage();
    }

    @Override
    // implementation for WarningDialogListener
    public void onNegative(WarningDialogFragment wdf) {
    }

    @Override
    // implementation for OnPageImageChoice
    public void onDiscard() {
        mPlayManager.discardNewPageImage();
    }

    @Override
    // implementation for OnPageImageChoice
    public void onKeep() {
        mPlayManager.keepNewPageImage();
    }

    @Override
    // implementation for OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    }

    @Override
    // implementation for OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHasBeenPlaying = mPlayManager.isPlaying();
        mPlayManager.pauseAudioReplay();
        mPlayingTimer.cancel();
    }

    /**
     * When user stops moving the progress hanlder
     * */
    @Override
    // implementation for OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        mPlayManager.seekTo(mReplaySeekBar.getProgress());
        if (mHasBeenPlaying) {
            mPlayManager.startAudioReplay();
            mPlayingTimer.start();
        }
    }

    // TODO: refactor: think hard about how these work with state transitions
    private void refreshControls() {
        mPager.setPageChangeEnabled(true);

        switch (mPlayManager.getPlayMode()) {
        case READER:
            mModeButton.setSaw(true);
            setAuthoringControlsVisibility(View.INVISIBLE);
            showAllControls();
            mPager.setPageChangeEnabled(true);
            switch (mPlayManager.getPlayState()) {
            case IDLE:
            case PLAYING_BACK_AUDIO:
                mRecordingTimer.cancel();
                mSecondsRecorded = 0;
                mControlsToggleAllowed = true;
                mStopButton.setVisibility(View.INVISIBLE);
                mAudioStatusView.setVisibility(View.INVISIBLE);
                mRecordButton.setVisibility(View.VISIBLE);
                setAudioPlaybackControlsVisibility(View.VISIBLE);
                refreshAudioPlaybackButtons();
                refreshProgress();
                break;
            case RECORDING_AUDIO:
                mControlsToggleAllowed = false;
                mSecondsRecorded = 0;
                mRecordingTimer.start();
                mPager.setPageChangeEnabled(false);
                setAuthoringControlsVisibility(View.INVISIBLE);
                setAudioPlaybackControlsVisibility(View.INVISIBLE);
                mRecordButton.setVisibility(View.INVISIBLE);
                mAudioStatusView.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
                break;
            default:
                if (BuildConfig.DEBUG)
                    throw new IllegalStateException("illegal play state in playback mode");
                else
                    return;
            }
            break;
        case AUTHOR:
            mModeButton.setSaw(false);
            switch (mPlayManager.getPlayState()) {
            case IDLE:
            case PLAYING_BACK_AUDIO:
                mRecordingTimer.cancel();
                mSecondsRecorded = 0;
                mControlsToggleAllowed = true;
                setAuthoringControlsVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.INVISIBLE);
                mAudioStatusView.setVisibility(View.INVISIBLE);
                mRecordButton.setVisibility(View.VISIBLE);
                setAudioPlaybackControlsVisibility(View.VISIBLE);
                refreshAudioPlaybackButtons();
                refreshProgress();
                break;
            case RECORDING_AUDIO:
                mControlsToggleAllowed = false;
                mSecondsRecorded = 0;
                mRecordingTimer.start();
                mPager.setPageChangeEnabled(false);
                setAuthoringControlsVisibility(View.INVISIBLE);
                setAudioPlaybackControlsVisibility(View.INVISIBLE);
                mRecordButton.setVisibility(View.INVISIBLE);
                mAudioStatusView.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
                break;
            case GETTING_IMAGE:
                mPager.setPageChangeEnabled(false);
                hideAllControls();
                break;
            }
            break;
        default:
            break;
        }
    }

    private void refreshAudioPlaybackButtons() {
        switch (mPlayManager.getAudioPlaybackState()) {
        case NO_AUDIO:
            switchPlaybackButton(null);
            break;
        case PLAYING:
            switchPlaybackButton(mPauseButton);
            break;
        case NOT_STARTED:
        case PAUSED:
        case FINISHED:
            switchPlaybackButton(mPlayButton);
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

        if (null != button)
            button.setVisibility(View.VISIBLE);
    }

    private void setAuthoringControlsVisibility(int visibility) {
        mPickPictureButton.setVisibility(visibility);
        mCaptureButton.setVisibility(visibility);
        mAddBeforeButton.setVisibility(visibility);
        mAddAfterButton.setVisibility(visibility);
        mDeleteButton.setVisibility(visibility);
    }

    private void setAudioPlaybackControlsVisibility(int visibility) {
        mPlayButton.setVisibility(visibility);
        mPauseButton.setVisibility(visibility);
        mReplaySeekBar.setVisibility(visibility);
        mTrackInfoView.setVisibility(visibility);
    }

    private void showAllControls() {
        mControlsView.setVisibility(View.VISIBLE);
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
    private ImageButton mPlayButton, mPauseButton;
    private ImageButton mRecordButton, mStopButton;
    private ImageButton mCaptureButton, mPickPictureButton;
    private ImageButton mAddBeforeButton, mAddAfterButton, mDeleteButton;
    private TextView mPageNumView, mTrackInfoView, mAudioStatusView;
    private SeekBar mReplaySeekBar;

    private boolean mHasBeenPlaying;

    private boolean mControlsToggleAllowed;

    private int mDuration = -1;

    private int mSecondsRecorded = 0;
    private CountDownTimer mRecordingTimer = new CountDownTimer(86400000, 1000) {
        public void onTick(long millisUntilFinished) {
            mSecondsRecorded++;
            int minutes = mSecondsRecorded / 60;
            int seconds = mSecondsRecorded % 60;

            if (seconds < 10) {
                mAudioStatusView.setText("" + minutes + ":0" + seconds);
            } else {
                mAudioStatusView.setText("" + minutes + ":" + seconds);
            }
        }

        public void onFinish() {
        }
    };

    private CountDownTimer mPlayingTimer = new CountDownTimer(86400000, 100) {
        public void onTick(long millisUntilFinished) {
            refreshProgress();
        }

        public void onFinish() {
        }
    };
}
