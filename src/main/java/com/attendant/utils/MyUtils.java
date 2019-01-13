package com.attendant.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyUtils {

    public static boolean thisDateAlreadyPassed(String dateAttendantStr) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        String curDateStr = format.format(new Date());
        Date curDate = format.parse(curDateStr);
        Date dateAttendant = format.parse(dateAttendantStr);

        return (curDate.getTime() - dateAttendant.getTime() > 0);
    }
}
