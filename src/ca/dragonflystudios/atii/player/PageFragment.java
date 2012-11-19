package ca.dragonflystudios.atii.player;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ca.dragonflystudios.atii.R;

public class PageFragment extends Fragment {

    public PageFragment(File pageFile, int pageNum, int numPages) {
        mPageFile = pageFile;
        mPageNum = pageNum;
        mNumPages = numPages;
    }
    
    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * The Fragment's UI is just a simple text view showing its instance
     * number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        
        View v = inflater.inflate(R.layout.page, container, false);
        ImageView iv = (ImageView) v.findViewById(R.id.page_image);
        Bitmap mBitmap = BitmapFactory.decodeFile(mPageFile.getAbsolutePath());
        iv.setImageBitmap(mBitmap);
        View tv = v.findViewById(R.id.page_num);
        ((TextView) tv).setText(mPageNum + "/" + mNumPages);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        //super.onDestroyView();
        
        if (null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
    
    private Bitmap mBitmap;
    private File mPageFile;
    private int mPageNum;
    private int mNumPages;
}
