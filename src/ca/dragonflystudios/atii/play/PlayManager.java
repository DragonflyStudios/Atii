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
import ca.dragonflystudios.atii.model.book.Page.PlaybackState;
import ca.dragonflystudios.utilities.Streams;

public class PlayManager implements Player.PlayCommandHandler, Player.PlayerState, Player.ImageRequestResultListener,
        MediaPlayer.OnCompletionListener, ViewPager.OnPageChangeListener {

    public interface PlayChangeListener {
        public void onPlayModeChanged(PlayMode oldMode, PlayMode newMode);

        public void onPlayStateChanged(PlayState oldState, PlayState newState);

        public void onPlaybackStateChanged(PlaybackState oldState, PlaybackState newState);

        public void onPageChanged(int newPage);

        public void onPageImageChanged(int pageNum);

        public void requestMoveToPage(int targetPage);

        public void onPagesEdited(int newPage);
    }

    public enum PlayMode {
        INVALID, READER, AUTHOR
    }

    public enum PlayState {
        INVALID, IDLE, PLAYING_BACK, RECORDING, GETTING_IMAGE
    }

    @Override
    // implementation for Player.PlayerState
    public void initializeState() {
        if (null != mPlayChangeListener) {
            int mCurrentPageNum = getInitialPageNum();
            mCurrentPage = mBook.getPage(mCurrentPageNum);
            mPlayChangeListener.onPlayModeChanged(PlayMode.INVALID, mPlayMode);
            
            mPlayChangeListener.requestMoveToPage(mCurrentPageNum);
            // the following call is a hack to address the issue with ViewPager's not calling onPageSelected()
            // upon first setCurrentItem(): http://code.google.com/p/android/issues/detail?id=27526
            mPlayChangeListener.onPageChanged(mCurrentPageNum);

            mPlayChangeListener.onPlayStateChanged(PlayState.INVALID, mPlayState);
            mPlayChangeListener.onPlaybackStateChanged(PlaybackState.INVALID, getPlaybackState());
        }
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
    public PlaybackState getPlaybackState() {
        if (null == mCurrentPage)
            return PlaybackState.NO_AUDIO;

        return mCurrentPage.getPlaybackState();
    }

    @Override
    // implementation for Player.PlayerState
    public int getTrackDuration() {
        if (!hasAudio())
            return -1;

        if (null == mMediaPlayer)
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnCompletionListener(this);
                if (mCurrentPage != null)
                    Log.w("getTrackDuration", "mCurrentPage is not null");
                if (mCurrentPage.getAudio() != null)
                    Log.w("getTrackDuration", "mCurrentPage.getAudio() is not null");
                if (mCurrentPage.getAudio().getAbsolutePath() != null)
                    Log.w("getTrackDuration", "mCurrentPage.getAudio().getAbsolutePath() is not null");
                mMediaPlayer.setDataSource(mCurrentPage.getAudio().getAbsolutePath());
                mMediaPlayer.prepare();
                Log.w("getTrackDuration", "Media player prepare called");
            } catch (IOException e) {
                Log.e(getClass().getName(), "prepare() failed with IOException");
                e.printStackTrace();
            }

        return mMediaPlayer.getDuration();
    }

    @Override
    // implementation for Player.PlayerState
    public int getProgress() {
        if (!hasAudio())
            return -1;

        if (null == mMediaPlayer || isPlaybackNotStarted())
            return 0;

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

        if (mBook.hasPages()) {
            mCurrentPageNum = getInitialPageNum();
            mCurrentPage = mBook.getPage(mCurrentPageNum);
        } else {
            // TODO: error handling? or just add an empty page?
            mCurrentPageNum = -1;
            mCurrentPage = null;
        }
    }

    private int getInitialPageNum() {
        // could be a persisted value
        return 0;
    }

    public void onResume() {
        if (isAutoReplay())
            startPlayback();
    }

    public void onPause() {
        stopPlayback();
        stopRecording();

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

    private void switchToPage(int newPageNum) {
        stopPlayback();

        if (mCurrentPageNum != newPageNum) {
            mCurrentPageNum = newPageNum;
            mCurrentPage = mBook.getPage(newPageNum);

            if (null != mPlayChangeListener) {
                mPlayChangeListener.onPageChanged(newPageNum);
                mPlayChangeListener.onPlaybackStateChanged(PlaybackState.INVALID, getPlaybackState());
            }
        }
    }

    private void setPlaybackState(PlaybackState newState) {
        PlaybackState oldState = getPlaybackState();
        if (oldState != newState) {
            mCurrentPage.setPlaybackState(newState);
            if (null != mPlayChangeListener) {
                mPlayChangeListener.onPlaybackStateChanged(oldState, newState);

                if (isPlaybackNotStarted() || !hasAudio())
                    setPlayState(PlayState.IDLE);
            }
        }
    }

    private boolean isPlaybackNotStarted() {
        return mCurrentPage.getPlaybackState() == PlaybackState.NOT_STARTED;
    }

    public boolean isPlaying() {
        return mCurrentPage.getPlaybackState() == PlaybackState.PLAYING;
    }

    private boolean isPaused() {
        return mCurrentPage.getPlaybackState() == PlaybackState.PAUSED;
    }

    private boolean isFinished() {
        return mCurrentPage.getPlaybackState() == PlaybackState.FINISHED;
    }

    private boolean hasAudio() {
        if (null == mCurrentPage)
            return false;

        return mCurrentPage.hasAudio();
    }

    @Override
    // implementation for PlayCommandHandler
    public void startPlayback() {
        if (null == mCurrentPage)
            return;

        if (hasAudio() && (isPlaybackNotStarted() || isPaused() || isFinished())) {
            if (null == mMediaPlayer) {
                try {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setDataSource(mCurrentPage.getAudio().getAbsolutePath());
                    mMediaPlayer.prepare();
                    Log.w("startAudioReplay", "Media player prepare called");
                } catch (IOException e) {
                    Log.e(getClass().getName(), "prepare() failed with IOException");
                    e.printStackTrace();
                }
            }

            mMediaPlayer.start();
            Log.w("startAudioReplay", "Media player start called");
            setPlaybackState(PlaybackState.PLAYING);
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void pausePlayback() {
        if (hasAudio() && (isPlaying()))
            if (null != mMediaPlayer) {
                mMediaPlayer.pause();
                setPlaybackState(PlaybackState.PAUSED);
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
    public void stopPlayback() {
        if (null != mMediaPlayer) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            setPlaybackState(PlaybackState.NOT_STARTED);
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void togglePlayMode() {
        if (mPlayMode != PlayMode.READER)
            setPlayMode(PlayMode.READER);
        else
            setPlayMode(PlayMode.AUTHOR);
    }

    private void setPlayMode(PlayMode newMode) {
        PlayMode oldMode = mPlayMode;

        switch (newMode) {
        case AUTHOR:
            if (PlayMode.READER == mPlayMode) {
                stopPlayback();
                mPlayMode = newMode;
                if (null != mPlayChangeListener)
                    mPlayChangeListener.onPlayModeChanged(oldMode, newMode);
            }
            break;
        case READER:
            if (PlayMode.AUTHOR == mPlayMode) {
                mPlayMode = newMode;
                if (null != mPlayChangeListener)
                    mPlayChangeListener.onPlayModeChanged(oldMode, newMode);
            }
        default:
            break;
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void startRecording() {
        stopPlayback();

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

        setPlayState(PlayState.RECORDING);
        mMediaRecorder.start();
    }

    @Override
    // implementation for PlayCommandHandler
    public void stopRecording() {
        if (mPlayState == PlayState.RECORDING) {
            if (null != mMediaRecorder) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;

                mCurrentPage.audioUpdated();
                setPlayState(PlayState.IDLE);
            }
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void captureImage(Activity requestingActivity) {
        stopPlayback();

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
        stopPlayback();

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
        stopPlayback();

        mBook.addPageAt(mCurrentPageNum);
        mCurrentPage = mBook.getPage(mCurrentPageNum);
        if (null != mPlayChangeListener)
            mPlayChangeListener.onPagesEdited(mCurrentPageNum);
    }

    @Override
    // implementation for PlayCommandHandler
    public void addPageAfter() {
        stopPlayback();

        mCurrentPageNum++;
        mBook.addPageAt(mCurrentPageNum);
        mCurrentPage = mBook.getPage(mCurrentPageNum);
        if (null != mPlayChangeListener)
            mPlayChangeListener.onPagesEdited(mCurrentPageNum);
    }

    @Override
    // implementation for PlayCommandHandler
    public void deletePage() {
        stopPlayback();

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
            stopPlayback();

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
        Log.w("onPageSelected", "mCurrentPageNum = " + mCurrentPageNum + "    position = " + position);
        if (mCurrentPageNum != position) {
            switchToPage(position);

            if (isAutoReplay()) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startPlayback();
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
