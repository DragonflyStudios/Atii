package ca.dragonflystudios.utilities;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Files {
    public static void copy(String src, String dst) throws IOException {
        copy(new FileInputStream(src), new FileOutputStream(src));
    }

    // TODO: shouldn't close streams from within the method!
    public static void copy(FileInputStream inStream, FileOutputStream outStream) throws IOException {
        try {
            if (null != inStream && null != outStream) {
                FileChannel inChannel = inStream.getChannel();
                FileChannel outChannel = outStream.getChannel();

                if (null != inChannel && null != outChannel)
                    inChannel.transferTo(0, inChannel.size(), outChannel);
            }
        } finally {
            inStream.close();
            outStream.close();
        }
    }
}
