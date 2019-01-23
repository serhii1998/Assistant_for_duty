package com.attendant.utils;

import com.attendant.googleSpreadsheet.SheetsQuickstart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class UtilsSpreadsheet {

    private static Logger logger = LoggerFactory.getLogger(UtilsSpreadsheet.class);

    public static boolean thisDateAlreadyPassed(String dateAttendantStr) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        String curDateStr = format.format(new Date());
        Date curDate = format.parse(curDateStr);
        Date dateAttendant = format.parse(dateAttendantStr);

        logger.info("******** UtilsSpreadsheet -> thisDateAlreadyPassed == {}, dateAttendantStr == {}, curDate == {}", curDate.getTime() - dateAttendant.getTime() > 0, dateAttendant, curDate);
        return (curDate.getTime() - dateAttendant.getTime() > 0);
    }

    //возвращает true в случае, если искомая комната найдена
    public static boolean checkExistenceThisRoomAndSetDateDutyToSendMessage(String room, SendMessage sendMessage) {
        sendMessage.setText("");
        boolean existenceThisRoom = false; // флаг существования комнаты в гугл таблице.
        // Если ее там в гугл таблицах нет, то ее не нужно записывать в БД

        List<List<Object>> values = null;
        try {
            values = SheetsQuickstart.infoAttendantGoogleSpreadsheet(); // получаем заданную страницу в виде вдумерного массива
        } catch (Exception e) {
            logger.warn("******** UtilsSpreadsheet ->  checkExistenceThisRoomAndSetDateDutyToSendMessage -> Exception values");
            e.printStackTrace();
        }

        logger.info("******** UtilsSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage -> room(message) == {}, sendMessage == {}", room, sendMessage.toString());
        checkDate:
        if (values != null) {
            for (List row : values) {
                for (int i = 0; i < row.size(); i++) {
                    if (room.equals(row.get(i).toString())) { // если переданая в сообщении комната равна комнате в БД из Гугл таблиц
                        existenceThisRoom = true;
                        String dateDuty = row.get(i + 1).toString().trim();// то достанем дату дежурства, а это соседняя колонка
                        try {
                            if (!thisDateAlreadyPassed(dateDuty)) { // прошла ли дата дежурства
                                sendMessage.setText("Комната " + room + " дежурит " + dateDuty);// ВНИМАНИЕ!!! изменение єтого сообщения, которое в setText,
                                //повлечет за собой изменение в данных, которые заносятся в базу. я парсю это сообщение, что бы найти в нем дату дежурства
                                // (dateDuty). главное, что-бы осталось слово дежурит а за этим словом дальше шла дата,
                                // так как привязываюсь для поиска я именно к слову "дежурит"
                                break checkDate;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        logger.info("******** UtilsSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage -> \n sendMessage.getText() == {}, existenceThisRoom == {}", sendMessage.getText(), existenceThisRoom);
        return existenceThisRoom;
    }

}
