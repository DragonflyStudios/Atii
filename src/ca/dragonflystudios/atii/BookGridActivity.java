package ca.dragonflystudios.atii;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
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
import ca.dragonflystudios.android.storage.Storage;
import ca.dragonflystudios.atii.model.book.BookInfo;
import ca.dragonflystudios.atii.play.Player;
import ca.dragonflystudios.utilities.Pathname;

// TODO: 
// [ ] Turn this into a GridView with Previews and Title     1 day

// TODO: handle the case when no book was found & show empty view

public class BookGridActivity extends Activity {
    private static final String SETTINGS = "atii_settings";
    private static final String FIRST_LAUNCH = "first_launch";

    public BookGridActivity() {
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

        mBookGridView = (GridView) getLayoutInflater().inflate(R.layout.book_grid, null);

        listBooks(storiesDir);
        mBookGridAdapter = new BookGridAdapter();
        mBookGridView.setAdapter(mBookGridAdapter);
        mBookGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent playIntent = new Intent(BookGridActivity.this, Player.class);
                playIntent.putExtra(Player.STORY_EXTRA_KEY, mBookInfos.get(position).getBookPath());
                startActivity(playIntent);
            }
        });

        /*
         * mBookGridView.setOnItemLongClickListener(new
         * OnItemLongClickListener() {
         * 
         * @Override
         * public boolean onItemLongClick(AdapterView<?> parent, View view, int
         * position, long id) {
         * if (mActionMode != null) {
         * return false;
         * }
         * 
         * // Start the CAB using the ActionMode.Callback defined above
         * mActionMode =
         * BookGridActivity.this.startActionMode(mActionModeCallback);
         * view.setSelected(true);
         * mSelectedView = view;
         * 
         * return true;
         * }
         * });
         */

        mSelected = new HashSet<Integer>();
        mBookGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mBookGridView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked && !mSelected.contains(position)) {
                    mSelected.add(position);
                    mBookGridAdapter.notifyDataSetChanged();
                } else if (!checked && mSelected.contains(position)) {
                    mSelected.remove(position);
                    mBookGridAdapter.notifyDataSetChanged();
                }

                int total = mSelected.size();
                mode.setTitle(total + " selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate a menu resource providing context menu items
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.book_selected, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false; // Return false if nothing is done
            }

            // Called when the user selects a contextual menu item
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                case R.id.menu_edit:
                    Log.d(getClass().getName(), "edit!");
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_delete:
                    Log.d(getClass().getName(), "delete!");
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
                }
            }

            // Called when the user exits the action mode
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mSelected.clear();
            }

        });

        setContentView(mBookGridView);
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
                convertView.setBackgroundColor(BookGridActivity.this.getResources().getColor(android.R.color.holo_blue_bright));
            else
                convertView.setBackgroundColor(BookGridActivity.this.getResources().getColor(android.R.color.white));

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_list, menu);
        return true;
    }

    private ArrayList<BookInfo> mBookInfos;
    private Set<Integer> mSelected;
    private GridView mBookGridView;
    private BookGridAdapter mBookGridAdapter;

    private static Context sAppContext;
}
