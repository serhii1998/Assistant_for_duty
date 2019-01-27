package com.attendant.utils;

import com.attendant.googleSpreadsheet.SheetsQuickstart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;


import java.text.ParseException;
import java.util.List;

import static com.attendant.utils.CommonUtils.todayBeforeOrEqualsThisDate;

public class UtilsSpreadsheet {

    private static Logger logger = LoggerFactory.getLogger(UtilsSpreadsheet.class);

    //возвращает true в случае, если искомая комната найдена
    public static boolean checkExistenceThisRoomAndSetDateDutyToSendMessage(String room, SendMessage sendMessage) {
        sendMessage.setText("");
        boolean existenceThisRoom = false; // флаг существования комнаты в гугл таблице.
        // Если ее там в гугл таблицах нет, то ее не нужно записывать в БД

        List<List<Object>> values = null;
        try {

            values = SheetsQuickstart.infoAttendantGoogleSpreadsheet("Database!A3:S"); // получаем заданную страницу в виде вдумерного массива
            logger.info("******** UtilsSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage -> room(message) == {}, sendMessage == {}", room, sendMessage.toString());
            System.out.println(values);
            checkDate:
            if (values != null) {
                for (List row : values) {
                    for (int i = 0; i < row.size(); i++) {
                        if (room.equals(row.get(i).toString())) { // если переданая в сообщении комната равна комнате в БД из Гугл таблиц
                            existenceThisRoom = true;
                            String dateDuty = row.get(i + 1).toString().trim();// то достанем дату дежурства, а это соседняя колонка
                            try {
                                if (!dateDuty.equals("") && todayBeforeOrEqualsThisDate(dateDuty)) { // прошла ли дата дежурства
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

        } catch (Exception e) {
            logger.warn("******** UtilsSpreadsheet ->  checkExistenceThisRoomAndSetDateDutyToSendMessage -> Exception values");
            e.printStackTrace();
        }

        logger.info("******** UtilsSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage -> \n sendMessage.getText() == {}, existenceThisRoom == {}", sendMessage.getText(), existenceThisRoom);
        return existenceThisRoom;
    }
}
