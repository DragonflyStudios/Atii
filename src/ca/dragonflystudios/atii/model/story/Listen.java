package ca.dragonflystudios.atii.model.story;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;

public class Listen extends Entity {
    public Listen() {

    }

    public Listen(String audioFileName, long start, long duration) {
        mAudioFileName = audioFileName;
        mStart = start;
        mDuration = duration;
    }

    public String getAudioFileName() {
        return mAudioFileName;
    }

    public Long getStart() {
        return mStart;
    }

    public Long getDuration() {
        return mDuration;
    }

    public void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Order won't matter. But this would only keep the last instance of
        // each kind.
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("audio")) {
                mAudioFileName = Parser.readTextFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "audio");
            } else if (name.equals("start")) {
                mStart = Parser.readLongFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "start");
            } else if (name.equals("duration")) {
                mDuration = Parser.readLongFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "duration");
            } else {
                Parser.skip(parser);
            }
        }
    }

    @Override
    public void saveToXml(XmlSerializer serializer) throws IOException, IllegalArgumentException, IllegalStateException {
        // TODO Auto-generated method stub
        
    }

    private String mAudioFileName;
    private Long mStart;
    private Long mDuration;
}
