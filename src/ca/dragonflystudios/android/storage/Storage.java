package ca.dragonflystudios.android.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import ca.dragonflystudios.utilities.Streams;

public class Storage
{

    public static boolean isExternalStorageAvailable()
    {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static boolean isExternalStorageWriteable()
    {
        return (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
    }

    public static void copyAssets(AssetManager assetManager, String srcPath, String dstPath)
    {
        String assets[] = null;
        try {
            assets = assetManager.list(srcPath);
            if (assets.length == 0) {
                InputStream inStream = assetManager.open(srcPath);
                FileOutputStream outStream = new FileOutputStream(dstPath);
                Streams.copy(inStream, outStream);
                inStream.close();
                outStream.close();
            } else {
                File dstFolder = new File(dstPath);
                if (!dstFolder.exists())
                    dstFolder.mkdir();
                for (String name : assets)
                    copyAssets(assetManager, srcPath + "/" + name, dstPath + "/" + name);
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

}
