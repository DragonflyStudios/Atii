package ca.dragonflystudios.android.dialog;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ca.dragonflystudios.android.storage.Storage;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.utilities.Pathname.FileNameComparator;

public class FolderChooser extends DialogFragment {

    public static final String PATH = "path";
    public static final String EXTERNAL_BASE_PATH = Storage.getExternalStorageRoot();

    // TODO: should also open media storage? this is so esp. for importing page
    // images

    public interface FolderChooserListener {
        public void onFolderChosen(File file);

        public void onFolderChooserCancel();
    }

    public static FolderChooser newInstance() {
        FolderChooser fc = new FolderChooser();
        return fc;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FolderChooserListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FolderChooserListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        mListView = new ListView(context);

        mCurrentFolder = Environment.getExternalStorageDirectory();
        mParents = new ArrayList<File>();

        ArrayList<File> subFolders = listFiles(mCurrentFolder);
        mListAdapter = new FileListAdapter(context, subFolders);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mParents.add(mCurrentFolder);
                mCurrentFolder = mListAdapter.getItem(position);
                getDialog().setTitle(mCurrentFolder.getName());
                ArrayList<File> subFolders = listFiles(mCurrentFolder);
                mListAdapter.clear();
                mListAdapter.addAll(subFolders);
                mListAdapter.notifyDataSetChanged();
            }
        });

        builder.setView(mListView).setTitle(R.string.choose_folder)
                .setPositiveButton(R.string.choose, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onFolderChosen(mCurrentFolder);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onFolderChooserCancel();
                    }
                });
        return builder.create();
    }

    private ArrayList<File> listFiles(File dir) {
        File[] fileList = dir.listFiles(mFilter);
        ArrayList<File> files = new ArrayList<File>();
        if (fileList != null) {
            files.addAll(Arrays.asList(fileList));
            Collections.sort(files, new FileNameComparator());
        }

        return files;
    }

    public static class FileListAdapter extends ArrayAdapter<File> {
        public FileListAdapter(Context context, List<File> fileList) {
            super(context, R.id.file_name, fileList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_list_item, null);
            }

            File file = getItem(position);
            ((TextView) convertView).setText(file.getName());
            return convertView;
        }
    }

    private FolderChooserListener mListener;
    private ListView mListView;
    private FileListAdapter mListAdapter;
    private ArrayList<File> mParents;
    private File mCurrentFolder;
    private FileFilter mFilter = new FileFilter() {
        @Override
        public boolean accept(File path) {
            return path.exists() && path.isDirectory();
        }
    };

}
