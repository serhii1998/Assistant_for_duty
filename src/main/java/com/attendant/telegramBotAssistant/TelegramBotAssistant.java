package com.attendant.telegramBotAssistant;

import com.attendant.model.ReminderEntity;
import com.attendant.utils.UtilsDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.attendant.utils.UtilsSpreadsheet.*;
import static com.attendant.utils.UtilsDB.*;

public class TelegramBotAssistant extends TelegramLongPollingBot {

    private boolean checkCreateReminder = false; // флаг, означающий, что пользователь хочет установить напоминание

    private static Logger logger = LoggerFactory.getLogger(TelegramBotAssistant.class);

    @Override
    public synchronized void onUpdateReceived(Update update) {
        logger.info("onUpdateReceived");
        String message = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        String room = update.getMessage().getText();
        if (checkCreateReminder) {
            message = "2 этап создания напоминания";
        }
        if (message.contains("Напоминание установлено на")) {
            logger.info("onUpdateReceived -> Напоминание установлено на... message == {}", message);
            String checkStr = "Напоминание установлено на";
            String checkStr2 = "комнату";
            message = message.substring(message.indexOf(checkStr) + checkStr.length()).trim();
            message = message.substring(0, message.length() - checkStr2.length()).trim();
            room = message;
            logger.info("onUpdateReceived -> after substring message == {}", message);
        }
        switch (message) {
            case "/start":
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> /start");
                message = "Привет! Я могу помочь тебе в поиске даты дежурства любой комнаты. Вот что я умею:\n" +
                        "1. Напиши мне комнату, а я пришлю тебе дату следующего дежурства этой комнаты\n" +
                        "2. Ты можешь создать напоминание дежурства своей комнаты. Нажми на кнопку \"Создать напоминание\", а после введи комнату, на которую хочешь создать напоминание.\n" +
                        "3. Напоминания можно удалять или менять, нажимая соответствующие кнопки: \"Удалить напоминание\" и \"Поменять комнату для напоминания\"\n" +
                        "4. Снизу кнопка, которая информирует тебя о выбраной для напоминания комнаты. если нажать на нее, то выведется сообщение со следующей датой дежурства этой комнаты\n" +
                        "У меня есть некоторая особенность: я нахожусь в режиме сна с 24:00 до 08:00. Поэтому, если ты мне напишешь что-то, когда я буду спать, то отвечу я тебе только утром, не обижайся)\n" +
                        "Напоминания о дежурстве приходят около 9:00. Достаточно поставить напоминание 1 раз и я буду проверять наличие новой даты дежурства выбранной комнаты! \n" +
                        "Давай начнем сотрудничесво)";
                sendMsg(message, chatId);
                break;
            case "Создать напоминание":
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> Создать напоминание");
                message = "Введи комнату на которую хочешь создать напоминание";
                checkCreateReminder = true;
                sendMsg(message, chatId);
                break;
            case "Поменять комнату для напоминания":
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> Поменять комнату для напоминания");
                message = "Введи комнату на которую хочешь поменять напоминание";
                checkCreateReminder = true;
                sendMsg(message, chatId);
                break;
            case "2 этап создания напоминания":
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> 2 этап создания напоминания");
                createReminderAndSendMessage(room, chatId);
                break;
            case "Удалить напоминание":
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> Удалить напоминание");
                message = "Напоминание удалено";
                deleteReminderFromChatId(chatId);
                sendMsg(message, chatId);
                break;
            case "Напоминание пока не установлено":
                break;
            default:
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> default switch");
                sendMsgSearchDateDutyInGoogleSpreadsheet(room, chatId);
                break;
        }

    }

