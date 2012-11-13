package ca.dragonflystudios.atii.story;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Rect;
import android.util.Xml;

// modeled after: http://developer.android.com/training/basics/network-ops/xml.html
public class Parser {

    public Story parse(String storyFilePath) throws XmlPullParserException, IOException {
        InputStream in = null;
        Story story = null;

        try {
            File file = new File(storyFilePath);
            in = new BufferedInputStream(new FileInputStream(file));
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            parser.require(XmlPullParser.START_TAG, Story.ns, "story");
            story = new Story(storyFilePath);
            story.loadFromXml(parser);
        } finally {
            if (in != null)
                in.close();
        }

        return story;
    }

    public static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
    }

    public static Rect readRectFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        Long ll = null, lt = null, lr = null, lb = null;
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("left")) {
                ll = readLongFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "left");
            } else if (name.equals("top")) {
                lt = readLongFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "top");
            } else if (name.equals("right")) {
                lr = readLongFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "right");
            } else if (name.equals("bottom")) {
                lb = readLongFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "bottom");
            } else {
                skip(parser);
            }
        }
        
        if (null == ll || null == lt || null == lr || null == lb)
            return null;
                    
        return new Rect(ll.intValue(), lt.intValue(), lr.intValue(), lb.intValue());
    }

    public static String readTextFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    public static Long readLongFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        Long l = null;
        if (parser.next() == XmlPullParser.TEXT) {
            l = Long.getLong(parser.getText());
            parser.nextTag();
        }
        return l;
    }
}
