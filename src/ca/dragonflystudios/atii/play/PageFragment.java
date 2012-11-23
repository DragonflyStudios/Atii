package ca.dragonflystudios.atii.play;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.atii.play.PlayerAdapter.PageChangeObservable;
import ca.dragonflystudios.atii.play.PlayerState.ReplayState;
import ca.dragonflystudios.utilities.Pathname;

public class PageFragment extends Fragment implements Observer, OnCompletionListener {

    // TODO: Use delegate pattern for PlayerState? TAI!

    // TODO: use setArguments()!
    public PageFragment(File pageFile, PlayerState playerState, PageChangeObservable pageChangeObservable, int pageNum, int numPages) {
        mPageFile = pageFile;
        mPageNum = pageNum;
        mNumPages = numPages;
        mHasAudio = audioForPage(pageFile);
        mPageChangeObservable = pageChangeObservable;

        mPlayerState = playerState;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mPageNum == mPlayerState.getCurrentPageNum())
            mPlayerState.setCurrentPageFragment(this);
    }

    /**
     * The Fragment's UI is just a simple text view showing its instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.page, container, false);
        ImageView iv = (ImageView) v.findViewById(R.id.page_image);
        Bitmap mBitmap = BitmapFactory.decodeFile(mPageFile.getAbsolutePath());
        iv.setImageBitmap(mBitmap);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPageChangeObservable.addObserver(this);

        // pure hack! to force playing of the 1st page when stories is first
        // played
        if (mPlayerState.isAutoReplay() && mPlayerState.isReplayNotStarted(mPageNum) && 0 == mPageNum && mHasAudio) {
            startPlaying();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPageChangeObservable.deleteObserver(this);
        if (mHasAudio && mPlayerState.isPlaying(mPageNum)) {
            stopPlaying(); // TODO: save the current position ...
        }

        if (mPlayerState.isRecording(mPageNum))
            stopRecording();
    }

    public void startPlaying() {
        if (null == mMediaPlayer) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setDataSource(mPageAudio.getPath());
                mMediaPlayer.prepare();
            } catch (IOException e) {
                Log.e(getClass().getName(), "prepare() failed with IOException");
                e.printStackTrace();
            }
        }

        mMediaPlayer.start();
        mPlayerState.setPageState(mPageNum, ReplayState.PLAYING);
    }

    public void pausePlaying() {
        if (null != mMediaPlayer) {
            mMediaPlayer.pause();
            mPlayerState.setPageState(mPageNum, ReplayState.PAUSED);
        }
    }

    public void stopPlaying() {
        if (null != mMediaPlayer) {
            mMediaPlayer.release();
            mMediaPlayer = null;

            mPlayerState.setPageState(mPageNum, ReplayState.FINISHED);
        }
    }

    public void startRecording() {
        if (null == mMediaRecorder) {
            try {
                mMediaRecorder = new MediaRecorder();
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mMediaRecorder.setOutputFile(getAudioOutputPath());
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                mMediaRecorder.prepare();
            } catch (IOException e) {
                Log.e(getClass().getName(), "media recorder prepare() failed");
            }
        }

        mMediaRecorder.start();
        mPlayerState.setPageState(mPageNum, ReplayState.RECORDING);
    }

    public void stopRecording() {
        if (null != mMediaRecorder) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;

            mHasAudio = true;
            mPlayerState.setPageState(mPageNum, ReplayState.NOT_STARTED);
        }
    }

    private String getAudioOutputPath() {
        String nameStem = Pathname.extractStem(mPageAudio.getName());
        mPageAudio = new File(mPageAudio.getParent(), nameStem + ".3gp");

        return mPageAudio.getAbsolutePath();
    }

    public boolean hasAudio() {
        return mHasAudio;
    }

    private boolean audioForPage(File pageFile) {
        String nameStem = Pathname.extractStem(pageFile.getName());
        mPageAudio = new File(pageFile.getParent(), nameStem + ".3gp");

        if (!mPageAudio.exists())
            mPageAudio = new File(pageFile.getParent(), nameStem + ".mp3");

        return mPageAudio.exists();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mPlayerState = null;
    }

    @Override
    // implementation for Observer
    public void update(Observable pageChangeObservable, Object page) {
        int p = (Integer) page;

        if (p == mPageNum)
            mPlayerState.setCurrentPageFragment(this);

        if (isResumed() && mHasAudio) {
            if (p == mPageNum) {
                if (mPlayerState.isAutoReplay() && !mPlayerState.isPlaying(mPageNum))
                    startPlaying(); // TODO: resume
            } else if (mPlayerState.isPlaying(mPageNum) || mPlayerState.isPaused(mPageNum))
                stopPlaying();
        }
    }

    @Override
    // implementation for MediaPlayer.OnCompletionListener
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp)
            stopPlaying();
    }

    private Bitmap mBitmap;
    private File mPageFile;
    private int mPageNum;
    private int mNumPages;

    private boolean mHasAudio;
    private File mPageAudio;
    private MediaPlayer mMediaPlayer;
    private MediaRecorder mMediaRecorder;

    private PageChangeObservable mPageChangeObservable;

    private PlayerState mPlayerState;
}
