/**
 * Created by max on 6/23/14.
 */
//package servers;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;


public class WorkerRunnable implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText   = null;
    public DatabaseUtils dbUtil = null;

    public WorkerRunnable(Socket clientSocket, String serverText, DatabaseUtils dbUtils) {
        this.dbUtil = dbUtils;
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
    }

    public void run() {
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
            request = BR.readLine().split(",");
            System.out.println("Request Received");
            if(request.length < 2){
                output.write(new String("Malformed Request: " + request + "\n").getBytes());
                return; // this feels wrong. I want to return and kill this thread.
            }
            String requestType = request[0];
            request[1] = request[1].trim().toUpperCase();
            request[2] = request[2].trim();
            if(request.length == 4){request[3] = request[3].trim().toUpperCase();}

            if(requestType.equals("addNewUser")){
                System.out.println("Add new user request");
                if(this.dbUtil.UserNameIsAvailable(request[1], request[2])){
                    //add user to DB
                    output.write(new String("New User added: " + request[1] + "\n").getBytes());
                }
                else{
                    output.write(new String("Username not available: " + request[1] + "\n").getBytes());
                }
                output.flush();
            }

            else if(requestType.equals("sendHej")){
                System.out.println("Send Hej request");
                if(this.dbUtil.validateUserNamePassword(request[1], request[2])){
                    System.out.println("User: " + request[1] + ", Validated ");
                    this.dbUtil.processHej(request[3],request[1]);
                }
                else{
                    System.out.println("User: " + request[1]+ ", Invalid Credentials");
                }
            }

            else if(requestType.equals("checkForHejs")){
                System.out.println("Check for Hej request");
                if(this.dbUtil.validateUserNamePassword(request[1], request[2])){
                    String hejs = this.dbUtil.reteriveHejs(request[1]);
                    System.out.println(hejs);
                    output.write(new String(hejs + "\n").getBytes());

                }
            }

            else if(requestType.equals("validateUsername")){
                System.out.println("validate username request");
                if(this.dbUtil.validateUserNamePassword(request[1], request[2])){
                    System.out.println("User: " + request[1] + ", Validated ");
                    output.write(new String("valid" + "\n").getBytes());
                }
                else{
                    System.out.println("User: " + request[1]+ ", Invalid Credentials");
                    output.write(new String("invalid" + "\n").getBytes());
                }
            }

            else if(requestType.equals("checkForUsername")){
                System.out.println("check username request");
                if(this.dbUtil.userExists(request[1])){
                    System.out.println("User: " + request[1] + ", exists ");
                    output.write(new String("valid" + "\n").getBytes());
                }
                else{
                    System.out.println("User: " + request[1]+ ", non-existent");
                    output.write(new String("invalid" + "\n").getBytes());
                }
            }


            System.out.println(responseStrBuilder);

            output.close();
            input.close();
            System.out.println("Request processed: " + this.serverText);
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }
}
