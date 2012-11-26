package ca.dragonflystudios.atii.play;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import ca.dragonflystudios.atii.R;

public class PageFragment extends Fragment {

    public static PageFragment newInstance(String pageImagePath) {
        PageFragment f = new PageFragment();

        Bundle args = new Bundle();
        args.putString("pageImage", pageImagePath);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPageImagePath = (getArguments() != null ? getArguments().getString("pageImage") : PlayManager.getDefaultPageImagePath());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.page, container, false);
        ImageView iv = (ImageView) v.findViewById(R.id.page_image);
        Bitmap mBitmap = BitmapFactory.decodeFile(mPageImagePath);
        iv.setImageBitmap(mBitmap);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        mPageImagePath = null;
        super.onDestroy();
    }

    private Bitmap mBitmap;
    private String mPageImagePath;
}
