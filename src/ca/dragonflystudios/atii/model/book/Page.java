package ca.dragonflystudios.atii.model.book;

import java.io.File;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import ca.dragonflystudios.atii.BuildConfig;
import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;
import ca.dragonflystudios.utilities.Pathname;

// TODO: Should we also allow a page that has no page image? If only to be symmetrical?

public class Page extends Entity {
    public enum PlaybackState {
        INVALID, NO_AUDIO, NOT_STARTED, PLAYING, PAUSED, FINISHED
    }

    public Page(File imageFolder, File audioFolder) {
        mImageFolder = imageFolder;
        mAudioFolder = audioFolder;

        // uses lazy initialization
        mInitialized = false;
        mState = PlaybackState.INVALID;

        mUsingNewImage = false;
    }

    public Page(File imageFolder, String imageFileName, File audioFolder, String audioFileName) {
        mImageFolder = imageFolder;
        mAudioFolder = audioFolder;

        if (null != imageFileName)
            mImage = new File(mImageFolder, imageFileName);
        if (null != audioFileName)
            mAudio = new File(mAudioFolder, audioFileName);

        // uses lazy initialization
        mInitialized = false;
        mState = PlaybackState.INVALID;

        mUsingNewImage = false;
    }

    public PlaybackState getPlaybackState() {
        if (!mInitialized)
            initializeAudioFile();

        return mState;
    }

    public void setPlaybackState(PlaybackState s) {
        mState = s;
    }

    public File getImage() {
        if (mUsingNewImage)
            return mNewImage;

        return mImage;
    }

    public File getImageFileForWriting() {
        if (null == mNewImage)
            mNewImage = Pathname.createUniqueFile(mImageFolder, ".jpg");
        mUsingNewImage = true;

        return mNewImage;
    }

    public void commitNewImage() {
        if (null != mImage && mImage.exists()) {
            if (!mImage.delete()) {
                String msg = "failed to delete " + mImage.getAbsolutePath();
                if (BuildConfig.DEBUG)
                    throw new RuntimeException(msg);
                else
                    Log.w(getClass().getName(), msg);
            }
        }

        mImage = mNewImage;
        mNewImage = null;
        mUsingNewImage = false;
    }

    public void discardNewImage() {
        if (null != mNewImage && mNewImage.exists()) {
            if (!mNewImage.delete()) {
                String msg = "failed to delete " + mNewImage.getAbsolutePath();
                if (BuildConfig.DEBUG)
                    throw new RuntimeException(msg);
                else
                    Log.w(getClass().getName(), msg);
            }
        }

        mNewImage = null;
        mUsingNewImage = false;
    }

    // TODO: something tricky here w.r.t. name correspondence. May not need to
    // do anything special. TAI!
    public void setImage(File newImage) {
        mImage = newImage;
    }

    public boolean isEmpty() {
        return (null == mImage && null == mAudio);
    }

    public File getAudio() {
        if (!mInitialized)
            initializeAudioFile();

        return mAudio;
    }

    public File getAudioFileForWriting() {
        if (null == mAudio)
            mAudio = new File(mAudioFolder, Pathname.createUniqueFileName(mAudioFolder, "3gp"));

        return mAudio;
    }

    public void setAudio(File newAudio) {
        mAudio = newAudio;

        if (null == mAudio)
            mInitialized = false;
        else if (mAudio.exists())
            mState = PlaybackState.NOT_STARTED;
        else
            mState = PlaybackState.NO_AUDIO;
    }

    public boolean hasAudio() {
        if (!mInitialized)
            initializeAudioFile();

        return (PlaybackState.NO_AUDIO != mState);
    }

    public void removePageFiles() {
        if (null != mImage && mImage.exists()) {
            mImage.delete();
            mImage = null;
        }

        if (null != mAudio && mAudio.exists()) {
            mAudio.delete();
            mAudio = null;
        }

        mInitialized = false;
        mState = PlaybackState.NO_AUDIO;
    }

    private void initializeAudioFile() {
        if (null != mAudio && mAudio.exists())
            mState = PlaybackState.NOT_STARTED;
        else
            mState = PlaybackState.NO_AUDIO;

        mInitialized = true;
    }

    public boolean isUsingNewImage() {
        return mUsingNewImage;
    }

    public String getImagePath() {
        File imageFile = mUsingNewImage ? mNewImage : mImage;

        if (null == imageFile)
            return null;

        return imageFile.getAbsolutePath();
    }

    public String getAudioPath() {
        if (null == mAudio)
            return null;

        return mAudio.getAbsolutePath();
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
        mState = PlaybackState.INVALID;
    }

    @Override
    public void saveToXml(XmlSerializer serializer) throws IOException, IllegalArgumentException, IllegalStateException {
        serializer.startTag("", "page");
        File imageFile = getImage();
        if (null != imageFile && imageFile.exists()) {
            serializer.startTag("", "image");
            serializer.text(imageFile.getName());
            serializer.endTag("", "image");
        }
        File audioFile = getAudio();
        if (null != audioFile && audioFile.exists()) {
            serializer.startTag("", "audio");
            serializer.text(audioFile.getName());
            serializer.endTag("", "audio");
        }
        serializer.endTag("", "page");
    }

    private PlaybackState mState;
    private File mImageFolder, mAudioFolder, mImage, mAudio;
    private File mNewImage;
    private boolean mInitialized, mUsingNewImage;
}
