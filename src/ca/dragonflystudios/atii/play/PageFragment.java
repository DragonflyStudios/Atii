package ca.dragonflystudios.atii.play;

import java.io.File;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import ca.dragonflystudios.android.media.Image;
import ca.dragonflystudios.atii.R;

public class PageFragment extends Fragment {

    public interface OnPageImageChoice {
        public void onDiscard();

        public void onKeep();
    }

    public static PageFragment newInstance(String pageImagePath, boolean isNewImage) {
        PageFragment f = new PageFragment();

        Bundle args = new Bundle();
        args.putString("pageImage", pageImagePath);
        args.putBoolean("newImage", isNewImage);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPageImagePath = getArguments().getString("pageImage");
        mIsNewImage = getArguments().getBoolean("newImage");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v;

        if ((null != mPageImagePath) && (new File(mPageImagePath).exists())) {
            v = inflater.inflate(R.layout.page, container, false);
            ImageView iv = (ImageView) v.findViewById(R.id.page_image);

            Bitmap mBitmap = Image.decodeBitmapFileSampled(mPageImagePath, container.getWidth(), container.getHeight());

            if (null != mBitmap)
                iv.setImageBitmap(mBitmap);
            else
                Log.i(getClass().getName(), "failed to decode page image file at " + mPageImagePath);

            if (mIsNewImage) {
                View bv = v.findViewById(R.id.choice_buttons);
                bv.setVisibility(View.VISIBLE);
                mDiscardButton = (ImageButton) bv.findViewById(R.id.discard_button);
                mDiscardButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        ((OnPageImageChoice) getActivity()).onDiscard();
                    }
                });
                mKeepButton = (ImageButton) bv.findViewById(R.id.keep_button);
                mKeepButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        ((OnPageImageChoice) getActivity()).onKeep();
                    }
                });
            }
        } else
            v = inflater.inflate(R.layout.empty_page, container, false);

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
    private boolean mIsNewImage;
    private ImageButton mDiscardButton, mKeepButton;
    private String mPageImagePath;
}
