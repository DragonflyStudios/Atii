package ca.dragonflystudios.atii.model.story;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;

public class Clip extends Entity {
    // We assume that a Clip is a pair for now.
    // There is a lot of room for play here!
    private Look mLook;
    private Listen mListen;

    public void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Order won't matter. But this would only keep the last instance of
        // "look" and "listen" respectively.
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("look")) {
                mLook = new Look();
                mLook.loadFromXml(parser);
            } else if (name.equals("listen")) {
                mListen = new Listen();
                mListen.loadFromXml(parser);
            } else {
                Parser.skip(parser);
            }
        }
    }

    public Look getLook() {
        return mLook;
    }
}
