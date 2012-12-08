package ca.dragonflystudios.utilities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

public class Pathname
{
    public static String extractExtension(String pathname) {
        int dotPos = pathname.lastIndexOf(".");
        return pathname.substring(dotPos + 1, pathname.length());
    }

    public static String extractStem(String pathname) {
        int dotPos = pathname.lastIndexOf(".");
        if (dotPos < 0)
            return pathname;

        return pathname.substring(0, dotPos);
    }

    public static class FileNameComparator implements Comparator<File>
    {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    public static String createUniqueFileName(File folder, String extension) {
        String fileName;

        do
            fileName = Time.getTimeStamp() + "." + extension;
        while (new File(folder, fileName).exists());

        return fileName;
    }

    public static String createUUIDFileName(File folder, String extension) {
        String fileName;

        do
            fileName = UUID.randomUUID().toString() + "." + extension;
        while (new File(folder, fileName).exists());

        return fileName;
    }

    public static File getTempFileInFolder(File folder) {
        return new File(folder, "_dragons_do_not_fly_" + Time.getTimeStamp() + ".tmp");
    }

}
