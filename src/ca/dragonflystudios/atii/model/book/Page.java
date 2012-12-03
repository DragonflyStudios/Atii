package ca.dragonflystudios.atii.model.book;

import java.io.File;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;
import ca.dragonflystudios.utilities.Pathname;

// TODO: Should we also allow a page that has no page image? If only to be symmetrical?

public class Page extends Entity {

    public enum AudioPlaybackState {
        INVALID, NO_AUDIO, NOT_STARTED, PLAYING, PAUSED, FINISHED
    }

    public Page(File imageFolder, File audioFolder) {
        mImageFolder = imageFolder;
        mAudioFolder = audioFolder;

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
        if (mInitialized)
            return;

        if (mAudio.exists())
            mState = AudioPlaybackState.NOT_STARTED;
        else
            mState = AudioPlaybackState.NO_AUDIO;

        mInitialized = true;
    }

    public String getImagePath() {
        return mImage.getAbsolutePath();
    }

    @Override
    public void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        String imageFileName = null;
        String audioFileName = null;
        mImage = null;
        mAudio = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("image")) {
                imageFileName = Parser.readTextFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "image");
            } else if (name.equals("audio")) {
                audioFileName = Parser.readTextFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "audio");
            }
        }

        if (null == imageFileName)
            imageFileName = Pathname.createUniqueFileName(mImageFolder, "jpg");

        if (null == audioFileName)
            audioFileName = Pathname.createUniqueFileName(mAudioFolder, "3gp");
        
        mImage = new File(mImageFolder, imageFileName);
        mAudio = new File(mAudioFolder, audioFileName);

        // uses lazy initialization
        mInitialized = false;
        mState = AudioPlaybackState.INVALID;
    }

    @Override
    public void saveToXml(XmlSerializer serializer) throws IOException, IllegalArgumentException, IllegalStateException {
    }

    private AudioPlaybackState mState;
    private File mImageFolder, mAudioFolder, mImage, mAudio;
    private boolean mInitialized;
}
