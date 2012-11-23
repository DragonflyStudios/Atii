package ca.dragonflystudios.atii.player;

import java.util.ArrayList;

public class PlayerState {

    // TODO: auto (page) advance ...

    public interface OnReplayChangeListener {
        // does not differentiate cross from within page changes
        public void onReplayStateChanged(ReplayState newState);
    }

    public interface OnPageChangeListener {
        // does not differentiate cross from within page changes
        public void onPageChanged(int newPage);
    }

    public enum ReplayState {
        INVALID, NOT_STARTED, PLAYING, PAUSED, FINISHED
    }

    public PlayerState(int numPages, OnReplayChangeListener rl, OnPageChangeListener pl) {
        mNumPages = numPages;
        mPageStates = new ArrayList<ReplayState>(numPages);

        for (int i = 0; i < numPages; i++)
            mPageStates.add(ReplayState.NOT_STARTED);

        mOnReplayChangeListener = rl;
        mOnPageChangeListener = pl;
        mCurrentPageFragment = null;
        mCurrentPageNum = -1;
    }

    public boolean isAutoReplay() {
        return mAutoReplay;
    }

    public void setAutoReplay(boolean auto) {
        mAutoReplay = auto;
    }

    public int getNumPages() {
        return mNumPages;
    }

    public void setNumPages(int numPages) {
        mNumPages = numPages; // what do we do with the page replay states?
    }

    public boolean hasAudioOnCurrentPage() {
        return mCurrentPageFragment.hasAudio();
    }

    public int getCurrentPageNum() {
        return mCurrentPageNum;
    }

    public void setCurrentPageNum(int newPage) {
        if (mCurrentPageNum != newPage) {
            ReplayState oldPageState = (mCurrentPageNum >= 0) ? getPageState(mCurrentPageNum) : ReplayState.INVALID;
            ReplayState newPageState = getPageState(newPage);
            mCurrentPageNum = newPage;
            if (null != mOnPageChangeListener)
                mOnPageChangeListener.onPageChanged(newPage);
            if (oldPageState != newPageState && null != mOnReplayChangeListener)
                mOnReplayChangeListener.onReplayStateChanged(newPageState);
        }
    }

    public ReplayState getPageState(int pageNum) {
        return mPageStates.get(pageNum);
    }

    public void setPageState(int pageNum, ReplayState state) {
        ReplayState oldState = mPageStates.get(pageNum);
        if (oldState != state) {
            mPageStates.set(pageNum, state);
            if (mCurrentPageNum == pageNum && null != mOnReplayChangeListener)
                mOnReplayChangeListener.onReplayStateChanged(state);
        }
    }

    public boolean isReplayNotStarted(int pageNum) {
        return mPageStates.get(pageNum) == ReplayState.NOT_STARTED;
    }

    public boolean isPlaying(int pageNum) {
        return mPageStates.get(pageNum) == ReplayState.PLAYING;
    }

    public boolean isPaused(int pageNum) {
        return mPageStates.get(pageNum) == ReplayState.PAUSED;
    }

    public boolean isFinished(int pageNum) {
        return mPageStates.get(pageNum) == ReplayState.FINISHED;
    }

    public PageFragment getCurrentPageFragment() {
        return mCurrentPageFragment;
    }

    public void setCurrentPageFragment(PageFragment pf) {
        if (mCurrentPageFragment != pf) {
            mCurrentPageFragment = pf;
        }
    }

    public void startPlaying() {
        if (mCurrentPageFragment.hasAudio()
                && (isReplayNotStarted(mCurrentPageNum) || isPaused(mCurrentPageNum) || isFinished(mCurrentPageNum)))
            mCurrentPageFragment.startPlaying();
    }

    public void pausePlaying() {
        if (mCurrentPageFragment.hasAudio() && (isPlaying(mCurrentPageNum)))
            mCurrentPageFragment.pausePlaying();
    }

    public void stopPlaying() {
        if (mCurrentPageFragment.hasAudio() && isPlaying(mCurrentPageNum))
            mCurrentPageFragment.stopPlaying();
    }

    private ArrayList<ReplayState> mPageStates;
    private int mCurrentPageNum;
    private int mNumPages;
    private boolean mAutoReplay;
    private PageFragment mCurrentPageFragment;

    private OnReplayChangeListener mOnReplayChangeListener;
    private OnPageChangeListener mOnPageChangeListener;
}
