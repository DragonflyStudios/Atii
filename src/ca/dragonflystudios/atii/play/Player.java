package ca.dragonflystudios.atii.play;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import ca.dragonflystudios.android.media.CameraPreview;
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

    public interface PlayCommandHandler {

        public void startAudioReplay();

        public void pauseAudioReplay();

        public void stopAudioReplay();

        public void togglePlayMode();

        public void startAudioRecording();

        public void stopAudioRecording();

        public void capturePhoto();

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

        mAdapter = new AtiiPagerAdapter(getSupportFragmentManager(), mPlayManager);

        setContentView(R.layout.player);
        mPlayerMainView = (ViewGroup) findViewById(R.id.player_main);

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
                mPlayManager.stopAudioRecording();
                // Create an instance of Camera
                mCamera = getCameraInstance();

                // Create our Preview view and set it as the content of our
                // activity.
                mPreview = new CameraPreview(Player.this, mCamera);
                mPlayerMainView.addView(mPreview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                mSnapButton = new SnapButton(Player.this);
                mPlayerMainView.addView(mSnapButton, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
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

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
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

    // TODO: refactor!
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    class SnapButton extends Button {
        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
                mPlayerMainView.removeView(mPreview);
                mPlayerMainView.removeView(mSnapButton);
                mPreview = null;
                mSnapButton = null;
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        };

        public SnapButton(Context ctx) {
            super(ctx);
            setText("!!!");
            setOnClickListener(clicker);
        }
    }

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d("Talkie", "Error creating media file. Check storage permissions.");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("Talkie", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("Talkie", "Error accessing file: " + e.getMessage());
            }
        }
    };

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Talkie");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private PlayManager mPlayManager;
    private AtiiPagerAdapter mAdapter;
    private AtiiPager mPager;
    private ViewGroup mPlayerMainView;

    private ViewGroup mControlsView;
    private SeesawButton mModeButton;
    private ImageButton mPlayButton, mPauseButton, mRepeatButton;
    private ImageButton mRecordButton, mStopButton;
    private ImageButton mCaptureButton;
    private ImageButton mAddBeforeButton, mAddAfterButton, mDeleteButton;
    private TextView mPageNumView;

    private boolean mControlsToggleAllowed;

    private SnapButton mSnapButton = null;
    private Camera mCamera = null;
    private CameraPreview mPreview = null;

}
