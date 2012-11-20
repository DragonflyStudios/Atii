package ca.dragonflystudios.atii.player;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.atii.player.PlayerAdapter.PageChangeObservable;
import ca.dragonflystudios.utilities.Pathname;

public class PageFragment extends Fragment implements Observer {

    // TODO: use setArguments()!
    public PageFragment(File pageFile, PageChangeObservable pageChangeObservable, int pageNum, int numPages) {
        mPageFile = pageFile;
        mPageNum = pageNum;
        mNumPages = numPages;
        mHasAudio = audioForPage(pageFile);
        mAutoPlayed = false;
        mIsPlaying = false;
        mPageChangeObservable = pageChangeObservable;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        View tv = v.findViewById(R.id.page_num);
        ((TextView) tv).setText((mPageNum+1) + "/" + mNumPages);
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
    }

    @Override
    public void onPause() {
        super.onPause();
        mPageChangeObservable.deleteObserver(this);
        if (mHasAudio && mIsPlaying) {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mMediaPlayer = new MediaPlayer();
        mIsPlaying = true;

        try {
            mMediaPlayer.setDataSource(mPageAudio.getPath());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mAutoPlayed = false;
        } catch (IOException e) {
            Log.e(getClass().getName(), "prepare() failed with IOException");
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if (null != mMediaPlayer) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mIsPlaying = false;
    }
    
    private boolean audioForPage(File pageFile) {
        String nameStem = Pathname.extractStem(pageFile.getName());
        File audioFile = new File(pageFile.getParent(), nameStem + ".mp3");
        
        if (audioFile.exists()) {
            mPageAudio = audioFile;
            return true;
        }
            
        return false;
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
    // implementation for Observer
    public void update(Observable pageChangeObservable, Object page) {
        if (isResumed() && mHasAudio) {
            if (((Integer)page) == mPageNum) {
                if (!mIsPlaying && !mAutoPlayed)
                    startPlaying();
            } else if (mIsPlaying)
                stopPlaying();
        }
    }
    
    private Bitmap mBitmap;
    private File mPageFile;
    private int mPageNum;
    private int mNumPages;
    
    private boolean mAutoPlayed;
    private boolean mHasAudio;
    private File mPageAudio;
    private MediaPlayer mMediaPlayer;
    
    private boolean mIsPlaying;
    
    private PageChangeObservable mPageChangeObservable;
}
