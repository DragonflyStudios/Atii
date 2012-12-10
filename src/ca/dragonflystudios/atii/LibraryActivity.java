package ca.dragonflystudios.atii;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import ca.dragonflystudios.android.dialog.WarningDialogFragment;
import ca.dragonflystudios.android.dialog.WarningDialogFragment.WarningDialogListener;
import ca.dragonflystudios.android.storage.Storage;
import ca.dragonflystudios.atii.model.book.BookInfo;
import ca.dragonflystudios.atii.play.BookCreationDialog;
import ca.dragonflystudios.atii.play.BookCreationDialog.BookCreationListener;
import ca.dragonflystudios.atii.play.PlayManager.PlayMode;
import ca.dragonflystudios.atii.play.Player;
import ca.dragonflystudios.utilities.Pathname;

// TODO: handle the case when no book was found & show empty view

public class LibraryActivity extends Activity implements WarningDialogListener, BookCreationListener {
    private static final String SETTINGS = "atii_settings";
    private static final String FIRST_LAUNCH = "first_launch";
    private static final String BOOK_OPEN_MODE = "book_open_mode";

    public LibraryActivity() {
        mBookInfos = new ArrayList<BookInfo>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        if (null == sAppContext)
            sAppContext = getApplicationContext();

        File storiesDir = new File(Environment.getExternalStorageDirectory(), "Atii/Stories");
        if (!storiesDir.exists())
            storiesDir.mkdirs();

        SharedPreferences prefs = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        if (prefs.getBoolean(FIRST_LAUNCH, true)) {
            Storage.copyAssets(getAssets(), "stories", storiesDir.getAbsolutePath());
            prefs.edit().putBoolean(FIRST_LAUNCH, false).commit();
        }

        String bom = prefs.getString(BOOK_OPEN_MODE, PlayMode.READER.toString());
        mAppMode = PlayMode.valueOf(bom);

        mBookGridView = (GridView) getLayoutInflater().inflate(R.layout.book_grid, null);

        listBooks(storiesDir);
        mBookGridAdapter = new BookGridAdapter();
        mBookGridView.setAdapter(mBookGridAdapter);
        mBookGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent playIntent = new Intent(LibraryActivity.this, Player.class);
                playIntent.putExtra(Player.STORY_EXTRA_KEY, mBookInfos.get(position).getBookPath());
                playIntent.putExtra(Player.PLAY_MODE_EXTRA_KEY, PlayMode.READER.toString());
                startActivity(playIntent);
            }
        });

        mSelected = new HashSet<Integer>();
        mBookGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mBookGridView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                if (checked && !mSelected.contains(position)) {
                    mSelected.add(position);
                    mBookGridAdapter.notifyDataSetChanged();
                    int count = mSelected.size();
                    mode.setTitle(count + " selected");
                    if (2 == count)
                        mode.invalidate();
                } else if (!checked && mSelected.contains(position)) {
                    mSelected.remove(position);
                    mBookGridAdapter.notifyDataSetChanged();
                    int count = mSelected.size();
                    mode.setTitle(count + " selected");
                    if (1 == count)
                        mode.invalidate();
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.book_selected, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                if (mSelected.size() > 1 || mAppMode == PlayMode.READER)
                    menu.findItem(R.id.menu_edit).setEnabled(false).setVisible(false);
                else
                    menu.findItem(R.id.menu_edit).setEnabled(true).setVisible(true);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                case R.id.menu_edit:
                    Log.d(getClass().getName(), "edit!");
                    if (1 < mSelected.size()) {
                        Log.w(getClass().getName(), "selected more than 1 book for editing!");
                        if (BuildConfig.DEBUG)
                            throw new RuntimeException("attempting to open more than 1 book for editing!");
                    }
                    int position = -1;
                    for (int p : mSelected)
                        position = p;
                    Intent playIntent = new Intent(LibraryActivity.this, Player.class);
                    playIntent.putExtra(Player.STORY_EXTRA_KEY, mBookInfos.get(position).getBookPath());
                    playIntent.putExtra(Player.PLAY_MODE_EXTRA_KEY, PlayMode.AUTHOR.toString());
                    startActivity(playIntent);

                    mode.finish();
                    return true;
                case R.id.menu_delete:
                    DialogFragment dialog = WarningDialogFragment.newInstance(R.string.book_deletion_dialog_title,
                            R.string.deletion_no_undo_warning);
                    dialog.show(getFragmentManager(), "BookDeletionDialogFragment");
                    mToBeOped = new HashSet<Integer>();
                    mToBeOped.addAll(mSelected);

                    mode.finish();
                    return true;
                default:
                    return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mSelected.clear();
            }

        });

        setContentView(mBookGridView);
    }

    @Override
    public void onPause() {
        SharedPreferences prefs = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        prefs.edit().putString(BOOK_OPEN_MODE, mAppMode.toString()).commit();

        super.onPause();
    }

    private class BookGridAdapter extends BaseAdapter {

        public int getCount() {
            return mBookInfos.size();
        }

        public Object getItem(int position) {
            return mBookInfos.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = LayoutInflater.from(sAppContext).inflate(R.layout.book_item, null);
            }

            BookInfo book = mBookInfos.get(position);
            TextView bookNameView = (TextView) convertView.findViewById(R.id.book_title);
            bookNameView.setText(book.getTitle());

            // Apparently no need to show title, coz it's already on the cover!
            bookNameView.setVisibility(View.INVISIBLE);

            ImageView bookPreviewView = (ImageView) convertView.findViewById(R.id.book_preview);
            File previewFile = book.getPreviewFile();

            if (previewFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(previewFile.getAbsolutePath());
                bookPreviewView.setImageBitmap(bitmap);
            } else {
                bookPreviewView.setImageResource(R.drawable.default_book_preview);
            }

            // somehow View.setSelected() does not work here: it does not force
            // the use of the background selector.
            // that's why we are using setBackgroundColor directly
            if (mSelected.contains(position))
                convertView.setBackgroundColor(LibraryActivity.this.getResources().getColor(android.R.color.holo_blue_bright));
            else
                convertView.setBackgroundColor(LibraryActivity.this.getResources().getColor(android.R.color.white));

            return convertView;
        }
    }

    private void listBooks(File storiesDir) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File path) {
                return path.exists() && path.isDirectory() && "atii".equalsIgnoreCase(Pathname.extractExtension(path.getName()));
            }
        };

        File[] bookFileList = storiesDir.listFiles(filter);
        for (File bookFile : bookFileList) {
            BookInfo bookInfo = new BookInfo(bookFile);
            mBookInfos.add(bookInfo);
        }

        Collections.sort(mBookInfos, new Comparator<BookInfo>() {
            @Override
            public int compare(BookInfo b1, BookInfo b2) {
                return b1.getTitle().compareToIgnoreCase(b2.getTitle());
            }
        });
    }

    @Override
    // implementation for DeleteDialogListener
    public void onPositive() {
        deleteBooks(mToBeOped);
        mToBeOped.clear();
        mToBeOped = null;
    }

    @Override
    // implementation for DeleteDialogListener
    public void onNegative() {

    }

    @Override
    // implementation for BookCreationListener
    public void onCreateBook(String title, File sourceFolder) {
        Log.d(getClass().getName(), "create book titled: " + title);
    }

    @Override
    // implementation for BookCreationListener
    public void onCancelBookCreation() {
        Log.d(getClass().getName(), "book creation cancelled");
    }

    private void deleteBooks(Set<Integer> bookPositions) {
        ArrayList<BookInfo> booksToDelete = new ArrayList<BookInfo>();

        for (int position : bookPositions)
            booksToDelete.add(mBookInfos.get(position));

        mBookInfos.removeAll(booksToDelete);

        // TODO: error handling by checking return value from info.delete()
        for (BookInfo info : booksToDelete)
            info.delete();

        mBookGridAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.library, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (mAppMode) {
        case READER:
            menu.findItem(R.id.menu_create).setEnabled(false).setVisible(false);
            menu.findItem(R.id.menu_switch_to_reader).setEnabled(false).setVisible(false);
            menu.findItem(R.id.menu_switch_to_author).setEnabled(true).setVisible(true);
            break;
        case AUTHOR:
            menu.findItem(R.id.menu_create).setEnabled(true).setVisible(true);
            menu.findItem(R.id.menu_switch_to_reader).setEnabled(true).setVisible(true);
            menu.findItem(R.id.menu_switch_to_author).setEnabled(false).setVisible(false);
            break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_switch_to_reader:
            mAppMode = PlayMode.READER;
            invalidateOptionsMenu();
            return true;
        case R.id.menu_switch_to_author:
            mAppMode = PlayMode.AUTHOR;
            invalidateOptionsMenu();
            return true;
        case R.id.menu_create:
            DialogFragment dialog = BookCreationDialog.newInstance();
            dialog.show(getFragmentManager(), "BookCreationDialogFragment");
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private PlayMode mAppMode;

    private ArrayList<BookInfo> mBookInfos;
    private Set<Integer> mSelected, mToBeOped;
    private GridView mBookGridView;
    private BookGridAdapter mBookGridAdapter;

    private static Context sAppContext;
}
