package ca.dragonflystudios.android.dialog;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ca.dragonflystudios.android.storage.Storage;
import ca.dragonflystudios.atii.R;
import ca.dragonflystudios.utilities.Pathname;
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
        mListAdapter = new FileListAdapter(context);
        mListView.setAdapter(mListAdapter);
        mListAdapter.setItems(subFolders);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selected = mListAdapter.getItem(position);
                if (selected.isDirectory()) {
                    mCurrentFolder = mListAdapter.getItem(position);
                    mParents.add(mCurrentFolder);
                    getDialog().setTitle(mCurrentFolder.getName());
                    ArrayList<File> subFolders = listFiles(mCurrentFolder);
                    mListAdapter.setItems(subFolders);
                }
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

    public static class FileListAdapter extends BaseAdapter {

        public FileListAdapter(Context context) {
            mFiles = new ArrayList<File>();
            mFolderDrawable = context.getResources().getDrawable(R.drawable.file_folder);
            mFileDrawable = context.getResources().getDrawable(R.drawable.file_file);
            mPictureDrawable = context.getResources().getDrawable(R.drawable.file_picture);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_list_item, null);
            }

            TextView tv = (TextView) convertView;
            File file = getItem(position);
            ((TextView) convertView).setText(file.getName());
            if (file.isDirectory())
                tv.setCompoundDrawablesWithIntrinsicBounds(mFolderDrawable, null, null, null);
            else if (isImageFile(file.getName()))
                tv.setCompoundDrawablesWithIntrinsicBounds(mPictureDrawable, null, null, null);
            else
                tv.setCompoundDrawablesWithIntrinsicBounds(mFileDrawable, null, null, null);

            return tv;
        }

        @Override
        public File getItem(int position) {
            return mFiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return mFiles.size();
        }

        public void setItems(ArrayList<File> items) {
            mFiles.clear();
            mFiles.addAll(items);
            notifyDataSetChanged();
        }

        private boolean isImageFile(String pathname) {
            String ext = Pathname.extractExtension(pathname);
            return ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext) || "png".equalsIgnoreCase(ext));
        }

        private ArrayList<File> mFiles;
        private Drawable mFileDrawable, mPictureDrawable, mFolderDrawable;
    }

    private FolderChooserListener mListener;
    private ListView mListView;
    private FileListAdapter mListAdapter;
    private ArrayList<File> mParents;
    private File mCurrentFolder;
    private FileFilter mFilter = new FileFilter() {
        @Override
        public boolean accept(File path) {
            return path.exists() && !path.isHidden();
        }
    };

}
