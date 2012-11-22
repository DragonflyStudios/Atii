package ca.dragonflystudios.atii.player;

import java.util.ArrayList;

public class PlayerState {

    // TODO: auto (page) advance ...

    public interface OnReplayChangeListener {
        public void onReplayStateChanged(ReplayState newState); // does not
                                                                // differentiate
                                                                // cross from
                                                                // within page
                                                                // changes
    }

    public enum ReplayState {
        NOT_STARTED, PLAYING, PAUSED, FINISHED
    }

    public PlayerState(int numPages, OnReplayChangeListener listener) {
        mNumPages = numPages;
        mPageStates = new ArrayList<ReplayState>(numPages);

        for (int i = 0; i < numPages; i++)
            mPageStates.add(ReplayState.NOT_STARTED);

        mOnReplayChangeListener = listener;
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

    public int getCurrentPageNum() {
        return mCurrentPageNum;
    }

    public void setCurrentPageNum(int currentPageNum) {
        mCurrentPageNum = currentPageNum;
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

    private ArrayList<ReplayState> mPageStates;
    private int mCurrentPageNum;
    private int mNumPages;
    private boolean mAutoReplay;

    private OnReplayChangeListener mOnReplayChangeListener;
}
