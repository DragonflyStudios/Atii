package ca.dragonflystudios.atii;

public interface WorldWindowChangeRequestListener {

    // in view coordinates
    public void onRequestWorldWindowTranslation(float deltaX, float deltaY);

    // in view coordinates
    public void onRequestWorldWindowScaling(float scaling, float focusX, float focusY);
}
