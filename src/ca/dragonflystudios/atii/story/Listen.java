package ca.dragonflystudios.atii.story;

public class Listen
{
    public Listen(String audioPath, long start, long duration) {
        mAudioPath = audioPath;
        mStart = start;
        mDuration = duration;
    }

    private String mAudioPath;
    private long mStart;
    private long mDuration;
}
