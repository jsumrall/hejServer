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
    JSONObject message;


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
            InputStream input  = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
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
            }
            catch(JSONException e){e.printStackTrace();}
            System.out.println(message);


            if(intent.equals(NEW_ACCOUNT)){
                //System.out.println("Add new user request");
                if(this.dbUtil.UserNameIsAvailable(username,password,regid)){
                    //add user to DB
                    output.write(("New User added: " + username + "\n").getBytes());
                }
                else{
                    output.write(("Username not available: " + username + "\n").getBytes());
                }
                output.flush();
            }

            if(intent.equals(VALIDATE_USER_NAME)){
                //System.out.println("validate username request");
                if(this.dbUtil.validateUserNamePasswordGCM(username,password,regid)){
                    //System.out.println("User: " + request[1] + ", Validated ");
                    output.write(("valid" + "\n").getBytes());
                }
                else{
                    //System.out.println("User: " + request[1]+ ", Invalid Credentials");
                    output.write(("invalid" + "\n").getBytes());
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
                    //System.out.println("User: " + request[3] + ", exists ");
                    output.write(("valid" + "\n").getBytes());
                }
                else{
                    //System.out.println("User: " + request[3]+ ", non-existent");
                    output.write(("invalid" + "\n").getBytes());
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
}
