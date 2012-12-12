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
import ca.dragonflystudios.utilities.Pathname;

public class Book extends Entity {

    public static Book create(File parentFolder, String title, File sourceFolder) {
        
        String name = Pathname.makeSafeForPath(title);
        // create bookFolder
        // copy files over 
        // generate preview.png
        
        Book book = new Book();
        if (null != sourceFolder && sourceFolder.exists())
            book.importPages(sourceFolder);

        return book;
    }

    public Book(File bookFolder) {
        mFolder = bookFolder;

        mInfo = new BookInfo(bookFolder);
        mPreviewFile = new File(bookFolder, "preview.png");
        mPages = new ArrayList<Page>();
        mImageFolder = new File(bookFolder, "images");
        if (!mImageFolder.exists())
            mImageFolder.mkdir();
        mAudioFolder = new File(bookFolder, "audios");
        if (!mAudioFolder.exists())
            mAudioFolder.mkdir();
        mPagesXmlFile = new File(bookFolder, "pages.xml");

        if (mPagesXmlFile.exists()) {
            Parser parser = new Parser();
            try {
                parser.parseXmlFileForEntity(new File(bookFolder, "pages.xml"), this, "pages");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            Log.i(getClass().getName(), "pages specification file " + mPagesXmlFile.getAbsolutePath() + " does not exist.");

        if (0 == mPages.size())
            mPages.add(new Page(mImageFolder, mAudioFolder));
    }

    public void save() {
        mInfo.save();

        if (mPages.size() > 1 || (mPages.size() == 1 && !mPages.get(0).isEmpty())) {
            try {
                saveToXml(Xml.newSerializer());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
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

    public void addPageAt(int pageNum) {
        if (pageNum >= mPages.size())
            pageNum = mPages.size();
        else if (pageNum < 0)
            pageNum = 0;

        Page newPage = new Page(mImageFolder, mAudioFolder);
        mPages.add(pageNum, newPage);
    }

    // returns index of the new current page
    public int deletePageAt(int pageNum) {
        if (pageNum >= mPages.size() || pageNum < 0)
            return -1;

        if (pageNum == 0 && mPages.size() == 1) {
            mPages.get(0).removePageFiles();
            return 0;
        }

        int newPage = pageNum;
        if (pageNum == mPages.size() - 1)
            newPage = pageNum - 1;
        Page page = mPages.remove(pageNum);
        page.removePageFiles();

        return newPage;
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
        FileOutputStream os = new FileOutputStream(mPagesXmlFile);
        serializer.setOutput(os, null);
        serializer.startDocument(null, true);
        if (mPages.size() > 0) {
            serializer.startTag("", "pages");
            for (Page page : mPages)
                page.saveToXml(serializer);
            serializer.endTag("", "pages");
        }
        serializer.endDocument();
        os.close();
    }

    public String getBookPath() {
        return mFolder.getAbsolutePath();
    }

    private File mFolder, mPagesXmlFile, mImageFolder, mAudioFolder;
    ArrayList<Page> mPages;
    private File mPreviewFile;

    private BookInfo mInfo;
}
