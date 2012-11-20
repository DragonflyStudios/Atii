package ca.dragonflystudios.atii.player;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Observable;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import ca.dragonflystudios.utilities.Pathname;
import ca.dragonflystudios.utilities.Pathname.FileNameComparator;

public class PlayerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {

    public PlayerAdapter(FragmentManager fm, File storyDir) {
        super(fm);

        mPageFiles = new ArrayList<File>();
        mNumPages = -1;
        mHasFrontCover = false;
        mHasBackCover = false;
        mPageChangeObservable = new PageChangeObservable();
        
        listPages(storyDir);
    }
    
    // TODO: It's unfortunate that the ViewPager/FragmentStatePagerAdapter API is inadequate such that
    //       we have to introduce these extras to get notifications of page change for the Fragments. TAI!
    public static class PageChangeObservable extends Observable {
        protected void setChanged() {
            super.setChanged();
        }
    }

    @Override
    public int getCount() {
        return mNumPages;
    }

    @Override
    public Fragment getItem(int position) {
        return new PageFragment(mPageFiles.get(position), mPageChangeObservable, position, mNumPages);
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageSelected(int position) {
        mPageChangeObservable.setChanged();
        mPageChangeObservable.notifyObservers(new Integer(position));
    }

    @Override
    // implementation for OnPageChangeListener
    public void onPageScrollStateChanged(int state) {
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
    
    private PageChangeObservable mPageChangeObservable;

}
