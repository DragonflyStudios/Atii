package ca.dragonflystudios.atii.model.book;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;

public class Book extends Entity {

    public Book(File bookFolder) {
        mFolder = bookFolder;

        mInfo = new BookInfo(bookFolder);
        mPreviewFile = new File(bookFolder, "preview.png");

        // the following is slightly contrived; could introduce either a Pages
        // class or a loadArray method.
        mPages = new ArrayList<Page>();
        Parser parser = new Parser();
        try {
            parser.parseXmlFileForEntity(new File(bookFolder, "pages.xml"), this, "pages");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getFolder() {
        return mFolder;
    }

    public int getNumPages() {
        return mPages.size();
    }

    public ArrayList<Page> getPages() {
        return mPages;
    }

    public boolean hasPages() {
        return mPages.size() > 0;
    }

    public Page getPage(int pageNum) {
        return mPages.get(pageNum);
    }

    public String getTitle() {
        return mInfo.getTitle();
    }

    public File getPreviewFile() {
        return mPreviewFile;
    }

    public boolean hasPreview() {
        return mPreviewFile.exists();
    }

    @Override
    public void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("page")) {
                Page page = new Page(mImageFolder, mAudioFolder);
                page.loadFromXml(parser);
                mPages.add(page);
            } else {
                Parser.skip(parser);
            }
        }
    }

    @Override
    public void saveToXml(XmlSerializer serializer) throws IOException, IllegalArgumentException, IllegalStateException {
    }

    private File mFolder, mImageFolder, mAudioFolder;
    ArrayList<Page> mPages;
    private File mPreviewFile;

    private BookInfo mInfo;
}
