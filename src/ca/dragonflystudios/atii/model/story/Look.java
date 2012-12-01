package ca.dragonflystudios.atii.model.story;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;

import android.graphics.Rect;

public class Look extends Entity
{
    public Look() {
        
    }

    public Look(String photoFileName, Rect worldWindow, Rect viewport) {
        mPhotoFileName = photoFileName;
        mWorldWindow = worldWindow;
        mViewport = viewport;
    }

    public String getPictureFileName() {
        return mPhotoFileName;
    }
    public Rect getWindowRect() {
        return mWorldWindow;
    }
    
    public Rect getViewportRect() {
        return mViewport;
    }
    
    public void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Order won't matter. But this would only keep the last instance of each kind.
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("photo")) {
                mPhotoFileName = Parser.readTextFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "photo");
            } else if (name.equals("window")) {
                mWorldWindow = Parser.readRectFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "window");
            } else if (name.equals("viewport")) {
                mViewport = Parser.readRectFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "viewport");
            } else {
                Parser.skip(parser);
            }
        }
    }

    @Override
    public void saveToXml(XmlSerializer serializer) throws IOException, IllegalArgumentException, IllegalStateException {
        // TODO Auto-generated method stub
        
    }

    private String mPhotoFileName;
    private Rect mWorldWindow;
    private Rect mViewport;
}
