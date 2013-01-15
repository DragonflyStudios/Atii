package ca.dragonflystudios.atii.play;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.util.Log;
import ca.dragonflystudios.atii.BuildConfig;
import ca.dragonflystudios.atii.model.book.Book;
import ca.dragonflystudios.atii.model.book.Page;
import ca.dragonflystudios.atii.model.book.Page.AudioPlaybackState;
import ca.dragonflystudios.utilities.Streams;

public class PlayManager implements Player.PlayCommandHandler, Player.PlayerState, Player.ImageRequestResultListener,
        MediaPlayer.OnCompletionListener, ViewPager.OnPageChangeListener {

    public interface PlayChangeListener {
        public void onModeChanged(PlayMode oldMode, PlayMode newMode);

        public void onPlayStateChanged(PlayState oldState, PlayState newState);

        // does not differentiate cross- vs. within-page state changes
        public void onAudioPlaybackStateChanged(AudioPlaybackState oldState, AudioPlaybackState newState);

        public void onPageChanged(int newPage);

        public void onPageImageChanged(int pageNum);

        public void requestMoveToPage(int targetPage);

        public void onPagesEdited(int newPage);
    }

    public enum PlayMode {
        READER, AUTHOR
    }

    public enum PlayState {
        IDLE, PLAYING_BACK_AUDIO, RECORDING_AUDIO, GETTING_IMAGE
    }

    @Override
    // implementation for Player.PlayerState
    public PlayMode getPlayMode() {
        return mPlayMode;
    }

    @Override
    // implementation for Player.PlayerState
    public PlayState getPlayState() {
        return mPlayState;
    }

    @Override
    // implementation for Player.PlayerState
    public int getPageNum() {
        return mCurrentPageNum;
    }

    @Override
    // implementation for Player.PlayerState
    public int getNumPages() {
        return mBook.getNumPages();
    }

    @Override
    // implementation for Player.PlayerState
    public Page getPage(int pageNum) {
        return mBook.getPage(pageNum);
    }

    @Override
    // implementation for Player.PlayerState
    public AudioPlaybackState getAudioPlaybackState() {
        if (null == mCurrentPage)
            return AudioPlaybackState.NO_AUDIO;

        return mCurrentPage.getAudioPlaybackState();
    }

    @Override
    // implementation for Player.PlayerState
    public int getTrackDuration() {
        if (!hasAudio())
            return -1;

        if (null == mMediaPlayer) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setDataSource(mCurrentPage.getAudio().getPath());
                mMediaPlayer.prepare();
            } catch (IOException e) {
                Log.e(getClass().getName(), "prepare() failed with IOException");
                e.printStackTrace();
            }
        }

        return mMediaPlayer.getDuration();
    }

    @Override
    // implementation for Player.PlayerState
    public int getProgress() {
        if (!hasAudio())
            return -1;

        if (null == mMediaPlayer) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setDataSource(mCurrentPage.getAudio().getPath());
                mMediaPlayer.prepare();
            } catch (IOException e) {
                Log.e(getClass().getName(), "prepare() failed with IOException");
                e.printStackTrace();
            }
        }

        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    // implementation for Player.PlayerState
    public boolean isAutoReplay() {
        return (mPlayMode == PlayMode.READER) && mAutoReplay;
    }

    @Override
    // implementation for Player.PlayerState
    public void setAutoReplay(boolean auto) {
        mAutoReplay = auto;
    }

    @Override
    // implementation for Player.PlayerState
    public boolean isAutoAdvance() {
        return (mPlayMode == PlayMode.READER) && mAutoAdvance;
    }

    @Override
    // implementation for Player.PlayerState
    public void setAutoAdvance(boolean auto) {
        mAutoAdvance = auto;
    }

    public PlayManager(String bookPath, PlayChangeListener pcl, PlayMode mode) {
        mBook = new Book(new File(bookPath), null);

        mPlayMode = mode;
        mPlayState = PlayState.IDLE;

        mPlayChangeListener = pcl;

        // TODO: add at least one page? handle empty book!!!
        if (mBook.hasPages()) {
            mCurrentPageNum = 0;
            mCurrentPage = mBook.getPage(mCurrentPageNum);
        } else {
            mCurrentPageNum = -1;
            mCurrentPage = null;
        }
    }

    // TODO: ... do we really need this one?
    public int getInitialPage() {
        // could be a persisted value
        return 0;
    }

    public void onResume() {
        if (isAutoReplay())
            startAudioReplay();
    }

    public void onPause() {
        stopAudioReplay();
        stopAudioRecording();

        mBook.save();
    }

    private void setPlayState(PlayState newState) {
        if (mPlayState != newState) {
            PlayState oldState = mPlayState;
            mPlayState = newState;

            if (null != mPlayChangeListener)
                mPlayChangeListener.onPlayStateChanged(oldState, newState);
        }
    }

    private void setCurrentPage(int newPageNum) {
        if (mCurrentPageNum != newPageNum) {
            AudioPlaybackState oldState = getAudioPlaybackState();
            mCurrentPageNum = newPageNum;
            AudioPlaybackState newState = getAudioPlaybackState();
            mCurrentPage = mBook.getPage(newPageNum);

            if (null != mPlayChangeListener)
                mPlayChangeListener.onPageChanged(newPageNum);

            if (oldState != newState && null != mPlayChangeListener)
                mPlayChangeListener.onAudioPlaybackStateChanged(oldState, newState);
        }
    }

    private void setAudioPlaybackState(AudioPlaybackState newState) {
        AudioPlaybackState oldState = getAudioPlaybackState();
        if (oldState != newState) {
            mBook.getPage(mCurrentPageNum).setAudioPlaybackState(newState);
            if (null != mPlayChangeListener)
                mPlayChangeListener.onAudioPlaybackStateChanged(oldState, newState);
        }
    }

    private boolean isReplayNotStarted() {
        return mCurrentPage.getAudioPlaybackState() == AudioPlaybackState.NOT_STARTED;
    }

    public boolean isPlaying() {
        return mCurrentPage.getAudioPlaybackState() == AudioPlaybackState.PLAYING;
    }

    private boolean isPaused() {
        return mCurrentPage.getAudioPlaybackState() == AudioPlaybackState.PAUSED;
    }

    private boolean isFinished() {
        return mCurrentPage.getAudioPlaybackState() == AudioPlaybackState.FINISHED;
    }

    private boolean hasAudio() {
        if (null == mCurrentPage)
            return false;

        return mCurrentPage.hasAudio();
    }

    @Override
    // implementation for PlayCommandHandler
    public void startAudioReplay() {
        if (null == mCurrentPage)
            return;

        if (hasAudio() && (isReplayNotStarted() || isPaused() || isFinished())) {
            if (null == mMediaPlayer) {
                try {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setDataSource(mCurrentPage.getAudio().getPath());
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    Log.e(getClass().getName(), "prepare() failed with IOException");
                    e.printStackTrace();
                }
            }

            mMediaPlayer.start();
            setAudioPlaybackState(AudioPlaybackState.PLAYING);
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void pauseAudioReplay() {
        if (hasAudio() && (isPlaying()))
            if (null != mMediaPlayer) {
                mMediaPlayer.pause();
                setAudioPlaybackState(AudioPlaybackState.PAUSED);
            }
    }

    @Override
    // implementation for PlayCommandHandler
    public void seekTo(int position) {
        if (null != mMediaPlayer)
            mMediaPlayer.seekTo(position);
    }

    @Override
    // implementation for PlayCommandHandler
    public void stopAudioReplay() {
        if (null != mMediaPlayer) {
            mMediaPlayer.release();
            mMediaPlayer = null;

            setAudioPlaybackState(AudioPlaybackState.FINISHED);
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void togglePlayMode() {
        if (mPlayMode != PlayMode.READER)
            switchPlayMode(PlayMode.READER);
        else
            switchPlayMode(PlayMode.AUTHOR);
    }

    private void switchPlayMode(PlayMode newMode) {
        PlayMode oldMode = mPlayMode;

        switch (newMode) {
        case AUTHOR:
            if (PlayMode.READER == mPlayMode) {
                stopAudioReplay();
                mPlayMode = newMode;
                if (null != mPlayChangeListener)
                    mPlayChangeListener.onModeChanged(oldMode, newMode);
            }
            break;
        case READER:
            if (PlayMode.AUTHOR == mPlayMode) {
                mPlayMode = newMode;
                if (null != mPlayChangeListener)
                    mPlayChangeListener.onModeChanged(oldMode, newMode);
            }
        default:
            break;
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void startAudioRecording() {
        stopAudioReplay();

        if (null == mCurrentPage)
            return;

        if (null == mMediaRecorder) {
            try {
                mMediaRecorder = new MediaRecorder();
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mMediaRecorder.setOutputFile(mCurrentPage.getAudioFileForWriting().getPath());
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                mMediaRecorder.prepare();
            } catch (IOException e) {
                Log.e(getClass().getName(), "media recorder prepare() failed");
            }
        }

        setPlayState(PlayState.RECORDING_AUDIO);
        mMediaRecorder.start();
    }

    @Override
    // implementation for PlayCommandHandler
    public void stopAudioRecording() {
        if (mPlayState == PlayState.RECORDING_AUDIO) {
            if (null != mMediaRecorder) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;

                setPlayState(PlayState.IDLE);
                setAudioPlaybackState(AudioPlaybackState.NOT_STARTED);
            }
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void captureImage(Activity requestingActivity) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imageFileUri = Uri.fromFile(mCurrentPage.getImageFileForWriting());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        setPlayState(PlayState.GETTING_IMAGE);
        requestingActivity.startActivityForResult(intent, Player.CAPTURE_IMAGE);
    }

    @Override
    // implementation for PlayCommandHandler
    public void pickImage(Activity requestingActivity) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        setPlayState(PlayState.GETTING_IMAGE);
        requestingActivity.startActivityForResult(Intent.createChooser(intent, "Select File"), Player.PICK_IMAGE);
    }

    @Override
    // implementation for PlayCommandHandler
    public void discardNewPageImage() {
        mCurrentPage.discardNewImage();
        if (null != mPlayChangeListener) {
            mPlayChangeListener.onPageImageChanged(mCurrentPageNum);
        }
        setPlayState(PlayState.IDLE);
    }

    @Override
    // implementation for PlayCommandHandler
    public void keepNewPageImage() {
        mCurrentPage.commitNewImage();
        if (null != mPlayChangeListener) {
            mPlayChangeListener.onPageImageChanged(mCurrentPageNum);
        }
        setPlayState(PlayState.IDLE);
    }

    @Override
    // implementation for PlayCommandHandler
    public void addPageBefore() {
        mBook.addPageAt(mCurrentPageNum);
        mCurrentPage = mBook.getPage(mCurrentPageNum);
        if (null != mPlayChangeListener)
            mPlayChangeListener.onPagesEdited(mCurrentPageNum);
    }

    @Override
    // implementation for PlayCommandHandler
    public void addPageAfter() {
        mCurrentPageNum++;
        mBook.addPageAt(mCurrentPageNum);
        mCurrentPage = mBook.getPage(mCurrentPageNum);
        if (null != mPlayChangeListener)
            mPlayChangeListener.onPagesEdited(mCurrentPageNum);
    }

    @Override
    // implementation for PlayCommandHandler
    public void deletePage() {

        int newPage = mBook.deletePageAt(mCurrentPageNum);

        if (newPage >= 0) {
            if (mBook.hasPages()) {
                mCurrentPageNum = newPage;
                mCurrentPage = mBook.getPage(newPage);
            } else {
                mCurrentPageNum = -1;
                mCurrentPage = null;
            }

            if (null != mPlayChangeListener)
                mPlayChangeListener.onPagesEdited(newPage);
        }
    }

    @Override
    // implementation for ImageRequester
    public void onGettingImageFailure() {
        setPlayState(PlayState.IDLE);
    }

    @Override
    // implementation for ImageRequester
    public void onImageCaptured() {
        if (null != mPlayChangeListener)
            mPlayChangeListener.onPageImageChanged(mCurrentPageNum);
    }

    @Override
    // implementation for ImageRequester
    public void onImagePicked(Uri pickedImage, ContentResolver resolver) {
        try {
            InputStream imageStream = resolver.openInputStream(pickedImage);
            if (setNewPageImage(imageStream) && null != mPlayChangeListener)
                mPlayChangeListener.onPageImageChanged(mCurrentPageNum);
            imageStream.close();
        } catch (FileNotFoundException fnfe) {
            if (BuildConfig.DEBUG) {
                fnfe.printStackTrace();
                throw new RuntimeException(fnfe);
            } else {
                Log.w(getClass().getName(), "selected image file not found: " + pickedImage);
            }
        } catch (IOException ioe) {
            if (BuildConfig.DEBUG) {
                ioe.printStackTrace();
                throw new RuntimeException(ioe);
            } else {
                Log.w(getClass().getName(), "failed to copy page image file");
            }
        }

    }

    private boolean setNewPageImage(InputStream newImageStream) {
        File imageFile = mCurrentPage.getImageFileForWriting();
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imageFile);
        } catch (FileNotFoundException fnfe) {
            if (BuildConfig.DEBUG) {
                fnfe.printStackTrace();
                throw new RuntimeException(fnfe);
            } else {
                Log.w(getClass().getName(), "target page image file not found: " + imageFile);
                return false;
            }
        }

        try {
            Streams.copy(newImageStream, fos);
            fos.close();
        } catch (IOException ioe) {
            if (BuildConfig.DEBUG) {
                ioe.printStackTrace();
                throw new RuntimeException(ioe);
            } else {
                Log.w(getClass().getName(), "failed to copy to target page image file: " + imageFile);
                return false;
            }
        }

        return true;
    }

    @Override
    // implementation for MediaPlayer.OnCompletionListener
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp) {
            stopAudioReplay();

            if (null != mPlayChangeListener && isAutoAdvance() && mCurrentPageNum < getNumPages() - 1)
                mPlayChangeListener.requestMoveToPage(mCurrentPageNum + 1);
        }
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageSelected(int position) {
        if (mCurrentPageNum != position) {
            stopAudioReplay();

            setCurrentPage(position);

            if (isAutoReplay()) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startAudioReplay();
                    }
                }, 1000);
            }
        }
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageScrollStateChanged(int state) {
    }

    private Book mBook;

    private boolean mAutoReplay;
    private boolean mAutoAdvance;

    private int mCurrentPageNum;
    private Page mCurrentPage;

    private PlayMode mPlayMode;
    private PlayState mPlayState;

    private PlayChangeListener mPlayChangeListener;

    private MediaPlayer mMediaPlayer;
    private MediaRecorder mMediaRecorder;
}
