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
import ca.dragonflystudios.atii.play.Page.ReplayState;
import ca.dragonflystudios.utilities.Pathname;
import ca.dragonflystudios.utilities.Pathname.FileNameComparator;

public class PlayManager implements Player.PlayCommandHandler, MediaPlayer.OnCompletionListener, ViewPager.OnPageChangeListener {

    // TODO: auto (page) advance ...

    public interface OnModeChangeListener {
        public void onModeChanged(PlayerMode newMode);
    }

    public interface OnReplayChangeListener {
        // does not differentiate cross- vs. within-page state changes
        public void onReplayStateChanged(ReplayState newState);
    }

    public interface OnPageChangeListener {
        public void onPageChanged(int newPage);
    }

    public enum PlayerMode {
        INVALID, PLAYBACK, PLAYOUT, RECORD, CAPTURE
    }

    public PlayManager(String storyPath, OnModeChangeListener mcl, OnReplayChangeListener rcl, OnPageChangeListener pcl) {

        File storyDir = new File(storyPath);
        mStoryPath = storyDir.getAbsolutePath();
        mStoryTitle = Pathname.extractStem(storyDir.getName());

        mPages = listPages(storyDir);
        mNumPages = mPages.size();

        mCurrentMode = PlayerMode.PLAYBACK;
        mOnModeChangeListener = mcl;
        mOnReplayChangeListener = rcl;
        mOnPageChangeListener = pcl;

        setCurrentPageNum(getInitialPage());
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
        return mAutoReplay && (mCurrentMode == PlayerMode.PLAYBACK);
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
        return mPages.get(mCurrentPageNum).hasAudio();
    }

    public int getCurrentPageNum() {
        return mCurrentPageNum;
    }

    public void setCurrentPageNum(int newPage) {
        if (mCurrentPageNum != newPage) {
            ReplayState oldState = (mCurrentPageNum >= 0) ? getState(mCurrentPageNum) : ReplayState.INVALID;
            ReplayState newState = getState(newPage);
            mCurrentPageNum = newPage;

            if (null != mOnPageChangeListener)
                mOnPageChangeListener.onPageChanged(newPage);

            if (oldState != newState && null != mOnReplayChangeListener)
                mOnReplayChangeListener.onReplayStateChanged(newState);
        }
    }

    public ReplayState getState(int pageNum) {
        return mPages.get(pageNum).state;
    }

    public void setState(int pageNum, ReplayState state) {
        ReplayState oldState = getState(pageNum);
        if (oldState != state) {
            mPages.get(pageNum).state = state;
            if (mCurrentPageNum == pageNum && null != mOnReplayChangeListener)
                mOnReplayChangeListener.onReplayStateChanged(state);
        }
    }

    public boolean isReplayNotStarted(int pageNum) {
        return getState(pageNum) == ReplayState.NOT_STARTED;
    }

    public boolean isPlaying(int pageNum) {
        return getState(pageNum) == ReplayState.PLAYING;
    }

    public boolean isPaused(int pageNum) {
        return getState(pageNum) == ReplayState.PAUSED;
    }

    public boolean isFinished(int pageNum) {
        return getState(pageNum) == ReplayState.FINISHED;
    }

    public boolean isRecording(int pageNum) {
        return getState(pageNum) == ReplayState.RECORDING;
    }

    public boolean hasAudio(int pageNum) {
        return mPages.get(pageNum).hasAudio();
    }

    @Override
    // implementation for PlayCommandHandler
    public void startAudioReplay() {
        if (hasAudio(mCurrentPageNum)
                && (isReplayNotStarted(mCurrentPageNum) || isPaused(mCurrentPageNum) || isFinished(mCurrentPageNum))) {
            if (null == mMediaPlayer) {
                try {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setDataSource(mPages.get(mCurrentPageNum).getAudio().getPath());
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    Log.e(getClass().getName(), "prepare() failed with IOException");
                    e.printStackTrace();
                }
            }

            mMediaPlayer.start();
            setState(mCurrentPageNum, ReplayState.PLAYING);
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void pauseAudioReplay() {
        if (hasAudio(mCurrentPageNum) && (isPlaying(mCurrentPageNum)))
            if (null != mMediaPlayer) {
                mMediaPlayer.pause();
                setState(mCurrentPageNum, ReplayState.PAUSED);
            }
    }

    @Override
    // implementation for PlayCommandHandler
    public void stopAudioReplay() {
        if (hasAudio(mCurrentPageNum) && isPlaying(mCurrentPageNum))
            if (null != mMediaPlayer) {
                mMediaPlayer.release();
                mMediaPlayer = null;

                setState(mCurrentPageNum, ReplayState.FINISHED);
            }
    }

    @Override
    // implementation for PlayCommandHandler
    public void togglePlayMode() {
        if (mCurrentMode != PlayerMode.PLAYBACK)
            switchMode(PlayerMode.PLAYBACK);
        else
            switchMode(PlayerMode.PLAYOUT);
    }

    public void switchMode(PlayerMode newMode) {
        switch (newMode) {
        case PLAYOUT:
            if (PlayerMode.PLAYBACK == mCurrentMode) {
                stopAudioReplay();
                mCurrentMode = newMode;
                if (null != mOnModeChangeListener)
                    mOnModeChangeListener.onModeChanged(newMode);
            }
            break;
        case PLAYBACK:
            if (PlayerMode.PLAYOUT == mCurrentMode) {
                mCurrentMode = newMode;
                if (null != mOnModeChangeListener)
                    mOnModeChangeListener.onModeChanged(newMode);
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
                mMediaRecorder.setOutputFile(mPages.get(mCurrentPageNum).getAudio().getPath());
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                mMediaRecorder.prepare();
            } catch (IOException e) {
                Log.e(getClass().getName(), "media recorder prepare() failed");
            }
        }

        mMediaRecorder.start();
        setState(mCurrentPageNum, ReplayState.RECORDING);
    }

    @Override
    // implementation for PlayCommandHandler
    public void stopAudioRecording() {
        if (isRecording(mCurrentPageNum)) {
            if (null != mMediaRecorder) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;

                setState(mCurrentPageNum, ReplayState.NOT_STARTED);
            }
        }
    }

    @Override
    // implementation for PlayCommandHandler
    public void capturePhoto() {
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

    public PlayerMode getCurrentMode() {
        return mCurrentMode;
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

            setCurrentPageNum(position);

            if (isAutoReplay())
                startAudioReplay();
        }
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageScrollStateChanged(int state) {
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

    private PlayerMode mCurrentMode;

    private OnModeChangeListener mOnModeChangeListener;
    private OnReplayChangeListener mOnReplayChangeListener;
    private OnPageChangeListener mOnPageChangeListener;

    private MediaPlayer mMediaPlayer;
    private MediaRecorder mMediaRecorder;
}
