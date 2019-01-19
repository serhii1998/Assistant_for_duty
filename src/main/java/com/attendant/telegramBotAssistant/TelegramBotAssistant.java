package com.attendant.telegramBotAssistant;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static com.attendant.utils.UtilsSpreadsheet.*;
import static com.attendant.utils.UtilsDB.*;

public class TelegramBotAssistant extends TelegramLongPollingBot {

    private boolean createReminder = false; // флаг, означающий, что пользователь хочет установить напоминание

    @Override
    public void onUpdateReceived(Update update) {
        String message = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        switch (message) {
            case "/start":
                message = "Привет! Напиши номер комнаты, дату дежурства которой хочешь узнать";
                sendMsg(message, chatId);
                break;
            case "Создать напоминание":
                message = "Введите комнату на которую хотите создать напоминание";
                createReminder = true;
                sendMsg(message, chatId);
                break;
            default:
                System.out.println("1");
                sendMsgSearchDateDuty(chatId, message);
                break;
        }

    }

    private synchronized void sendMsg(String message, String chatId) {
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private synchronized void sendMsgSearchDateDuty(String chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);

        try {
            if (checkExistenceThisRoomAndSetDateDutyToSendMessage(message, sendMessage) && !createReminder) {
                if (sendMessage.getText().trim().equals("")) {
                    execute(sendMessage.setText("В графике пока нет даты следующего дежурства." +
                            " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));
                } else {
                    execute(sendMessage);
                }
            } else if (!createReminder) {// здесь нужно это условие, что бы в случае установки напоминание бот не попадал в этот else
                execute(sendMessage.setText("Такой комнаты нет в графике"));
            }

            if (checkExistenceThisRoomAndSetDateDutyToSendMessage(message, sendMessage) && createReminder) {

                String dateDutyFromSendMessage = sendMessage.getText();// по моей логике, тут должна быть дата дежурства, которая добавилась из метода checkExistenceThisRoomAndSetDateDutyToSendMessage.
                String room = message;

                createReminder = false; // флаг. когда он в инверсии, это значит, что установки напоминания небыло.
                // Пользователь просто хочет узнать дату дежурства
                if (dateDutyFromSendMessage.trim().equals("")) {
                    try {// в таблице нету след даты дежурства, но напоминание все равно должно быть установлено
                        workDB(chatId, room, dateDutyFromSendMessage);
                        execute(sendMessage.setText("Напоминание создано. пока следующей даты дежурства нету. " +
                                "Бот будет предупреждать о наступлении дежурства в течении 3-х дней перед " +
                                "дежурством включая день дежурства." +
                                " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));

                    } catch (Exception e) {
                        execute(sendMessage.setText("Что-то пошло не так( мы работаем над устранением неисправности"));
                        System.out.println("catch create reminder in sendMsgSearchDateDuty");
                        e.printStackTrace();
                    }

                } else {
                    try { // все ок, комната найдена и напоминание установлено
                        workDB(chatId, room, dateDutyFromSendMessage);
                        execute(sendMessage.setText("Напоминание создано. Бот напомнит о дежурстве в течении " +
                                "3 дней до наступления дежурства включая день дежурства."));
                    } catch (TelegramApiException e) {
                        execute(sendMessage.setText("Что-то пошло не так"));
                        System.out.println("sendMsgSearchDateDuty catch create reminder");
                        e.printStackTrace();
                    }
                }
            } else if (createReminder) {
                execute(sendMessage.setText("Такой комнаты нет в графике")); // если такой комнаты не существует
            }

        } catch (Exception e) {
            System.out.println("sendMsgSearchDateDuty catch");
            e.printStackTrace();
        }
    }

    private void workDB(String chatId, String room, String dateDutyFromSendessage) {
        /*
         * давай поразсуждаем. вот я уже определил, что эта комната существует в гугл таблице. все ок.
         * но пользователь хочет установить напоинание. Для этого:
         * 1. он должен ввести свою комнату.
         * 2. я эту комнату запоминаю, запоминаю chatid, с которого пришел запрос на напоминание.
         * 3. нахожу дату дежурства, когда надо ему напомнить.
         * 4. запоминаю эту дату и записываю ее в базу к тому чату, от которого поступил запрос
         * 5. потом у меня будет поток, который будет запускаться 1 раз в сутки, сканировать базу данных
         * 6. он будет искать даты в ML? сравнивать с текущей.
         * 7. если текущая дата на 2 меньше, чем в бд, то нужно отослать напоминание тому чату,
         * с которого поступил запрос на напоминание
         * 8. собственно вроде бы все. */

        // находим целевую дату дежурства с помощью слова "дежурит"
        String subString = "дежурит";
        String dateDuty = dateDutyFromSendessage.substring(dateDutyFromSendessage.indexOf(subString) + subString.length()).trim();
        System.out.println(dateDuty);
        try (Connection connection = dataConnection()) {
            PreparedStatement prepareStatement = connection.prepareStatement("select chat_id from reminder_for_duty where chat_id = ?");
            prepareStatement.setString(1, chatId);
            ResultSet resultSetChatId = prepareStatement.executeQuery();

            if (resultSetChatId.next()) {

                prepareStatement = connection.prepareStatement("update reminder_for_duty set number_room = ?, date_duty = ? where chat_id = ?");
                prepareStatement.setString(1, room);
                prepareStatement.setString(2, dateDuty);
                prepareStatement.setString(3, resultSetChatId.getString("chat_id"));
                prepareStatement.executeUpdate();

            } else {

                PreparedStatement preparedStatement = connection.prepareStatement("insert into reminder_for_duty (chat_id, number_room, date_duty) values (?,?,?)");
                preparedStatement.setString(1, chatId);
                preparedStatement.setString(2, room);
                preparedStatement.setString(3, dateDuty.trim());
                System.out.println(dateDuty.length() + "///////////////////////////////////////////////" + dateDuty.toString());

                preparedStatement.execute();
            }
            resultSetChatId.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public synchronized void setButtons(SendMessage sendMessage) {
        //Создаем клавиуатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        KeyboardButton keyboardButton = new KeyboardButton("Создать напоминание");
        KeyboardButton keyboardButton2 = new KeyboardButton("222222");

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(keyboardButton);
        keyboardRow.add(keyboardButton2);

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(keyboardRow);

        replyKeyboardMarkup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }

    @Override
    public String getBotUsername() {
        return "Assistant_for_duty";
    }

    @Override
    public String getBotToken() {
        return "701533339:AAGZpsNPVAblKnbq_kxkYOcTMX3F4gJKlSg";
    }

}
