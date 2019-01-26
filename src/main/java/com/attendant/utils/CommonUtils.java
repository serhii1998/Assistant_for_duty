package com.attendant.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {
    //определить, переданная дата равна сегодняшней, или уже прошла
    public static boolean todayBeforeOrEqualsThisDate(String dateDutyStr) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        Date today = format.parse(format.format(new Date()));
        Date dateDuty = format.parse(dateDutyStr);

        return today.before(dateDuty) || today.equals(dateDuty);
    }

}
