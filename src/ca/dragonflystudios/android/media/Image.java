package ca.dragonflystudios.android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Image {

    // adapted from:
    // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    public static Bitmap decodeBitmapFileSampled(String path, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap decodeBitmapFileIntoSize(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        // scale to best fit
        float sourceWidth = (float) options.outWidth;
        float sourceHeight = (float) options.outHeight;
        float sourceRatio = sourceWidth / sourceHeight;
        float destRatio = (float) reqWidth / (float) reqHeight;
        float destWidth, destHeight;
        if (sourceRatio > destRatio) {
            destWidth = reqWidth;
            destHeight = reqWidth / sourceRatio;
        } else {
            destWidth = reqHeight * sourceRatio;
            destHeight = reqHeight;
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, (int) destWidth, (int) destHeight, false);
        return bitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (0 == reqWidth || 0 == reqHeight) {
            if (width > 2000 || height > 2000)
                inSampleSize = Math.max(Math.round((float) width / 1000f), Math.round((float) width / 1000f));
            else
                inSampleSize = 1;
        } else if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
}
