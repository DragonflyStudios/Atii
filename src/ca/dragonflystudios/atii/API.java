package ca.dragonflystudios.atii;

public interface API {

    // This section is the "data model" interface.
    // These are used to programatically "play" a playable.
    // They could be used to implement? record? compose? perform? a replayable?

    public interface Activity { } // manages frames ... starting, ending ... real time ...
    public interface Navigation extends Activity { }
    public interface Stopover extends Navigation { }
    public interface Hop extends Navigation { }

    public interface Navigable {
        public void navigateTo(Stopover stopover);

        public void navigateBy(Hop hop);
    }

    public interface Indication extends Activity { }
    public interface Indicatable {
        public void indicate(Indication indication);
    }

    public interface Enunciation extends Activity { }
    public interface Enunciable {
        public void enunciate(Enunciation enunciation);
    }

    public interface Playable extends Navigable, Indicatable, Enunciable { }

    // These are used to replay a Replayable
    public interface Replayable {
        public void start();

        public void stop();

        public void pause();

        public void resume();

        public void next();

        public void previous();

        public void fastforward();

        public void fastbackward();

        public void toStart();

        public void toFinish();
    }

    // Need a set of ... real time UI event monitoring interfaces ...

    public interface Monitorable { }
    public interface ActivityMonitor {
        public void onActivityStart(Monitorable observable, Activity activity);
        public void onActivityUpdate(Monitorable observable, Activity activity);
        public void onActivityFinish(Monitorable observable, Activity activity);
    }


    public interface Touchable {
        public void onTap();

        public void onDoubleTap();

        public void onFling();

        public void onPinch();

        public void onTwist();
    }

}
