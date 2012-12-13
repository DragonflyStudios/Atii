package ca.dragonflystudios.atii;

import ca.dragonflystudios.utilities.Pathname;

public class Globals {

    // TODO: see also: http://developer.android.com/reference/android/provider/MediaStore.Images.Thumbnails.html
    public static final int PREVIEW_WIDTH = 300;
    public static final int PREVIEW_HEIGHT = 300;

    //TODO: refactor -- does this belong here? this is app-specific "supported image formats"
    public static boolean isImageFile(String pathname) {
        String ext = Pathname.extractExtension(pathname);
        return ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext) || "png".equalsIgnoreCase(ext));
    }

}
