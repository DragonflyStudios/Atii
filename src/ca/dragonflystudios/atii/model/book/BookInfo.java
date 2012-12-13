package ca.dragonflystudios.atii.model.book;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;
import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;
import ca.dragonflystudios.utilities.Files;
import ca.dragonflystudios.utilities.Pathname;

public class BookInfo extends Entity {

    public BookInfo(File bookFolder, String title) {
        mBookFolder = bookFolder;
        mAuthorNames = new ArrayList<String>();
        mIllustratorNames = new ArrayList<String>();

        if (null == title || "".equals(title))
            mTitle = Pathname.extractStem(bookFolder.getName());
        else
            mTitle = title;

        mBookXmlFile = new File(bookFolder, "book.xml");
        if (mBookXmlFile.exists()) {
            Parser parser = new Parser();
            try {
                parser.parseXmlFileForEntity(mBookXmlFile, this, "book");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            Log.i(getClass().getName(), "book specification file " + mBookXmlFile.getAbsolutePath() + " does not exist.");
    }

    public void save() {
        try {
            saveToXml(Xml.newSerializer());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String getBookPath() {
        return mBookFolder.getAbsolutePath();
    }

    public String getTitle() {
        return mTitle;
    }

    public ArrayList<String> getAuthorNames() {
        return mAuthorNames;
    }

    public File getPreviewFile() {
        return new File(mBookFolder, "preview.png");
    }

    @Override
    public void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                mTitle = Parser.readTextFromXml(parser);
                parser.require(XmlPullParser.END_TAG, ns, "title");
            } else if (name.equals("author")) {
                mAuthorNames.add(Parser.readTextFromXml(parser));
                parser.require(XmlPullParser.END_TAG, ns, "author");
            } else if (name.equals("illustrator")) {
                mIllustratorNames.add(Parser.readTextFromXml(parser));
                parser.require(XmlPullParser.END_TAG, ns, "illustrator");
            }
        }
    }

    @Override
    public void saveToXml(XmlSerializer serializer) throws IOException, IllegalArgumentException, IllegalStateException {
        if (null == mBookXmlFile)
            return;

        FileOutputStream os = new FileOutputStream(mBookXmlFile);
        serializer.setOutput(os, null);
        serializer.startDocument(null, true);
        serializer.startTag("", "book");
        if (null != mTitle && "" != mTitle) {
            serializer.startTag("", "title");
            serializer.text(mTitle);
            serializer.endTag("", "title");
        }
        if (mAuthorNames.size() > 0) {
            serializer.startTag("", "authors");

            for (String name : mAuthorNames) {
                serializer.startTag("", "author");
                serializer.text(name);
                serializer.endTag("", "author");
            }
            serializer.endTag("", "authors");
        }
        if (mIllustratorNames.size() > 0) {
            serializer.startTag("", "illustrators");
            for (String name : mIllustratorNames) {
                serializer.startTag("", "author");
                serializer.text(name);
                serializer.endTag("", "author");
            }
            serializer.endTag("", "illustrators");
        }
        serializer.endTag("", "book");
        serializer.endDocument();
        os.close();
    }

    public boolean delete() {
        return Files.deleteRecursive(mBookFolder);
    }

    private File mBookFolder, mBookXmlFile;
    private String mTitle;
    private ArrayList<String> mAuthorNames;
    private ArrayList<String> mIllustratorNames;
}
