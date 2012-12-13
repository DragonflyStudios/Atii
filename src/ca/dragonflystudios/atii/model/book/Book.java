package ca.dragonflystudios.atii.model.book;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Xml;
import ca.dragonflystudios.android.media.Image;
import ca.dragonflystudios.atii.BuildConfig;
import ca.dragonflystudios.atii.Globals;
import ca.dragonflystudios.atii.model.Entity;
import ca.dragonflystudios.atii.model.Parser;
import ca.dragonflystudios.utilities.Files;
import ca.dragonflystudios.utilities.Pathname;
import ca.dragonflystudios.utilities.Pathname.FileNameComparator;

public class Book extends Entity {

    private static final int MAX_TITLE_CHARS = 20;

    public static Book create(File parentFolder, String title, File sourceFolder) {

        int length = title.length();
        File bookFolder = Pathname.createUniqueFile(parentFolder, Pathname.makeSafeForPath(title.substring(0,
                (length > MAX_TITLE_CHARS) ? MAX_TITLE_CHARS : length)) + "_", "atii");
        if (!bookFolder.mkdirs()) {
            if (BuildConfig.DEBUG)
                throw new RuntimeException("failed to create book folder for new book titled " + title);
            else {
                Log.w("Book.create()", "failed to create book folder for new booked titled " + title);
                return null;
            }
        }

        Book book = new Book(bookFolder, title);
        if (null != sourceFolder && sourceFolder.exists())
            book.importPages(sourceFolder);

        book.save();

        return book;
    }

    public Book(File bookFolder, String title) {
        mFolder = bookFolder;

        mInfo = new BookInfo(bookFolder, title);
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

    private void importPages(File sourceFolder) {

        File[] imageList = sourceFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File path) {
                return path.exists() && !path.isHidden() && Globals.isImageFile(path.getName());
            }
        });
        ArrayList<File> imageFiles = new ArrayList<File>();
        if (imageList != null) {
            imageFiles.addAll(Arrays.asList(imageList));
            Collections.sort(imageFiles, new FileNameComparator());
        }

        mPages.clear();

        // TODO: this is blocking the UI thread
        for (File file : imageFiles) {
            String name = file.getName();
            try {
                Files.copy(file.getAbsolutePath(), new File(mImageFolder, name).getAbsolutePath());
            } catch (IOException ioe) {
                ioe.printStackTrace();
                if (BuildConfig.DEBUG)
                    throw new RuntimeException(ioe);
            }
            mPages.add(new Page(mImageFolder, name, mAudioFolder, null));
        }

        // TODO: refactor this one into a separate method
        if (!imageFiles.isEmpty()) {
            Bitmap coverBmp = Image.decodeBitmapFileIntoSize(imageFiles.get(0).getAbsolutePath(), Globals.PREVIEW_WIDTH,
                    Globals.PREVIEW_HEIGHT);
            try {
                FileOutputStream out = new FileOutputStream(mPreviewFile);
                coverBmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                if (BuildConfig.DEBUG)
                    throw new RuntimeException(ioe);
            }
        }
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
