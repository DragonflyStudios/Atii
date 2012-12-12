package ca.dragonflystudios.atii.play;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import ca.dragonflystudios.android.dialog.FolderChooser;
import ca.dragonflystudios.android.dialog.FolderChooser.FolderChooserListener;
import ca.dragonflystudios.atii.R;

public class BookCreationDialog extends DialogFragment {
    public interface BookCreationListener {
        public void onCreateBook(String title, File sourceFolder);

        public void onCancelBookCreation();
    }

    public static BookCreationDialog newInstance() {
        return new BookCreationDialog();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (BookCreationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement BookCreationListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup creationView = (ViewGroup) inflater.inflate(R.layout.book_creation_view, null);
        mTitleEntry = (EditText) creationView.findViewById(R.id.book_title_entry);
        mImportCheckBox = (CheckBox) creationView.findViewById(R.id.checkbox_import);
        mImportCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    mImportFolderEntry.setVisibility(View.VISIBLE);
                else
                    mImportFolderEntry.setVisibility(View.INVISIBLE);
            }
        });

        mImportFolderEntry = (EditText) creationView.findViewById(R.id.source_folder_entry);
        mImportFolderEntry.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FolderChooser fc = FolderChooser.newInstance();
                fc.show(getFragmentManager(), "FolderChooserFragment");
            }
        });
        builder.setView(creationView).setTitle(R.string.book_creation)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String title = mTitleEntry.getText().toString();
                        if ("".equals(title))
                            title = mTitleEntry.getHint().toString();

                        File sourceFolder = new File(mImportFolderPath);
                        if (null == mImportFolderPath || "".equals(mImportFolderPath) || !sourceFolder.exists())
                            sourceFolder = null;

                        mListener.onCreateBook(title, sourceFolder);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onCancelBookCreation();
                    }
                });
        return builder.create();
    }

    public void updateImportPath(String newPath) {
        mImportFolderPath = newPath;
        mImportFolderEntry.setText(newPath);
    }

    private BookCreationListener mListener;
    private String mImportFolderPath;
    private EditText mTitleEntry;
    private CheckBox mImportCheckBox;
    private EditText mImportFolderEntry;
}
