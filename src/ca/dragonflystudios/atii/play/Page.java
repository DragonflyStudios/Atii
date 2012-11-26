package ca.dragonflystudios.atii.play;

import java.io.File;

import ca.dragonflystudios.utilities.Pathname;

// TODO: Should we also allow a page that has no page image? If only to be symmetrical?

public class Page {

    public enum ReplayState {
        INVALID, UNINITIALIZED, NO_AUDIO, NOT_STARTED, PLAYING, PAUSED, FINISHED, RECORDING
    }

    // TODO: hide STATE so as to do appropriate lazy initialization!!!
    //       SHOULD NOT expose UNITITIALIZED! Should use a boolean instead!
    
    public ReplayState state;

    public Page(File imageFile) {
        mImage = imageFile;
        mAudio = null;
        // uses lazy initialization
        state = ReplayState.UNINITIALIZED;
    }

    public File getImage() {
        return mImage;
    }

    // TODO: something tricky here w.r.t. name correspondence. May not need to
    // do anything special. TAI!
    public void setImage(File newImage) {
        mImage = newImage;
    }

    public File getAudio() {
        if (ReplayState.UNINITIALIZED == state)
            initializeAudioFile();

        return mAudio;
    }

    public void setAudio(File newAudio) {
        mAudio = newAudio;

        if (null == mAudio)
            state = ReplayState.UNINITIALIZED;
        else if (mAudio.exists())
            state = ReplayState.NOT_STARTED;
        else
            state = ReplayState.NO_AUDIO;
    }

    public boolean hasAudio() {
        if (ReplayState.UNINITIALIZED == state)
            initializeAudioFile();

        return (ReplayState.NO_AUDIO != state);
    }

    private void initializeAudioFile() {
        String nameStem = Pathname.extractStem(mImage.getName());
        mAudio = new File(mImage.getParent(), nameStem + ".3gp");

        if (mAudio.exists())
            state = ReplayState.NOT_STARTED;
        else
            state = ReplayState.NO_AUDIO;
    }

    File mImage;
    File mAudio;
}
