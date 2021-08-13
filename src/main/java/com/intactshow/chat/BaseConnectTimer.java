package com.intactshow.chat;

import java.sql.SQLException;
import java.util.TimerTask;

public class BaseConnectTimer extends TimerTask {
    @Override
    public void run() {
        try{
            if (!ApiClient.con.createStatement().execute("SELECT NOW()")){
                ApiClient.dbConnect();
            }
        }catch (SQLException ex){
            ApiClient.dbConnect();
            ex.printStackTrace();
        }


    }
}
