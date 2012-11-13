package ca.dragonflystudios.atii.story;

import android.graphics.Rect;

public class Look
{
    public Look(String picturePath, Rect worldWindow, Rect viewport) {
        mPicturePath = picturePath;
        mWorldWindow = worldWindow;
        mViewport = viewport;
    }

    private String mPicturePath;
    private Rect mWorldWindow;
    private Rect mViewport;
}
