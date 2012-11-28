package ca.dragonflystudios.atii.play;

import java.io.File;

import ca.dragonflystudios.utilities.Pathname;

// TODO: Should we also allow a page that has no page image? If only to be symmetrical?

public class Page {

    public enum AudioPlaybackState {
        INVALID, NO_AUDIO, NOT_STARTED, PLAYING, PAUSED, FINISHED
    }

    // TODO: hide STATE so as to do appropriate lazy initialization!!!
    // SHOULD NOT expose UNITITIALIZED! Should use a boolean instead!

    public Page(File imageFile) {
        mImage = imageFile;
        mAudio = null;
        // uses lazy initialization
        mInitialized = false;
        mState = AudioPlaybackState.INVALID;
    }

    public AudioPlaybackState getAudioPlaybackState() {
        if (!mInitialized)
            initializeAudioFile();

        return mState;
    }

    public void setAudioPlaybackState(AudioPlaybackState s) {
        mState = s;
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
        if (!mInitialized)
            initializeAudioFile();

        return mAudio;
    }

    public void setAudio(File newAudio) {
        mAudio = newAudio;

        if (null == mAudio)
            mInitialized = false;
        else if (mAudio.exists())
            mState = AudioPlaybackState.NOT_STARTED;
        else
            mState = AudioPlaybackState.NO_AUDIO;
    }

    public boolean hasAudio() {
        if (!mInitialized)
            initializeAudioFile();

        return (AudioPlaybackState.NO_AUDIO != mState);
    }

    private void initializeAudioFile() {
        String nameStem = Pathname.extractStem(mImage.getName());
        mAudio = new File(mImage.getParent(), nameStem + ".3gp");

        if (mAudio.exists())
            mState = AudioPlaybackState.NOT_STARTED;
        else
            mState = AudioPlaybackState.NO_AUDIO;

        mInitialized = true;
    }

    public String getImagePath() {
        return mImage.getAbsolutePath();
    }

    private AudioPlaybackState mState;
    private File mImage;
    private File mAudio;
    private boolean mInitialized;
}
