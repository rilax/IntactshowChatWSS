package com.intactshow.chat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;


public class ApiClient {

    private static final String url = "jdbc:mysql://"+Settings.DB_HOST_VALUE+":"+Settings.DB_PORT_VALUE+"/"+Settings.DB_BASE_VALUE+"?useUnicode=true&characterEncoding=utf8";
    private static final String user = Settings.DB_USER_VALUE;
    private static final String password = Settings.DB_PASSWORD_VALUE;

    public static Connection con;
    //private static Statement stmt;

    public ApiClient(){
        dbConnect();

    }

    public static void dbConnect(){
        try{
            con = DriverManager.getConnection(url, user, password);
            Timer timer = new Timer();
            timer.schedule(new BaseConnectTimer(), 10*1000, 10*1000);
        }catch (SQLException ex){
            ex.printStackTrace();
        }
    }

    public static void dbDisconnect(){
        try {
            con.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void apiMessage(WSClient client, String json){
        //dbConnect();
        System.out.println(json);
        JSONParser parser = new JSONParser();
        try {

            JSONObject jsonObject = (JSONObject) parser.parse(json);
            switch ((String) jsonObject.get("section")){
                case "authorization":
                    JSONObject response = new JSONObject();
                    response.put("section", "authorization");

                    if(checkAuthorization((JSONObject)jsonObject.get("data"))){
                        JSONObject data = (JSONObject)jsonObject.get("data");
                        client.isAuth = true;
                        client.userID =  Integer.parseInt((String) data.get("userID"));

                        response.put("status", true);
                    }else{
                        response.put("status", false);
                    }
                    client.client.send(response.toJSONString());

                    for(int i = 0; i < WSServer.client.size(); i++){
                        if(!WSServer.client.get(i).equals(client)){
                            response = new JSONObject();
                            response.put("section", "userOnline");
                            response.put("userID", client.userID);
                            WSServer.client.get(i).client.send(response.toJSONString());
                        }
                    }
                    break;

                case "getDialogList":
                    String responseString = getDialogList(client.userID);
                    if(!responseString.equals(null)){
                        client.client.send(responseString);
                    }
                    break;

                case "getMessageList":
                    String responseMessage = gedMessageList((JSONObject)jsonObject.get("data"), client.userID, 0);
                    if(!responseMessage.equals(null)){
                        client.client.send(responseMessage);
                    }
                    break;

                case "addMessage":
                    addMessage((JSONObject)jsonObject.get("data"), client.userID);
                    break;
                case "addMessageStiker":
                    addMessageSticker((JSONObject)jsonObject.get("data"), client.userID);
                    break;
                case "getTyping":
                    getTyping((JSONObject)jsonObject.get("data"));
                    break;
                case "clearNewMessage":
                    JSONObject dataClear = (JSONObject)jsonObject.get("data");
                    int dialogID = (int)(long)dataClear.get("dialogID");
                    client.client.send(clearNewMessage(dialogID, client.userID));
                    break;
            }
        } catch (ParseException e) {
            e.printStackTrace();

        }
        //dbDisconnect();
    }

    private String clearNewMessage(int dialogID, int userID){
        JSONObject response = new JSONObject();
        response.put("section", "clearNewMessage");
        response.put("dialogID", dialogID);

        String sqlClear = "UPDATE `ph_chat_message` SET\n" +
                          "`view_message` = 0\n" +
                          "WHERE\n" +
                          "`dialog_id` = "+dialogID+" AND\n" +
                          "`sender_id` <> "+userID+" AND\n" +
                          "`view_message` = 1";
        try {
            Statement stmtClear = con.createStatement();
            stmtClear.executeUpdate(sqlClear);
            response.put("result", true);
        } catch (SQLException e) {
            response.put("result", false);
            e.printStackTrace();
        }
        return response.toJSONString();
    }

    private boolean checkAuthorization(JSONObject data){
        String userID = (String) data.get("userID");
        String identif = (String) data.get("identif");
        boolean result = false;

        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(pu.id) AS cnt FROM ph_users pu WHERE pu.id = "+userID+" AND pu.password = '"+identif+"'");
            rs.next();
            if(rs.getInt("cnt") == 1){
                result = true;
            }
            rs.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    private void getTyping(JSONObject data){
        Long dialogID = (Long)data.get("dialogID");
        Long userFrom = (Long)data.get("userFrom");

        JSONObject response = new JSONObject();
        response.put("section", "getTyping");
        response.put("dialogID", dialogID);

        /*
        WSClient client = WSServer.getClient(userFrom.intValue());
        if(client != null){
            client.client.send(response.toJSONString());
        }
        */
        List<WSClient> clients = WSServer.getClients(userFrom.intValue());
        if(clients.size() > 0){
            for(int i = 0; i < clients.size(); i++){
                clients.get(i).client.send(response.toJSONString());
            }
        }

    }

    private void addMessageSticker(JSONObject data, int userID){
        Long dialogID = (Long)data.get("dialogID");
        Long userFrom = (Long)data.get("userFrom");
        String message = (String)data.get("message");

        String sqlAddMesage = "INSERT INTO ph_chat_message (message, sender_id, dialog_id, status, view_message) VALUES ('"+message+"', "+userID+", "+dialogID+", 1, 1)";

        try {
            PreparedStatement statement = con.prepareStatement(sqlAddMesage, Statement.RETURN_GENERATED_KEYS);
            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            int lastID = 0;
            if(resultSet.next()){
                lastID = resultSet.getInt(1);
            }

            JSONObject response = new JSONObject();

            response.put("dialogID", dialogID);
            response.put("offset", 0L);
            response.put("limit", 1L);

            /*
            WSClient client = WSServer.getClient(userFrom.intValue());
            if(!client.equals(null)){
                client.client.send(gedMessageList(response, userFrom.intValue(), lastID));
            }
            client = WSServer.getClient(userID);
            if(!client.equals(null)){
                client.client.send(gedMessageList(response, userID, lastID));
            }
            */
            List<WSClient> clients = WSServer.getClients(userFrom.intValue());
            if(clients.size() > 0){
                for(int i = 0; i < clients.size(); i++){
                    clients.get(i).client.send(gedMessageList(response, userFrom.intValue(), lastID));
                }
            }
            clients = WSServer.getClients(userID);
            if(clients.size() > 0){
                for(int i = 0; i < clients.size(); i++){
                    clients.get(i).client.send(gedMessageList(response, userID, lastID));
                }
            }
        }catch (SQLException ex){
            ex.printStackTrace();
        }

    }

    private void addMessage(JSONObject data, int userID){

        Long dialogID = (Long)data.get("dialogID");
        Long userFrom = (Long)data.get("userFrom");
        String message = (String)data.get("message");

        String sqlAddMesage = "INSERT INTO ph_chat_message (message, sender_id, dialog_id, view_message) VALUES ('"+message+"', "+userID+", "+dialogID+", 1)";
        try {
            PreparedStatement statement = con.prepareStatement(sqlAddMesage, Statement.RETURN_GENERATED_KEYS);
            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            int lastID = 0;
            if(resultSet.next()){
                lastID = resultSet.getInt(1);
            }

            JSONObject response = new JSONObject();

            response.put("dialogID", dialogID);
            response.put("offset", 0L);
            response.put("limit", 1L);

            /*
            WSClient client = WSServer.getClient(userFrom.intValue());
            if(client != null){
                client.client.send(gedMessageList(response, userFrom.intValue(), lastID));
            }
            client = WSServer.getClient(userID);
            if(client != null){
                client.client.send(gedMessageList(response, userID, lastID));
            }
            */

            List<WSClient> clients = WSServer.getClients(userFrom.intValue());
            if(clients.size() > 0){
                for(int i = 0; i < clients.size(); i++){
                    clients.get(i).client.send(gedMessageList(response, userFrom.intValue(), lastID));
                }
            }
            clients = WSServer.getClients(userID);
            if(clients.size() > 0){
                for(int i = 0; i < clients.size(); i++){
                    clients.get(i).client.send(gedMessageList(response, userID, lastID));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.out.println(throwables.getMessage());
        }
    }

    private String gedMessageList(JSONObject data, int userID, int id){

        Long dialogID = (Long)data.get("dialogID");
        Long offset = (Long)data.get("offset");
        Long limit = (Long)data.get("limit");

        JSONObject response = new JSONObject();

        String lastMessageText = "";
        String lastMessageDate = "";
        int lastMessageStatus = 0;

        String sqlMessages = "";

        if(id > 0){
            sqlMessages = "SELECT\n" +
                    "  pcm.id, pcm.message, pcm.sender_id, pcm.dialog_id, pcm.status,\n" +
                    "  IF(\n" +
                    "    DATE_FORMAT(pcm.date_create, '%d.%m.%Y') = DATE_FORMAT(NOW(), '%d.%m.%Y'),\n" +
                    "    DATE_FORMAT(pcm.date_create, '%H:%i:%s'),\n" +
                    "    DATE_FORMAT(pcm.date_create, '%d.%m.%Y %H:%i:%s')\n" +
                    "  ) AS date_create\n" +
                    "FROM ph_chat_message pcm \n" +
                    "WHERE \n" +
                    "  pcm.dialog_id = "+dialogID+" AND\n" +
                    "  pcm.id = "+id+" \n" +
                    "  ORDER BY pcm.date_create LIMIT "+offset+", "+limit;
        }else{
            sqlMessages = "SELECT\n" +
                    "  pcm.id, pcm.message, pcm.sender_id, pcm.dialog_id, pcm.status, pcm.view_message,\n" +
                    "  IF(\n" +
                    "    DATE_FORMAT(pcm.date_create, '%d.%m.%Y') = DATE_FORMAT(NOW(), '%d.%m.%Y'),\n" +
                    "    DATE_FORMAT(pcm.date_create, '%H:%i:%s'),\n" +
                    "    DATE_FORMAT(pcm.date_create, '%d.%m.%Y %H:%i:%s')\n" +
                    "  ) AS date_create\n" +
                    "FROM ph_chat_message pcm \n" +
                    "WHERE \n" +
                    "  pcm.dialog_id = "+dialogID+" \n" +
                    "  ORDER BY pcm.id ";
        }

        try {
            ResultSet resMessages = con.createStatement().executeQuery(sqlMessages);

            response.put("section", "getMessageList");
            response.put("userID", userID);
            response.put("dialogID", dialogID);
            JSONArray messageItem = new JSONArray();

            while (resMessages.next()){
                JSONObject message = new JSONObject();
                message.put("id",resMessages.getInt("id"));

                message.put("message", resMessages.getString("message"));
                message.put("senderID", resMessages.getInt("sender_id"));
                message.put("dateCreate", resMessages.getString("date_create"));
                message.put("status", resMessages.getInt("status"));

                lastMessageText = resMessages.getString("message");
                lastMessageStatus = resMessages.getInt("status");
                lastMessageDate = resMessages.getString("date_create");

                messageItem.add(message);
            }

            switch (lastMessageStatus){
                case 1:
                    lastMessageText = "Sticker";
                    break;
                case 2:
                    lastMessageText = "File";
                    break;
            }

            if(lastMessageText.length() > 15){
                lastMessageText = lastMessageText.substring(0,15) + "...";
            }

            response.put("lastMessageText", lastMessageText);
            response.put("lastMessageStatus", lastMessageStatus);
            response.put("lastMessageDate", lastMessageDate);

            response.put("countNewMessage", getCountNewMessage((int)(long)dialogID, userID));
            response.put("getCountMessage", messageItem.size());
            response.put("data", messageItem);
        }catch (SQLException ex){
            ex.printStackTrace();
        }
        return response.toJSONString();


    }

    private String getDialogList (int idUser){

        try {
            Statement stmt = con.createStatement();
            String sql = "SELECT `ph_chat_dialog`.*\n" +
                    "FROM `ph_chat_dialog`\n" +
                    "    LEFT JOIN `ph_chat_user_to_dialog` ON `ph_chat_user_to_dialog`.`dialog_id` = `ph_chat_dialog`.`id`\n" +
                    "WHERE `ph_chat_user_to_dialog`.`user_id` = " + idUser;
            ResultSet rs = stmt.executeQuery(sql);

            JSONObject obj = new JSONObject();
            obj.put("section", "setDialogList");

            JSONArray listD = new JSONArray();



            while (rs.next()){

                String sqlDialogItem = "SELECT `ph_user_attributes`.`id`,\n" +
                        "  `ph_user_attributes`.`internalKey`,\n" +
                        "  `ph_user_attributes`.`fullname`\n" +
                        "FROM `ph_chat_user_to_dialog` \n" +
                        "  LEFT JOIN `ph_user_attributes` ON `ph_chat_user_to_dialog`.`user_id` = `ph_user_attributes`.`internalKey`\n" +
                        "WHERE \n" +
                        "  `ph_chat_user_to_dialog`.`dialog_id` = "+ rs.getInt("id") +" AND\n" +
                        "  `ph_user_attributes`.`internalKey` <> " + idUser;

                Statement stmtDialogItem = con.createStatement();
                ResultSet resDialogItem = stmtDialogItem.executeQuery(sqlDialogItem);
                while (resDialogItem.next()){
                    JSONObject itemD = new JSONObject();

                    itemD.put("idDialog", rs.getInt("id"));
                    itemD.put("dialogName", resDialogItem.getString("fullname"));
                    itemD.put("userToID", resDialogItem.getInt("internalKey"));
                    itemD.put("online", WSServer.getUserStatus(resDialogItem.getInt("internalKey")));

                    String sqlMessage = "SELECT \n" +
                            "  pcm.message,\n" +
                            "  pcm.status,\n" +
                            "  IF(\n" +
                            "    DATE_FORMAT(pcm.date_create, '%d.%m.%Y') = DATE_FORMAT(NOW(), '%d.%m.%Y'),\n" +
                            "    DATE_FORMAT(pcm.date_create, '%H:%i:%s'),\n" +
                            "    IF(\n" +
                            "      DATE_FORMAT(pcm.date_create, '%Y') = DATE_FORMAT(NOW(), '%Y'),\n" +
                            "      DATE_FORMAT(pcm.date_create, '%d.%m.%Y'),\n" +
                            "      DATE_FORMAT(pcm.date_create, '%Y')\n" +
                            "    )\n" +
                            "  ) AS DateCreate\n" +
                            "FROM \n" +
                            "  ph_chat_message pcm\n" +
                            "WHERE\n" +
                            "  pcm.dialog_id = "+ rs.getInt("id") +" \n" +
                            "  ORDER BY pcm.date_create DESC LIMIT 1";

                    Statement stmtMessage = con.createStatement();
                    ResultSet resMessage = stmtMessage.executeQuery(sqlMessage);

                    String dateCreate = "";
                    String lastMessage = "";
                    int status = 0;

                    while (resMessage.next()){
                        lastMessage = resMessage.getString("message");
                        dateCreate = resMessage.getString("DateCreate");
                        status = resMessage.getInt("status");
                    }

                    itemD.put("countNewMessage", getCountNewMessage(rs.getInt("id"), idUser));

                    switch (status){
                        case 1:
                            lastMessage = "Sticker";
                            break;
                        case 2:
                            lastMessage = "File";
                            break;
                    }

                    itemD.put("lastMessage", lastMessage);
                    itemD.put("lastMessageDate", dateCreate);

                    listD.add(itemD);
                }
            }
            obj.put("data", listD);

            return  obj.toJSONString();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private int getCountNewMessage(int dialogID, int userID){
        int count = 0;
        String sqlCountMessage = "SELECT COUNT(id) AS cnt FROM `ph_chat_message` WHERE `dialog_id` = "+dialogID+" AND `sender_id` <> "+userID+" AND `view_message` = 1";
        try {
            Statement stmtMessage = con.createStatement();
            ResultSet resCountMessage = stmtMessage.executeQuery(sqlCountMessage);
            while (resCountMessage.next()){
                count = resCountMessage.getInt("cnt");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  count;
    }

    public static void exitUser(WSClient client){
        for(int i = 0; i < WSServer.client.size(); i++){
            if(!WSServer.client.get(i).equals(client)){
                JSONObject response = new JSONObject();
                response.put("section", "userOffline");
                response.put("userID", client.userID);
                WSServer.client.get(i).client.send(response.toJSONString());
            }
        }
    }
}
