package ca.dragonflystudios.atii.story;

import java.io.File;
import java.util.ArrayList;

public class Story
{
    public Story(String storyFilePath) {
        mStoryFilePath = storyFilePath;
        mStoryFile = new File(storyFilePath);
    }

    public void initialize() {
        // read in mStoryFile and initialize mClips ...
    }

    private String mStoryFilePath;
    private File mStoryFile;
    private ArrayList<Clip> mClips;
}
