package ca.dragonflystudios.atii;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ca.dragonflystudios.android.storage.Storage;
import ca.dragonflystudios.atii.player.Player;
import ca.dragonflystudios.utilities.Pathname;
import ca.dragonflystudios.utilities.Pathname.FileNameComparator;

// TODO: handle the case when no book was found & show empty view

public class BookListActivity extends Activity
{
    public static final String  STORY_EXTRA_KEY = "STORY_FOLDER_NAME";
    private static final String SETTINGS        = "atii_settings";
    private static final String FIRST_LAUNCH    = "first_launch";

    public BookListActivity()
    {
        mBookDirs = new ArrayList<File>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
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

        // initialize mStoryListView
        mBookListView = new ListView(this);

        listBooks(storiesDir);
        mBookListAdapter = new BookListAdapter(this, mBookDirs);
        mBookListView.setAdapter(mBookListAdapter);
        mBookListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent playIntent = new Intent(BookListActivity.this, Player.class);
                playIntent.putExtra(STORY_EXTRA_KEY, mBookDirs.get(position).getAbsolutePath());
                startActivity(playIntent);
            }
        });

        setContentView(mBookListView);
        mActionBar = getActionBar();
    }

    private static class BookListAdapter extends ArrayAdapter<File>
    {
        public BookListAdapter(Context context, List<File> bookList)
        {
            super(context, R.id.book_name, bookList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (null == convertView) {
                convertView = LayoutInflater.from(sAppContext).inflate(R.layout.book_item, null);
            }

            File book = getItem(position);
            TextView bookNameView = (TextView) convertView.findViewById(R.id.book_name);
            bookNameView.setText(Pathname.extractStem(book.getName()));
            return convertView;
        }
    }

    // action item: Search, Create

    // list items:
    // Thumbnail ... Book title + description underneath it ... Last Modified
    // Date ...
    // create a database? walk the file system?
    // [x] walk the file system sdcard/Atii ...
    // [x] List all folders ... filter in those with ".atii"
    // [ ] adapter turns thumb.bmp, title.txt, and blurb.txt ... into an item
    // view ...
    // [ ] story ...

    // design layout for BookList

    private void listBooks(File storiesDir)
    {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File path)
            {
                return path.exists() && path.isDirectory() && "atii".equalsIgnoreCase(Pathname.extractExtension(path.getName()));
            }
        };

        File[] bookList = storiesDir.listFiles(filter);
        if (bookList != null) {
            mBookDirs.clear();
            mBookDirs.addAll(Arrays.asList(bookList));
            Collections.sort(mBookDirs, new FileNameComparator());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.book_list, menu);
        return true;
    }

    private ArrayList<File> mBookDirs;
    private ListView        mBookListView;
    private BookListAdapter mBookListAdapter;
    private ActionBar       mActionBar;

    private static Context  sAppContext;
}
