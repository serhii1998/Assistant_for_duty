package com.attendant.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Human {

    private long id;
    private long chat_id;
    private String name;
    private String phoneNumber;
    private ArrayList<String> dateAttendant;
    private Room room;

}
