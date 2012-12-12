package ca.dragonflystudios.utilities;

import java.io.File;
import java.util.Comparator;
import java.util.UUID;

public class Pathname {
    // '~' causes problem on iOS when it is the first character of the file name.
    public static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"',
            ':', '~' };

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

    public static String makeSafeForPath(String string) {
        return string.replaceAll("\\W+", "_");
    }

    public static class FileNameComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    public static File createUniqueFile(File folder, String extension) {
        return new File(folder, createUniqueFileName(folder, "", extension));
    }

    public static File createUniqueFile(File folder, String prefix, String extension) {
        return new File(folder, createUniqueFileName(folder, prefix, extension));
    }

    public static String createUniqueFileName(File folder, String extension) {
        return createUniqueFileName(folder, "", extension);
    }

    public static String createUniqueFileName(File folder, String prefix, String extension) {
        String fileName;
        String suffix =  (null == extension && "".equals(extension)) ? "" : "." + extension;

        do
            fileName = prefix + Time.getTimeStamp() + suffix;
        while (new File(folder, fileName).exists());

        return fileName;
    }

    public static String createUUIDFileName(File folder, String extension) {
        String fileName;
        String suffix =  (null == extension && "".equals(extension)) ? "" : "." + extension;

        do
            fileName = UUID.randomUUID().toString() + suffix;
        while (new File(folder, fileName).exists());

        return fileName;
    }

    public static File getTempFileInFolder(File folder) {
        return new File(folder, "_dragons_do_not_fly_" + Time.getTimeStamp() + ".tmp");
    }

}
