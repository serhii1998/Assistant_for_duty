package com.attendant.utils;

import com.attendant.googleSpreadsheet.SheetsQuickstart;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class UtilsSpreadsheet {

    public static boolean thisDateAlreadyPassed(String dateAttendantStr) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        String curDateStr = format.format(new Date());
        Date curDate = format.parse(curDateStr);
        Date dateAttendant = format.parse(dateAttendantStr);

        return (curDate.getTime() - dateAttendant.getTime() > 0);
    }

    //возвращает true в случае, если искомая комната найдена
    public static boolean getDateAttendant(String s, SendMessage sendMessage) {

        List<List<Object>> values = null;
        try {
            values = SheetsQuickstart.infoAttendantGoogleSpreadsheet();
        } catch (Exception e) {
            System.out.println("sendMsg catch");
            e.printStackTrace();
        }
        if (values != null) {
            for (List row : values) {
                for (int i = 0; i < row.size(); i++) {
                    if (s.equals(row.get(i).toString())) { // если переданая в сообщении комната равна комнате в БД из Гугл таблиц
                        String dateAttendant = row.get(i + 1).toString();// то достанем дату дежурства, а это соседняя колонка
                        try {
                            if (!thisDateAlreadyPassed(dateAttendant)) { // прошла ли дата дежурства
                                sendMessage.setText(dateAttendant);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
/////////////////////>>?????????????????????????????
        return true;
    }

}
