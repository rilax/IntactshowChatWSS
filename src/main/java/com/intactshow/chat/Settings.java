package com.intactshow.chat;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Settings {
    public static String WS_HOST_VALUE;
    public static int WS_PORT_VALUE;

    public static String DB_HOST_VALUE;
    public static int DB_PORT_VALUE;
    public static String DB_USER_VALUE;
    public static String DB_PASSWORD_VALUE;
    public static String DB_BASE_VALUE;

    public Settings(String urlToFileConfig) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(new File(urlToFileConfig)));
        WS_HOST_VALUE = props.getProperty("WS_HOST_VALUE", "localhost");
        WS_PORT_VALUE = Integer.valueOf(props.getProperty("WS_PORT_VALUE", "8887"));
        DB_HOST_VALUE = props.getProperty("DB_HOST_VALUE", "localhost");
        DB_PORT_VALUE = Integer.valueOf(props.getProperty("DB_PORT_VALUE", "3306"));
        DB_USER_VALUE = props.getProperty("DB_USER_VALUE", "root");
        DB_PASSWORD_VALUE = props.getProperty("DB_PASSWORD_VALUE", "");
        DB_BASE_VALUE = props.getProperty("DB_BASE_VALUE", "host1763385_intac");
    }

}
