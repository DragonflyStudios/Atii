package ca.dragonflystudios.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.util.Log;

public class Files {
    public static void copy(String src, String dst) throws IOException {
        copy(new FileInputStream(src), new FileOutputStream(dst));
    }

    // TODO: shouldn't close streams from within the method!
    public static void copy(FileInputStream inStream, FileOutputStream outStream) throws IOException {
        try {
            if (null != inStream && null != outStream) {
                FileChannel inChannel = inStream.getChannel();
                FileChannel outChannel = outStream.getChannel();

                if (null != inChannel && null != outChannel) {
                    long count = inChannel.transferTo(0, inChannel.size(), outChannel);
                    Log.d("Files.copy", count + " many bytes have been transferred.");
                }
            }
        } finally {
            inStream.close();
            outStream.close();
        }
    }

    public static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                if (!deleteRecursive(child))
                    return false;

        return fileOrDirectory.delete();
    }

    public static void rm(String path) {
        File file = new File(path);

        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) {
            }
        }
    }
}
