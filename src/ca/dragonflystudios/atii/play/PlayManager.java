package ca.dragonflystudios.atii.play;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewGroup;
import ca.dragonflystudios.android.media.camera.PhotoSnapper;
import ca.dragonflystudios.atii.model.book.Book;
import ca.dragonflystudios.atii.model.book.Page;
import ca.dragonflystudios.atii.model.book.Page.AudioPlaybackState;

public class PlayManager implements Player.PlayCommandHandler, MediaPlayer.OnCompletionListener, ViewPager.OnPageChangeListener,
        PhotoSnapper.OnCompletionListener
{

    // TODO: auto (page) advance ...

    public interface PlayChangeListener
    {
        public void onModeChanged(PlayMode newMode);

        public void onPlayStateChanged(PlayState newState);

        // does not differentiate cross- vs. within-page state changes
        public void onAudioPlaybackStateChanged(AudioPlaybackState newState);

        public void onPageChanged(int newPage);

        public void onPageImageChanged(int pageNum);

        // this is a hack that breaks the integrity of "PlayChangeListener".
        // TAI!
        public void requestPageChange(int newPage);

        public void requestPageChangeNotify(int newPage);
    }

    public enum PlayMode
    {
        READER, AUTHOR
    }

    public enum PlayState
    {
        IDLE, PLAYING_BACK_AUDIO, RECORDING_AUDIO, CAPTURING_PHOTO
    }

    public PlayMode getPlayMode() {
        return mPlayMode;
    }

    public PlayState getPlayState() {
        return mPlayState;
    }

    private void setPlayState(PlayState newState) {
        if (mPlayState != newState) {
            mPlayState = newState;

            if (null != mPlayChangeListener)
                mPlayChangeListener.onPlayStateChanged(newState);
        }
    }

    public AudioPlaybackState getAudioPlaybackState() {
        if (null == mCurrentPage)
            return AudioPlaybackState.NO_AUDIO;

        return mCurrentPage.getAudioPlaybackState();
    }

    public PlayManager(String bookPath, PlayChangeListener pcl, PlayMode mode)
    {
        mBook = new Book(new File(bookPath), null);

        mPlayMode = mode;
        mPlayState = PlayState.IDLE;

        mPlayChangeListener = pcl;

        if (mBook.hasPages()) {
            mCurrentPageNum = 0;
            mCurrentPage = mBook.getPage(mCurrentPageNum);
        } else {
            mCurrentPageNum = -1;
            mCurrentPage = null;
        }
    }

    public String getImagePathForPage(int pageNum) {
        File imageFile = mBook.getPage(pageNum).getImage();

        if (null == imageFile)
            return null;

        return imageFile.getAbsolutePath();
    }

    public Page getPage(int pageNum) {
        return mBook.getPage(pageNum);
    }

    public int getInitialPage() {
        // could be a persisted value
        return 0;
    }

    public boolean isAutoReplay() {
        return (mPlayMode == PlayMode.READER) && mAutoReplay;
    }

    public void setAutoReplay(boolean auto) {
        mAutoReplay = auto;
    }

    public boolean isAutoAdvance() {
        return (mPlayMode == PlayMode.READER) && mAutoAdvance;
    }

    public void setAutoAdvance(boolean auto) {
        mAutoAdvance = auto;
    }

    public int getNumPages() {
        return mBook.getNumPages();
    }

    public boolean hasAudioOnCurrentPage() {
        return mCurrentPage.hasAudio();
    }

    public int getCurrentPageNum() {
        return mCurrentPageNum;
    }

    private void setCurrentPage(int newPageNum) {
        if (mCurrentPageNum != newPageNum) {
            AudioPlaybackState oldState = getAudioPlaybackState(mCurrentPageNum);
            AudioPlaybackState newState = getAudioPlaybackState(newPageNum);
            mCurrentPageNum = newPageNum;
            mCurrentPage = mBook.getPage(newPageNum);

            if (null != mPlayChangeListener)
                mPlayChangeListener.onPageChanged(newPageNum);

            if (oldState != newState && null != mPlayChangeListener)
                mPlayChangeListener.onAudioPlaybackStateChanged(newState);
        }
    }

    public AudioPlaybackState getAudioPlaybackState(int pageNum) {
        return mBook.getPage(pageNum).getAudioPlaybackState();
    }

    public void setAudioPlaybackState(int pageNum, AudioPlaybackState state) {
        AudioPlaybackState oldState = getAudioPlaybackState(pageNum);
        if (oldState != state) {
            mBook.getPage(pageNum).setAudioPlaybackState(state);
            if (mCurrentPageNum == pageNum && null != mPlayChangeListener)
                mPlayChangeListener.onAudioPlaybackStateChanged(state);
        }
    }

    private boolean isReplayNotStarted() {
        return mCurrentPage.getAudioPlaybackState() == AudioPlaybackState.NOT_STARTED;
    }

    private boolean isPlaying() {
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
            setAudioPlaybackState(mCurrentPageNum, AudioPlaybackState.PLAYING);
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void pauseAudioReplay() {
        if (hasAudio() && (isPlaying()))
            if (null != mMediaPlayer) {
                mMediaPlayer.pause();
                setAudioPlaybackState(mCurrentPageNum, AudioPlaybackState.PAUSED);
            }
    }

    @Override
    // implementation for PlayCommandHandler
    public void stopAudioReplay() {
        if (hasAudio() && (isPlaying() || isPaused()))
            if (null != mMediaPlayer) {
                mMediaPlayer.release();
                mMediaPlayer = null;

                setAudioPlaybackState(mCurrentPageNum, AudioPlaybackState.FINISHED);
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

    public void switchPlayMode(PlayMode newMode) {
        switch (newMode) {
        case AUTHOR:
            if (PlayMode.READER == mPlayMode) {
                stopAudioReplay();
                mPlayMode = newMode;
                if (null != mPlayChangeListener)
                    mPlayChangeListener.onModeChanged(newMode);
            }
            break;
        case READER:
            if (PlayMode.AUTHOR == mPlayMode) {
                mPlayMode = newMode;
                if (null != mPlayChangeListener)
                    mPlayChangeListener.onModeChanged(newMode);
            }
        default:
            break;
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void startAudioRecording() {
        stopAudioReplay();

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
                setAudioPlaybackState(mCurrentPageNum, AudioPlaybackState.NOT_STARTED);
            }
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void capturePhoto(ViewGroup hostView) {
        new PhotoSnapper(hostView, mCurrentPage.getImagePath(), this);
    }

    @Override
    // implementation for PlayCommandHandler
    public void capturePhoto(Activity requestingActivity) {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        Uri imageFileUri = Uri.fromFile(mCurrentPage.getImageFileForWriting());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        requestingActivity.startActivityForResult(intent, Player.CAPTURE_PHOTO);
    }

    public void discardNewPageImage() {
        mCurrentPage.discardNewImage();
    }

    public void keepNewPageImage() {
        mCurrentPage.commitNewImage();
    }

    @Override
    // implementation for PlayCommandHandler
    public void stopPhotoCapture() {

    }

    @Override
    // implementation for PlayCommandHandler
    public void addPageBefore() {
        int newPage = mCurrentPageNum;

        mBook.addPageAt(newPage);
        if (null != mPlayChangeListener)
            mPlayChangeListener.requestPageChangeNotify(newPage);
    }

    @Override
    // implementation for PlayCommandHandler
    public void addPageAfter() {
        int newPage = mCurrentPageNum + 1;

        mBook.addPageAt(newPage);
        if (null != mPlayChangeListener)
            mPlayChangeListener.requestPageChangeNotify(newPage);
    }

    @Override
    // implementation for PlayCommandHandler
    public void deletePage() {
        int newPage = mBook.deletePageAt(mCurrentPageNum);
        if (null != mPlayChangeListener && 0 <= newPage)
            mPlayChangeListener.requestPageChangeNotify(newPage);
    }

    @Override
    // implementation for MediaPlayer.OnCompletionListener
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp) {
            stopAudioReplay();

            if (null != mPlayChangeListener && isAutoAdvance() && mCurrentPageNum < getNumPages() - 1)
                mPlayChangeListener.requestPageChange(mCurrentPageNum + 1);
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

    @Override
    // implementation for PhotoSnapper.OnCompletionListener
    public void onPhotoSnapperCompletion(File photoFile, boolean success) {
        if (success && null != mPlayChangeListener) {
            mPlayChangeListener.onPageImageChanged(mCurrentPageNum);
        }
    }

    public void saveBook() {
        mBook.save();
    }

    private Book               mBook;

    private boolean            mAutoReplay;
    private boolean            mAutoAdvance;

    private int                mCurrentPageNum;
    private Page               mCurrentPage;

    private PlayMode           mPlayMode;
    private PlayState          mPlayState;

    private PlayChangeListener mPlayChangeListener;

    private MediaPlayer        mMediaPlayer;
    private MediaRecorder      mMediaRecorder;
}
