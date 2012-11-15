package ca.dragonflystudios.atii.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ca.dragonflystudios.atii.model.story.Story;

import android.graphics.Rect;
import android.util.Xml;

// modeled after: http://developer.android.com/training/basics/network-ops/xml.html
public class Parser {

    public void parse(File storyFile, Story story) throws XmlPullParserException, IOException {
        InputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(storyFile));
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            parser.require(XmlPullParser.START_TAG, Story.ns, "story");
            story.loadFromXml(parser);
        } finally {
            if (in != null)
                in.close();
        }
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
        int l = 0, t = 0, r = 0, b = 0;
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("left")) {
                l = readIntFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "left");
            } else if (name.equals("top")) {
                t = readIntFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "top");
            } else if (name.equals("right")) {
                r = readIntFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "right");
            } else if (name.equals("bottom")) {
                b = readIntFromXml(parser);
                parser.require(XmlPullParser.END_TAG, Entity.ns, "bottom");
            } else {
                skip(parser);
            }
        }

        return new Rect(l, t, r, b);
    }

    public static String readTextFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    public static int readIntFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        int v = 0;
        if (parser.next() == XmlPullParser.TEXT) {
            v = Integer.parseInt(parser.getText());
            parser.nextTag();
        }
        return v;
    }

    public static long readLongFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        long l = 0;
        if (parser.next() == XmlPullParser.TEXT) {
            l = Long.parseLong(parser.getText());
            parser.nextTag();
        }
        return l;
    }
}
