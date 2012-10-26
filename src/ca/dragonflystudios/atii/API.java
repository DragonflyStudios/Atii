package ca.dragonflystudios.atii;

public interface API {

    // This section is the "data model" interface.
    public abstract static class Navigation {
    }

    public abstract static class Stopover extends Navigation {
    }

    public abstract static class Hop extends Navigation {
    }

    public interface Navigable {
        public void navigateTo(Stopover stopover);

        public void navigateBy(Hop hop);
    }

    public abstract static class Indication { }
    public interface Indicatable {
        public void indicate(Indication indication);
    }

    public abstract static class Enunciation { }
    public interface Enunciable {
        public void enunciate();
    }

    public interface Playable extends Navigable, Indicatable, Enunciable {
    }

    // Need a set of ... UI event interfaces ...

    public interface NavigationObserver {
    }

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

    public interface Touchable {
        public void onTap();

        public void onDoubleTap();

        public void onFling();

        public void onPinch();

        public void onTwist();
    }

}
