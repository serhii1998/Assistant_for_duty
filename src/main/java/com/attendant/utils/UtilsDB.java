package com.attendant.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UtilsDB {

    private final String USERNAME = "npuzgpmqumbnlt";
    private final String PASSWORD = "07cf879928c6163797018e61397b2ecdbdfe2b6731ad51ac64d613b039c74d13";
    private final String URL = "jdbc:postgresql://ec2-79-125-4-96.eu-west-1.compute.amazonaws.com:5432/de2b5g3itjb4ku";

    public Connection dataConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection connection =  DriverManager.getConnection(
                URL,USERNAME, PASSWORD);
        return connection;
    }
}
