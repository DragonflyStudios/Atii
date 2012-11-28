package ca.dragonflystudios.android.media.camera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import ca.dragonflystudios.android.storage.Storage;
import ca.dragonflystudios.atii.BuildConfig;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.utilities.Files;

public class PhotoSnapper implements PictureCallback {

    // TODO: retake logic -- take one, show one for a few seconds; then allow
    // taking another one -- show "Retake" button; one is happy enough then
    // press "Done"

    public interface OnCompletionListener {
        public void onPhotoSnapperCompletion(File photoFile, boolean success);
    }

    public PhotoSnapper(ViewGroup hostView, String photoFilePath, OnCompletionListener l) {
        mHostView = hostView;
        mPhotoFilePath = photoFilePath;
        mPSL = l;

        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            // TODO: error processing with graceful failure
            throw new RuntimeException(e);
        }

        mPreview = new CameraPreview(hostView.getContext(), mCamera);
        mHostView.addView(mPreview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mButtonsView = (ViewGroup) LayoutInflater.from(hostView.getContext()).inflate(R.layout.photo_snapper_controls, null);

        mExposurePlusButton = (Button) mButtonsView.findViewById(R.id.exposure_plus_button);
        mExposurePlusButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            }
        });
        mSnapButton = (Button) mButtonsView.findViewById(R.id.snap_button);
        mSnapButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mCamera.takePicture(null, null, PhotoSnapper.this);
            }
        });
        mExposureMinusButton = (Button) mButtonsView.findViewById(R.id.exposure_minus_button);
        mExposureMinusButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            }
        });

        mDoneButton = (Button) mButtonsView.findViewById(R.id.done_button);
        mDoneButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                File photoFile = null;

                if (null != mTempFile && mTempFile.exists()) {
                    photoFile = new File(mPhotoFilePath);
                    try {
                        FileOutputStream fos = new FileOutputStream(photoFile);
                        FileInputStream fis = new FileInputStream(mTempFile);

                        Files.copy(fis, fos);
                    } catch (FileNotFoundException e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                            throw new RuntimeException();
                        } else
                            Log.w(getClass().getName(), "failed to open file for copying from " + mTempFile + " to " + mPhotoFilePath);
                        photoFile = null;
                    } catch (IOException e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                            throw new RuntimeException();
                        } else
                            Log.w(getClass().getName(), "failed to copy from " + mTempFile + " to " + mPhotoFilePath);
                        photoFile = null;
                    } finally {
                        deleteTempFile();
                    }
                }

                if (null != mPSL)
                    mPSL.onPhotoSnapperCompletion(photoFile, photoFile != null);

                cleanUp();
            }
        });

        mCancelButton = (Button) mButtonsView.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                deleteTempFile();
                if (null != mPSL)
                    mPSL.onPhotoSnapperCompletion(null, false);

                cleanUp();
            }
        });

        // it seems that to successfully remove mButtonsView, it can't be added directly through inflate(, root) ...
        mHostView.addView(mButtonsView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void cleanUp() {
        if (mCamera != null) {
            mCamera.release();
        }

        mHostView.removeView(mButtonsView);
        mHostView.removeView(mPreview);

        mCamera = null;
        mPreview = null;
        mButtonsView = null;
        mSnapButton = null;
        mExposurePlusButton = null;
        mExposureMinusButton = null;
        mDoneButton = null;
        mCancelButton = null;
        mPhotoFilePath = null;
    }

    private void deleteTempFile() {
        if (null != mTempFile && mTempFile.exists() && mTempFile.delete())
            Log.w(getClass().getName(), "failed to detele temp file: " + mTempFile);
    }

    @Override
    // implementation for PictureCallback
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            if (null == mTempFile)
                mTempFile = Storage.getTempFile(mHostView.getContext());
            FileOutputStream fos = new FileOutputStream(mTempFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(getClass().getName(), "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(getClass().getName(), "Error accessing file: " + e.getMessage());
        }
    }

    private ViewGroup mHostView;
    private ViewGroup mButtonsView;
    private String mPhotoFilePath;
    private CameraPreview mPreview;
    private Button mDoneButton, mCancelButton, mExposurePlusButton, mSnapButton, mExposureMinusButton;
    private Camera mCamera;

    private OnCompletionListener mPSL;
    private File mTempFile;
}
