package ca.dragonflystudios.android.dialog.file;

import android.app.DialogFragment;
import ca.dragonflystudios.android.storage.Storage;

public class Chooser extends DialogFragment {

    public static final String PATH = "path";
    public static final String EXTERNAL_BASE_PATH = Storage.getExternalStorageRoot();

}
