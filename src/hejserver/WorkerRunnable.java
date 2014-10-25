package hejserver; /**
 * Created by max on 6/23/14.
 */
//package servers;

import org.json.JSONException;
import org.json.JSONObject;


import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;


public class WorkerRunnable implements Runnable{

    protected SSLSocket clientSocket = null;
    protected String serverText   = null;
    public DatabaseUtils dbUtil = null;
    public static final String NEW_ACCOUNT = "addNewUser";
    public static final String UPDATE_REGID = "updateregid";
    public static final String VALIDATE_USER_NAME = "validateUsername";
    public static final String CHECK_FOR_USERNAME = "checkForUsername";
    public static final String SEND_HEJ = "sendHej";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";
    public static final String AD = "AD";
    JSONObject message;
    InputStream input;
    OutputStream output;


    public WorkerRunnable(SSLSocket clientSocket, String serverText, DatabaseUtils dbUtils) {
        this.dbUtil = dbUtils;
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
    }


    public void run() {
        String username = "";
        String password = "";
        String target = "";
        String intent = "";
        String regid = "";
        try {
            this.input  = clientSocket.getInputStream();
            this.output = clientSocket.getOutputStream();
            //long time = System.currentTimeMillis();
            /*output.write(("HTTP/1.1 200 OK\n\nWorkerRunnable: " +
                    this.serverText + " - " +
                    time +
                    "").getBytes());*/
            BufferedReader BR = new BufferedReader(new InputStreamReader(input));
            StringBuilder responseStrBuilder = new StringBuilder();

            String[] request;
            try {
                message = new JSONObject(BR.readLine());
                username = message.getString("username").toUpperCase();
                password = message.getString("password");
                target = message.getString("target").toUpperCase();
                intent = message.getString("intent");
                regid = message.getString("regid");
                System.out.println(message.toString(1).replaceAll("\"password\": \".*\\n", ""));
            }
            catch(JSONException e){e.printStackTrace();}



            if(intent.equals(NEW_ACCOUNT)){
                //System.out.println("Add new user request");
                if(this.dbUtil.checkGCMID(regid) && this.dbUtil.UserNameIsAvailable(username,password,regid)){
                    //add user to DB
                    respondSuccess();
                }
                else{
                    respondFail();
                }
                output.flush();
            }

            if(intent.equals(VALIDATE_USER_NAME)){
                //System.out.println("validate username request");
                if(this.dbUtil.validateUserNamePasswordGCM(username,password,regid)){
                    respondSuccess();
                }
                else{
                    respondFail();
                }
            }


            if(intent.equals(SEND_HEJ)){
                //System.out.println("Send Hej request");
                if(this.dbUtil.validateUserNamePassword(username,password)){
                    //System.out.println("User: " + request[1] + ", Validated ");
                    this.dbUtil.processHej(target,username);
                }

            }

            if(intent.equals(CHECK_FOR_USERNAME)){
                //System.out.println("check username request");
                if(this.dbUtil.userExists(target)){
                    respondSuccess();
                }
                else{
                    respondFail();
                }
            }
            if(intent.equals(AD)){
                System.out.println("AD =------");

                if(username.equals("specialADUser88943765".toUpperCase()) && password.equals("specialADPassword34756345")){
                    this.dbUtil.broadcastMessage(target);
                }
            }

            output.close();
            input.close();
            //System.out.println("Request processed: " + this.serverText);
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }
    private void respondSuccess(){
        try {
            this.output.write((SUCCESS + "\n").getBytes());
        }
        catch(Exception e){e.printStackTrace();}


    }
    private void respondFail(){
        try {
            this.output.write((FAIL + "\n").getBytes());

        }
        catch (Exception e){e.printStackTrace();}

    }
}
