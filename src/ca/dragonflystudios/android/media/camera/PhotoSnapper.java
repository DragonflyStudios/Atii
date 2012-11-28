package ca.dragonflystudios.android.media.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

public class PhotoSnapper {

    // TODO: retake logic -- take one, show one for a few seconds; then allow
    // taking another one -- show "Retake" button; one is happy enough then
    // press "Done"

    public PhotoSnapper(ViewGroup hostView, PictureCallback callback) {
        mHostView = hostView;
        mPictureCallback = callback;

        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            // TODO: error processing with graceful failure
            throw new RuntimeException(e);
        }

        mPreview = new CameraPreview(hostView.getContext(), mCamera);
        mHostView.addView(mPreview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mButtonsView = new FrameLayout(hostView.getContext());
        mHostView.addView(mButtonsView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mSnapButton = new Button(hostView.getContext());
        mSnapButton.setText("Snap");
        mSnapButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPictureCallback);
            }
        });
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.LEFT);
        mButtonsView.addView(mSnapButton, lp);

        mDoneButton = new Button(hostView.getContext());
        mDoneButton.setText("Done");
        mDoneButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                stop();
            }
        });
        lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.RIGHT);
        mButtonsView.addView(mDoneButton, lp);
    }

    public void stop() {
        if (mCamera != null) {
            mCamera.release();
        }

        mButtonsView.removeView(mSnapButton);
        mButtonsView.removeView(mDoneButton);
        mHostView.removeView(mButtonsView);
        mHostView.removeView(mPreview);

        mCamera = null;
        mPreview = null;
        mButtonsView = null;
        mSnapButton = null;
        mDoneButton = null;
        mPictureCallback = null;
        mPhotoFilePath = null;
    }

    private ViewGroup mHostView;
    private FrameLayout mButtonsView;
    private String mPhotoFilePath;
    private CameraPreview mPreview;
    private PictureCallback mPictureCallback;
    private Button mSnapButton, mDoneButton;
    private Camera mCamera;
}