    private synchronized void createReminderAndSendMessage(String room, String chatId) {
        logger.info("-!-!-!-!-!-!-!-!-!- TelegramBotAssistant -> createReminderAndSendMessage");
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(room);
        setButtons(sendMessage, chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);

        boolean checkExistenceThisRoom = checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage);
        try {
            if (checkExistenceThisRoom && checkCreateReminder) {
                checkCreateReminder = false; // флаг. когда он false, это значит, что установки напоминания небыло. сейчас меняем в false потому, что установка напоминание происходит сейчас

                logger.info("-!-!-!-!-!-!-!-!-!- createReminderAndSendMessage -> checkCreateReminder = true");

                String dateDutyFromSendMessage = sendMessage.getText().trim(); // по моей логике, тут должна быть дата дежурства, которая добавилась из метода checkExistenceThisRoomAndSetDateDutyToSendMessage.
                // находим целевую дату дежурства с помощью слова "дежурит"
                String subString = "дежурит";
                String dateDuty = "";

                if (!dateDutyFromSendMessage.equals("")) {//если в гугл таблице существует дата следующего дежурства, то получить его из sendMessage и распарсить, та как там не чистая дата а уже готовое сообщение.
                    dateDuty = dateDutyFromSendMessage.substring(dateDutyFromSendMessage.indexOf(subString) + subString.length()).trim();
                }
                logger.info("chat_id == {}, room == {}, dateDutyFromSendMessage == {}, dateDuty == {}", chatId, room, dateDutyFromSendMessage, dateDuty);

                // Пользователь просто хочет узнать дату дежурства
                if (dateDuty.equals("")) {
                    try {// в таблице нету след даты дежурства, но напоминание все равно должно быть установлено
                        logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") = {}", dateDuty.equals(""));
                        saveReminderInDB(chatId, room, dateDuty);
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Напоминание создано для комнаты " + room + ". Пока следующей даты дежурства нет. " +
                                "Я буду уведомлять тебя о наступлении дежурства в течении 3-х дней перед " +
                                "дежурством, включая день дежурства." +
                                " Следи за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));

                    } catch (Exception e) {
                        logger.warn("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") CATCH");
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Что-то пошло не так( код ошибки 1. сообщите о проблеме и код ошибки старосте этажа"));
                        e.printStackTrace();
                    }

                } else {

                    try { // все ок, комната найдена и напоминание установлено
                        logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") = {}, {}", dateDuty.equals(""), dateDuty);
                        saveReminderInDB(chatId, room, dateDuty);
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Напоминание создано для комнаты " + room + ". Дата дежурства этой комнаты: " + dateDuty +
                                ". Я буду напоминать о дежурстве в течении 3 дней перед дежурством, включая день дежурства"));
                    } catch (TelegramApiException e) {
                        logger.warn("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") == false CATCH");
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Что-то пошло не так( код ошибки 2. сообщите о проблеме и код ошибки старосте этажа"));
                        e.printStackTrace();
                    }

                }

            } else if (checkCreateReminder) {
                logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage) = false, checkCreateReminder = true");
                execute(sendMessage.setText("Такой комнаты нет в графике")); // если такой комнаты не существует
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendMsg(String message, String chatId) {
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage, chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            logger.info("-!-!-!-!-!-!-!-!-!- sendMsg");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendMsgSearchDateDutyInGoogleSpreadsheet(String room, String chatId) {
        logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet");
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage, chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);

        boolean checkExistenceThisRoom = checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage);
        try {
            if (checkExistenceThisRoom && !checkCreateReminder) {
                if (sendMessage.getText().trim().equals("")) {
                    logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> sendMessage.getText().trim().equals(\"\") = true + {}", sendMessage.toString());
                    execute(sendMessage.setText("В графике пока нет даты следующего дежурства для комнаты " + room + ". Установи напоминание или " +
                            " следи за графиком здесь: https://docs.google.com/spreadsheets/d/1zr8LAZrBNCZJVWdptHRPBINRVyuhGgacEPiEcTU8zCs/edit#gid=73285756"));
                } else {
                    logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> sendMessage.getText().trim().equals(\"\") = false + {}", sendMessage.toString());
                    execute(sendMessage);
                }
            } else if (!checkCreateReminder) {// здесь нужно это условие, что бы в случае установки напоминание бот не попадал в этот else
                logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> else if");
                execute(sendMessage.setText("Такой комнаты нет в графике"));
            }

        } catch (Exception e) {
            logger.warn("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet CATCH");
            e.printStackTrace();
        }
    }

    public synchronized static void setButtons(SendMessage sendMessage, String chatId) {
        logger.info("-!-!-!-!-!-!-!-!-!- setButtons");
        String room = getRoomRemainderByChatId(chatId).trim();
        //Создаем клавиуатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        KeyboardButton keyboardButton = new KeyboardButton();
        KeyboardButton keyboardButton2 = new KeyboardButton();
        KeyboardButton keyboardButton3 = new KeyboardButton();
        KeyboardRow keyboardRow = new KeyboardRow();
        KeyboardRow keyboardRow2 = new KeyboardRow();
        List<KeyboardRow> keyboard = new ArrayList<>();

        if (room.equals("")) {
            keyboardButton.setText("Создать напоминание");
            keyboardButton2.setText("Напоминание пока не установлено");

            keyboardRow.add(keyboardButton);
            keyboardRow.add(keyboardButton2);

            keyboard.add(keyboardRow);

        } else {
            keyboardButton.setText("Поменять комнату для напоминания");
            keyboardButton2.setText("Удалить напоминание");
            keyboardButton3.setText("Напоминание установлено на " + room + " комнату");

            keyboardRow.add(keyboardButton);
            keyboardRow.add(keyboardButton2);
            keyboardRow2.add(keyboardButton3);

            keyboard.add(keyboardRow);
            keyboard.add(keyboardRow2);
        }

        logger.info("-!-!-!-!-!-!-!-!-!- setButtons -> keyboard == {}", keyboard.toString());

        replyKeyboardMarkup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }


    /*
     * 1. key - это значение дня. 0 - это текущий день. 1 - завтрашний. 2 - послезавтрашний
     * 2. ArrayList<ReminderEntity> - это списки напоминаний для соответстующих дней(для тех, кто дежурит сегодня, завтра поле-завтра)
     */

    public synchronized void sendReminder(HashMap<Integer, ArrayList<ReminderEntity>> mapReminders) {
        logger.info("-!-!-!-!-!-!-!-!-!- sendReminder -> {}", mapReminders.toString());
        for (Map.Entry<Integer, ArrayList<ReminderEntity>> entry : mapReminders.entrySet()) {
            int dayDuty = entry.getKey(); // 0 - сегодня, 1 - завтра, 2 - послезавтра
            String message = "";
            for (ReminderEntity r : entry.getValue()) {
                switch (dayDuty) {
                    case 0:
                        if (!r.isSendConfirmationToday()) {
                            message = "Привет! Комната " + r.getNumberRoom() + " сегодня дежурная. Удачного дня :)";
                            sendMsg(message, r.getChatId());
                        }
                        break;
                    case 1:
                        if (!r.isSendConfirmationTomorrow()) {
                            message = "Привет! Комната " + r.getNumberRoom() + " завтра (" + r.getDateDuty() + ") дежурная. Удачного дня :)";
                            sendMsg(message, r.getChatId());
                        }
                        break;
                    case 2:
                        if (!r.isSendConfirmationAfterTomorrow()) {
                            message = "Привет! Комната " + r.getNumberRoom() + " дежурит через 2 дня (" + r.getDateDuty() + "). Удачного дня :)";
                            sendMsg(message, r.getChatId());
                        }
                        break;
                    default:
                        logger.warn("-!-!-!-!-!-!-!-!-!- sendReminder -> default -> WARNING");
                        break;
                }
            }
        }

        setStatusSendingReminder(mapReminders);
    }

    public void sendReminderIn1600(String today) {
        String message = "Не забывай, дежурство в 17:00. Хорошего вечера :)";
        ArrayList<ReminderEntity> reminders = new ArrayList<>(UtilsDB.getReminderGivenDate(today));

        for (ReminderEntity r : reminders) {
            if (!r.isSendConfirmationTodayIn1600()) {
                logger.info("-!-!-!-!-!-!-!-!-!- sendReminderIn1600 -> !r.isSendConfirmationTodayIn1600() == false, today == {}", today);
                sendMsg(message, r.getChatId());
            }
        }

        setStatusSendingReminder(reminders);
    }

    @Override
    public String getBotUsername() {
        return "!!!Дежурство!!!";
    }

    @Override
    public String getBotToken() {
        return "701533339:AAGZpsNPVAblKnbq_kxkYOcTMX3F4gJKlSg";
    }
}
