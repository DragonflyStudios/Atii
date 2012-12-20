package ca.dragonflystudios.atii.play;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

    protected final static int CAPTURE_PHOTO = 0;
    protected final static int PICK_PHOTO = 1;

    public interface PlayCommandHandler {

        public void startAudioReplay();

        public void pauseAudioReplay();

        public void stopAudioReplay();

        public void togglePlayMode();

        public void startAudioRecording();

        public void stopAudioRecording();

        public void capturePhoto(ViewGroup hostView);

        public void capturePhoto(Activity requestingActivity);

        public void pickPhoto(Activity requestingActivity);

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

        mPickPictureButton = (ImageButton) mControlsView.findViewById(R.id.pick_picture);
        mPickPictureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPager.setPageChangeEnabled(false);
                hideAllControls();
                mPlayManager.pickPhoto(Player.this);
            }
        });

        mCaptureButton = (ImageButton) mControlsView.findViewById(R.id.capture);
        mCaptureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPager.setPageChangeEnabled(false);
                hideAllControls();
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
                DialogFragment dialog = WarningDialogFragment.newInstance(R.string.page_deletion_dialog_title,
                        R.string.deletion_no_undo_warning);
                dialog.show(getFragmentManager(), "PageDeletionDialogFragment");
            }
        });

        mPageNumView = (TextView) mControlsView.findViewById(R.id.page_num);
        updateProgressForPage(mPlayManager.getInitialPage());

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

        mPlayManager.saveBook();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case CAPTURE_PHOTO:
            if (resultCode == RESULT_OK) {
                onPageImageChanged(mPlayManager.getCurrentPageNum());
            } else {
                showAllControls();
                mPager.setPageChangeEnabled(true);
            }
            break;
        case PICK_PHOTO:
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                try {
                    InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                    if (mPlayManager.setNewPageImage(imageStream))
                        onPageImageChanged(mPlayManager.getCurrentPageNum());
                } catch (FileNotFoundException fnfe) {
                    if (BuildConfig.DEBUG) {
                        fnfe.printStackTrace();
                        throw new RuntimeException(fnfe);
                    } else {
                        Log.w(getClass().getName(), "selected image file not found: " + selectedImage);
                    }
                }
            } else {
                showAllControls();
                mPager.setPageChangeEnabled(true);
            }
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
    public void onAudioPlaybackStateChanged(AudioPlaybackState newState) {
        runOnUiThread(new Runnable() {
            public void run() {
                updateControls();
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPageChanged(final int newPage) {
        runOnUiThread(new Runnable() {
            public void run() {
                updatePageNumView(newPage);
                updateProgressForPage(newPage);
            }
        });
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
        runOnUiThread(new Runnable() {
            public void run() {
                updateControls();
            }
        });
    }

    @Override
    // implementation for PlayChangeListener
    public void onPlayStateChanged(PlayState newState) {
        runOnUiThread(new Runnable() {
            public void run() {
                updateControls();
            }
        });
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
    public void updateProgress(int progress, int duration) {
        if (mDuration != duration) {
            mDuration = duration;
            mReplaySeekBar.setMax(duration);
        }
        SimpleDateFormat df = new SimpleDateFormat("mm:ss");
        String trackInfoText = df.format(progress) + " / " + df.format(duration);
        mTrackInfoView.setText(trackInfoText);
        mReplaySeekBar.setProgress(progress);
    }

    private void updateProgressForPage(int pageNum) {
        updateProgress(mPlayManager.getCurrentProgress(), mPlayManager.getTrackDuration(pageNum));
    }

    @Override
    // implementation for PlayChangeListener
    public void requestPageChange(int newPage) {
        mPager.setCurrentItem(newPage);
    }

    @Override
    // implementation for PlayChangeListener
    public void requestPageChangeNotify(int newPage) {
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(newPage);
        updatePageNumView(newPage);
        updateProgressForPage(newPage);
    }

    @Override
    // implementation for DeleteDialogListener
    public void onPositive() {
        mPlayManager.deletePage();
    }

    @Override
    // implementation for DeleteDialogListener
    public void onNegative() {
    }

    @Override
    // implementation for OnPageImageChoice
    public void onDiscard() {
        int pageNum = mPlayManager.getCurrentPageNum();
        mPlayManager.discardNewPageImage();
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(pageNum);
        showAllControls();
        mPager.setPageChangeEnabled(true);
    }

    @Override
    // implementation for OnPageImageChoice
    public void onKeep() {
        int pageNum = mPlayManager.getCurrentPageNum();
        mPlayManager.keepNewPageImage();
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(pageNum);
        showAllControls();
        mPager.setPageChangeEnabled(true);
    }

    @Override
    // implementation for OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    }
 
    @Override
    // implementation for OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        mPlayManager.stopUpdateProgressBar();
    }
 
    /**
     * When user stops moving the progress hanlder
     * */
    @Override
    // implementation for OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        mPlayManager.stopUpdateProgressBar();
        mPlayManager.seekTo(mReplaySeekBar.getProgress());
        mPlayManager.updateProgressBar();
    }
    
    private void updateControls() {
        mPager.setPageChangeEnabled(true);

        switch (mPlayManager.getPlayMode()) {
        case READER:
            mModeButton.setSaw(true);
            setAuthoringControlsVisibility(View.INVISIBLE);
            mStopButton.setVisibility(View.INVISIBLE);
            switch (mPlayManager.getPlayState()) {
            case IDLE:
            case PLAYING_BACK_AUDIO:
                updateAudioPlaybackButtons();
                break;
            case RECORDING_AUDIO:
                mPager.setPageChangeEnabled(false);
                setAuthoringControlsVisibility(View.INVISIBLE);
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
                setAuthoringControlsVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.INVISIBLE);
                updateAudioPlaybackButtons();
                break;
            case RECORDING_AUDIO:
                mPager.setPageChangeEnabled(false);
                setAuthoringControlsVisibility(View.INVISIBLE);
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

    private void setAuthoringControlsVisibility(int visibility) {
        mPickPictureButton.setVisibility(visibility);
        mCaptureButton.setVisibility(visibility);
        mAddBeforeButton.setVisibility(visibility);
        mAddAfterButton.setVisibility(visibility);
        mDeleteButton.setVisibility(visibility);
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
    private ImageButton mPlayButton, mPauseButton, mRepeatButton;
    private ImageButton mRecordButton, mStopButton;
    private ImageButton mCaptureButton, mPickPictureButton;
    private ImageButton mAddBeforeButton, mAddAfterButton, mDeleteButton;
    private TextView mPageNumView, mTrackInfoView;
    private SeekBar mReplaySeekBar;

    private boolean mControlsToggleAllowed;

    private int mDuration = -1;
}
