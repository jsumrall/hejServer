package hejserver;
import java.util.Scanner;
import hejserver.DatabaseUtils;
/**
 * Created by max on 6/23/14.
 */

public class Main {

    public static void main(String[] args){
        int port = 8000;
        DatabaseUtils dbUtil = new DatabaseUtils();
        ThreadPooledServer server = new ThreadPooledServer(port, dbUtil);
        Thread t = new Thread(server);
        t.start();

        Scanner reader = new Scanner(System.in);
        //DBCollection table = db.getCollection("user");
        System.out.println("Server Started on port: " + port);

        String clInput = "";
        while(!clInput.equals("stop")){
            clInput = reader.next().trim().toLowerCase();
            if(clInput.equals("stop")){
                System.out.println("Stopping Server");
                server.stop();
            }
        }

        //server.stop();
    }
}

