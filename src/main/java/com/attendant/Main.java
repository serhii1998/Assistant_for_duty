package com.attendant;

import com.attendant.telegramBotAssistant.TelegramBotAssistant;
import com.attendant.threads.ReminderThread;
import com.attendant.threads.UpTimeThread;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import javax.annotation.PostConstruct;


@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @PostConstruct
    public void init() {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new TelegramBotAssistant());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread upTimeThread = new Thread(new UpTimeThread(), "UpTimeThread");
        upTimeThread.start();

        ReminderThread reminderThread = new ReminderThread("ReminderThread");
        reminderThread.start();
    }
}
