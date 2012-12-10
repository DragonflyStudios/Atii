package ca.dragonflystudios.atii.play;

import ca.dragonflystudios.atii.model.book.Page;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class AtiiPagerAdapter extends FragmentStatePagerAdapter {

    public AtiiPagerAdapter(FragmentManager fm, PlayManager pm) {
        super(fm);
        mPlayManager = pm;
    }

    @Override
    public int getCount() {
        return mPlayManager.getNumPages();
    }

    @Override
    public Fragment getItem(int position) {
        Page page = mPlayManager.getPage(position);
        return PageFragment.newInstance(page.getImagePath(), page.isUsingNewImage());
    }

    private PlayManager mPlayManager;
}
