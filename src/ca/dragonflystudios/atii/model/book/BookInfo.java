package ca.dragonflystudios.atii.model.book;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;
import ca.dragonflystudios.utilities.Pathname;

public class BookInfo extends Entity {

    public BookInfo(File bookFolder) {
        mBookFolder = bookFolder;

        File bookXmlFile = new File(bookFolder, "book.xml");
        mTitle = null;
        mAuthorNames = new ArrayList<String>();
        mIllustratorNames = new ArrayList<String>();

        initializeWithDefaults(bookFolder);

        if (bookXmlFile.exists()) {
            Parser parser = new Parser();
            try {
                parser.parseXmlFileForEntity(bookXmlFile, this, "book");
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private void initializeWithDefaults(File bookFolder) {
        mTitle = Pathname.extractStem(bookFolder.getName());
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
        // TODO Auto-generated method stub

    }

    private File mBookFolder;
    private String mTitle;
    private ArrayList<String> mAuthorNames;
    private ArrayList<String> mIllustratorNames;
}
