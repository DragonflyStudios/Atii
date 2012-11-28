package ca.dragonflystudios.atii.play;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewGroup;
import ca.dragonflystudios.android.media.camera.PhotoSnapper;
import ca.dragonflystudios.atii.play.Page.AudioPlaybackState;
import ca.dragonflystudios.utilities.Pathname;
import ca.dragonflystudios.utilities.Pathname.FileNameComparator;

public class PlayManager implements Player.PlayCommandHandler, MediaPlayer.OnCompletionListener, ViewPager.OnPageChangeListener,
        PhotoSnapper.OnCompletionListener {

    // TODO: auto (page) advance ...

    public interface PlayChangeListener {
        public void onModeChanged(PlayMode newMode);

        public void onPlayStateChanged(PlayState newState);

        // does not differentiate cross- vs. within-page state changes
        public void onAudioPlaybackStateChanged(AudioPlaybackState newState);

        public void onPageChanged(int newPage);

        public void onPageImageChanged(int pageNum);
    }

    public enum PlayMode {
        INVALID, PLAYBACK, PLAYOUT
    }

    public enum PlayState {
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
        return mCurrentPage.getAudioPlaybackState();
    }

    public PlayManager(String storyPath, PlayChangeListener pcl) {

        File storyDir = new File(storyPath);
        mStoryPath = storyDir.getAbsolutePath();
        mStoryTitle = Pathname.extractStem(storyDir.getName());

        mPages = listPages(storyDir);
        mNumPages = mPages.size();

        mPlayMode = PlayMode.PLAYBACK;
        mPlayState = PlayState.IDLE;

        mPlayChangeListener = pcl;

        mCurrentPageNum = 0;
        mCurrentPage = mPages.get(mCurrentPageNum);
    }

    public static String getDefaultPageImagePath() {
        return (new File(Environment.getExternalStorageDirectory(), "Atii/defaults/default_page_image.jpg")).getAbsolutePath();
    }

    public String getImagePathForPage(int pageNum) {
        return mPages.get(pageNum).getImage().getAbsolutePath();
    }

    public String getStoryPath() {
        return mStoryPath;
    }

    public String getStoryTitle() {
        return mStoryTitle;
    }

    public int getInitialPage() {
        // could be a persisted value
        return 0;
    }

    public boolean isAutoReplay() {
        return mAutoReplay && (mPlayMode == PlayMode.PLAYBACK);
    }

    public void setAutoReplay(boolean auto) {
        mAutoReplay = auto;
    }

    public int getNumPages() {
        return mNumPages;
    }

    public void setNumPages(int numPages) {
        mNumPages = numPages;
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
            mCurrentPage = mPages.get(newPageNum);

            if (null != mPlayChangeListener)
                mPlayChangeListener.onPageChanged(newPageNum);

            if (oldState != newState && null != mPlayChangeListener)
                mPlayChangeListener.onAudioPlaybackStateChanged(newState);
        }
    }

    public AudioPlaybackState getAudioPlaybackState(int pageNum) {
        return mPages.get(pageNum).getAudioPlaybackState();
    }

    public void setAudioPlaybackState(int pageNum, AudioPlaybackState state) {
        AudioPlaybackState oldState = getAudioPlaybackState(pageNum);
        if (oldState != state) {
            mPages.get(pageNum).setAudioPlaybackState(state);
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
        if (mPlayMode != PlayMode.PLAYBACK)
            switchPlayMode(PlayMode.PLAYBACK);
        else
            switchPlayMode(PlayMode.PLAYOUT);
    }

    public void switchPlayMode(PlayMode newMode) {
        switch (newMode) {
        case PLAYOUT:
            if (PlayMode.PLAYBACK == mPlayMode) {
                stopAudioReplay();
                mPlayMode = newMode;
                if (null != mPlayChangeListener)
                    mPlayChangeListener.onModeChanged(newMode);
            }
            break;
        case PLAYBACK:
            if (PlayMode.PLAYOUT == mPlayMode) {
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
                mMediaRecorder.setOutputFile(mCurrentPage.getAudio().getPath());
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
    public void stopPhotoCapture() {

    }

    @Override
    // implementation for PlayCommandHandler
    public void addPageBefore() {
    }

    @Override
    // implementation for PlayCommandHandler
    public void addPageAfter() {
    }

    @Override
    // implementation for PlayCommandHandler
    public void deletePage() {
    }

    @Override
    // implementation for MediaPlayer.OnCompletionListener
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp)
            stopAudioReplay();
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

            if (isAutoReplay())
                startAudioReplay();
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

    private ArrayList<Page> listPages(File storyDir) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File path) {
                String name = path.getName();
                return path.exists() && !path.isDirectory() && "jpg".equalsIgnoreCase(Pathname.extractExtension(name))
                        && !"front".equals(Pathname.extractStem(name)) && !"back".equals(Pathname.extractStem(name));
            }
        };

        File[] pageList = storyDir.listFiles(filter);
        ArrayList<File> pageFiles = new ArrayList<File>();

        if (pageList != null) {
            pageFiles.addAll(Arrays.asList(pageList));
            Collections.sort(pageFiles, new FileNameComparator());
        }

        File frontCover = new File(storyDir, "front.jpg");
        if (frontCover.exists())
            pageFiles.add(0, frontCover);

        File backCover = new File(storyDir, "back.jpg");
        if (backCover.exists())
            pageFiles.add(backCover);

        ArrayList<Page> pages = new ArrayList<Page>(pageFiles.size());
        for (File pageFile : pageFiles)
            pages.add(new Page(pageFile));

        return pages;
    }

    private String mStoryPath;
    private String mStoryTitle;

    private ArrayList<Page> mPages;
    private int mNumPages;

    private boolean mAutoReplay;

    private int mCurrentPageNum;
    private Page mCurrentPage;

    private PlayMode mPlayMode;
    private PlayState mPlayState;

    private PlayChangeListener mPlayChangeListener;

    private MediaPlayer mMediaPlayer;
    private MediaRecorder mMediaRecorder;
}
