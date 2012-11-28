package ca.dragonflystudios.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {

    public static String getTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

}
