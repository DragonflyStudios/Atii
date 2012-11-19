package ca.dragonflystudios.atii.player;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ca.dragonflystudios.utilities.Pathname;
import ca.dragonflystudios.utilities.Pathname.FileNameComparator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PlayerAdapter extends FragmentStatePagerAdapter {

    public PlayerAdapter(FragmentManager fm, File storyDir) {
        super(fm);

        mPageFiles = new ArrayList<File>();
        mNumPages = -1;
        mHasFrontCover = false;
        mHasBackCover = false;

        listPages(storyDir);
    }

    @Override
    public int getCount() {
        return mNumPages;
    }

    @Override
    public Fragment getItem(int position) {
        return PageFragment.newInstance(position);
    }

    public boolean hasFrontCover() {
        return mHasFrontCover;
    }

    public boolean hasBackCover() {
        return mHasBackCover;
    }

    // TODO: refactor and combine with listBooks() in BookListActivity?
    private void listPages(File storyDir) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File path) {
                String name = path.getName();
                return path.exists() && !path.isDirectory() && "jpg".equalsIgnoreCase(Pathname.extractExtension(name))
                        && !"front".equals(Pathname.extractStem(name)) && !"back".equals(Pathname.extractStem(name));
            }
        };

        File[] pageList = storyDir.listFiles(filter);

        mPageFiles.clear();
        if (pageList != null) {
            mPageFiles.addAll(Arrays.asList(pageList));
            Collections.sort(mPageFiles, new FileNameComparator());
        }

        File frontCover = new File(storyDir, "front.jpg");
        if (frontCover.exists())
            mPageFiles.add(0, frontCover);

        File backCover = new File(storyDir, "back.jpg");
        if (backCover.exists())
            mPageFiles.add(backCover);

        mNumPages = mPageFiles.size();
    }

    private ArrayList<File> mPageFiles;
    private int mNumPages;
    private boolean mHasFrontCover;
    private boolean mHasBackCover;
}
