package ca.dragonflystudios.atii.story;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Story extends Entity {
    public Story(File storyFolder) {
        mStoryFolderPath = storyFolder.getAbsolutePath();

        File storyFile = new File(storyFolder, "story.xml");
        Parser parser = new Parser();
        try {
            parser.parse(storyFile, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getStoryFolderPath() {
        return mStoryFolderPath;
    }

    public ArrayList<Clip> getClips() {
        return mClips;
    }

    @Override
    public void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        mClips = new ArrayList<Clip>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("clip")) {
                Clip clip = new Clip();
                clip.loadFromXml(parser);
                mClips.add(clip);
            } else {
                Parser.skip(parser);
            }
        }
    }

    private String mStoryFolderPath;
    private ArrayList<Clip> mClips;
}
