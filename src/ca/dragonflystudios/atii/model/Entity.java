package ca.dragonflystudios.atii.model;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public abstract class Entity {
    protected static final String ns = null; // we don't use name space

    // I sort of wish Java allows overriding of static methods and specification
    // of static methods in interfaces. That way, each subclass of Entity could
    // define their own factory-from-xlm method.
    public abstract void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException;
    public abstract void saveToXml(XmlSerializer serializer) throws IOException, IllegalArgumentException, IllegalStateException;

}
