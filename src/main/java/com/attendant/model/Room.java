package com.attendant.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Room {

    private long id;
    private ArrayList<Human> humans;
    private int numberRoom;

}
