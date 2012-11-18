package ca.dragonflystudios.utilities;

import java.io.File;
import java.util.Comparator;

public class Pathname
{
    public static String extractExtension(String pathname) {
        int dotPos = pathname.lastIndexOf(".");
        return pathname.substring(dotPos+1, pathname.length());
    }
    
    public static String extractStem(String pathname) {
        int dotPos = pathname.lastIndexOf(".");
        return pathname.substring(0, dotPos);
    }
    
    public static class FileNameComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
              return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

}
