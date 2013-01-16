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
import ca.dragonflystudios.atii.model.book.Page.PlaybackState;
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

        public void startPlayback();

        public void pausePlayback();

        public void seekTo(int position);

        public void stopPlayback();

        public void togglePlayMode();

        public void startRecording();

        public void stopRecording();

        public void captureImage(Activity requestingActivity);

        public void pickImage(Activity requestingActivity);

        public void keepNewPageImage();

        public void discardNewPageImage();

        public void addPageBefore();

        public void addPageAfter();

        public void deletePage();
    }

    public interface PlayerState {
        public void initializeState();

        public PlayMode getPlayMode();

        public PlayState getPlayState();

        public int getPageNum();

        public int getNumPages();

        public Page getPage(int pageNum);

        public PlaybackState getPlaybackState();

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
        mNoAudioStatusView = (TextView) mControlsView.findViewById(R.id.no_audio_status);
        mSecondsRecordedView = (TextView) mControlsView.findViewById(R.id.seconds_recorded);
        mPlaybackSeekBar = (SeekBar) mControlsView.findViewById(R.id.replay_seek_bar);
        mPlaybackSeekBar.setOnSeekBarChangeListener(this);

        mPlayButton = (ImageButton) mControlsView.findViewById(R.id.play);
        mPlayButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.startPlayback();
            }
        });

        mPauseButton = (ImageButton) mControlsView.findViewById(R.id.pause);
        mPauseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.pausePlayback();
            }
        });

        mRecordButton = (ImageButton) mControlsView.findViewById(R.id.record);
        mRecordButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPlayManager.startRecording();
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
                mPlayManager.stopRecording();
            }
        });

        mPlayManager.initializeState();
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
    public void onPlayModeChanged(PlayMode oldMode, final PlayMode newMode) {
        runOnUiThread(new Runnable() {
            public void run() {
                switch (newMode) {
                case READER:
                    mModeButton.setSaw(true);
                    setAuthoringControlsVisibility(View.INVISIBLE);
                    break;
                case AUTHOR:
                    mModeButton.setSaw(false);
                    setAuthoringControlsVisibility(View.VISIBLE);
                    break;
                default:
                    break;
                }
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPlayStateChanged(final PlayState oldState, final PlayState newState) {
        runOnUiThread(new Runnable() {
            public void run() {
                switch (oldState) {
                case IDLE:
                case PLAYING_BACK:
                    exitingPlayingback();
                    break;
                case RECORDING:
                    exitingRecording();
                    break;
                case GETTING_IMAGE:
                    exitingGettingImage();
                    break;
                default:
                    break;
                }

                switch (newState) {
                case IDLE:
                case PLAYING_BACK:
                    enteringPlayingback();
                    break;
                case GETTING_IMAGE:
                    enteringGettingImage();
                    break;
                case RECORDING:
                    enteringRecording();
                    break;
                default:
                    break;
                }
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPlaybackStateChanged(PlaybackState oldState, final PlaybackState newState) {
        runOnUiThread(new Runnable() {
            public void run() {
                refreshPlaybackButtons(newState);
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPageChanged(final int newPage) {
        runOnUiThread(new Runnable() {
            public void run() {
                refreshStatusDisplays();
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPageImageChanged(int pageNum) {
        refreshPageImage(pageNum);
    }

    @Override
    // implementation for PlayChangeListener
    public void requestMoveToPage(int newPage) {
        mPager.setCurrentItem(newPage);
    }

    @Override
    // implementation for PlayChangeListener
    public void onPagesEdited(int newPage) {
        refreshPlaybackButtons(mPlayManager.getPlaybackState());
        refreshStatusDisplays();
        refreshPageImage(newPage);
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
        mPlayManager.pausePlayback();
        mPlayingTimer.cancel();
    }

    /**
     * When user stops moving the seek bar
     * */
    @Override
    // implementation for OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        mPlayManager.seekTo(mPlaybackSeekBar.getProgress());
        if (mHasBeenPlaying) {
            mPlayManager.startPlayback();
            mPlayingTimer.start();
        }
    }

    private void enteringPlayingback() {
        mControlsToggleAllowed = true;
        mPager.setPageChangeEnabled(true);
        mRecordButton.setVisibility(View.VISIBLE);
        setPlaybackControlsVisibility(View.VISIBLE);
        refreshPlaybackButtons(mPlayManager.getPlaybackState());
        refreshProgress();
        if (PlayMode.AUTHOR == mPlayManager.getPlayMode())
            setAuthoringControlsVisibility(View.VISIBLE);
        else
            setAuthoringControlsVisibility(View.INVISIBLE);
    }

    private void exitingPlayingback() {
        setPlaybackControlsVisibility(View.INVISIBLE);
    }

    private void enteringRecording() {
        mControlsToggleAllowed = false;
        mPager.setPageChangeEnabled(false);
        mSecondsRecorded = 0;
        mRecordingTimer.start();
        mPager.setPageChangeEnabled(false);
        mRecordButton.setVisibility(View.INVISIBLE);
        mNoAudioStatusView.setVisibility(View.INVISIBLE);
        mSecondsRecordedView.setVisibility(View.VISIBLE);
        mStopButton.setVisibility(View.VISIBLE);
        setAuthoringControlsVisibility(View.INVISIBLE);
    }

    private void exitingRecording() {
        mSecondsRecordedView.setVisibility(View.INVISIBLE);
        mStopButton.setVisibility(View.INVISIBLE);
        mRecordingTimer.cancel();
    }

    private void enteringGettingImage() {
        mControlsToggleAllowed = false;
        mPager.setPageChangeEnabled(false);
        hideAllControls();
    }

    private void exitingGettingImage() {
        showAllControls();
    }

    private void refreshPageImage(int pageNum) {
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(pageNum);

        // Had to use the above because the following line does not work!
        // mAdapter.notifyDataSetChanged();
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

    private void refreshProgress() {
        int duration = mPlayManager.getTrackDuration();
        int progress = mPlayManager.getProgress();

        if (mDuration != duration) {
            if (duration > 0)
                mPlaybackSeekBar.setMax(duration);
            mDuration = duration;
        }

        if (mDuration > 0 && progress >= 0) {
            mPlaybackSeekBar.setVisibility(View.VISIBLE);
            mTrackInfoView.setVisibility(View.VISIBLE);
            mNoAudioStatusView.setVisibility(View.INVISIBLE);

            SimpleDateFormat df = new SimpleDateFormat("mm:ss");
            String trackInfoText = df.format(progress) + " / " + df.format(duration);
            mTrackInfoView.setText(trackInfoText);
            mPlaybackSeekBar.setProgress(progress);
        } else {
            mPlaybackSeekBar.setVisibility(View.INVISIBLE);
            mTrackInfoView.setVisibility(View.INVISIBLE);
            mNoAudioStatusView.setVisibility(View.VISIBLE);
        }
    }

    private void refreshPlaybackButtons(PlaybackState newState) {
        switch (newState) {
        case NO_AUDIO:
            switchPlaybackButton(null);
            break;
        case PLAYING:
            mPlayingTimer.start();
            switchPlaybackButton(mPauseButton);
            break;
        case NOT_STARTED:
        case PAUSED:
        case FINISHED:
            mPlayingTimer.cancel();
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

    private void setPlaybackControlsVisibility(int visibility) {
        mPlayButton.setVisibility(visibility);
        mPauseButton.setVisibility(visibility);
        mPlaybackSeekBar.setVisibility(visibility);
        mTrackInfoView.setVisibility(visibility);
    }

    private void hideAllControls() {
        mControlsView.setVisibility(View.INVISIBLE);
    }

    private void showAllControls() {
        mControlsView.setVisibility(View.VISIBLE);
    }

    private void toggleAllControls() {
        if (View.VISIBLE == mControlsView.getVisibility())
            mControlsView.setVisibility(View.INVISIBLE);
        else
            mControlsView.setVisibility(View.VISIBLE);
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
    private TextView mPageNumView, mTrackInfoView, mNoAudioStatusView, mSecondsRecordedView;
    private SeekBar mPlaybackSeekBar;

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
                mSecondsRecordedView.setText("" + minutes + ":0" + seconds);
            } else {
                mSecondsRecordedView.setText("" + minutes + ":" + seconds);
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
