/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.attendant;

import com.attendant.telegramBotAssistant.TelegramBotAssistant;
import com.attendant.threads.ReminderThread;
import com.attendant.threads.UpTimeThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.GregorianCalendar;

//
//@Slf4j
//@Configuration
@SpringBootApplication
public class Main extends SpringBootServletInitializer {

//    public static void main(String[] args) {
//        SpringApplication.run(Main.class, args);
//        log.info("test start");
//        ApiContextInitializer.init();
//        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
//        try {
//            telegramBotsApi.registerBot(new TelegramBotAssistant());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        Thread upTimeThread = new Thread(new UpTimeThread(), "UpTimeThread");
//        upTimeThread.start();
//
//        ReminderThread reminderThread = new ReminderThread("ReminderThread");
//        reminderThread.start();
//
//    }

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

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Main.class);
    }
}
