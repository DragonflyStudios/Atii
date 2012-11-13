package ca.dragonflystudios.atii.story;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Story extends Entity {
    public Story(String storyFilePath) {
        mStoryFilePath = storyFilePath;
    }

    public String getStoryFilePath() {
        return mStoryFilePath;
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

    private String mStoryFilePath;
    private ArrayList<Clip> mClips;
}
