import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

import java.net.UnknownHostException;

/**
 * Created by max on 6/23/14.
 */

public class Main {

    public static void main(String[] args){
        DatabaseUtils dbUtil = new DatabaseUtils();
        ThreadPooledServer server = new ThreadPooledServer(9000, dbUtil);
        Thread t = new Thread(server);
        t.start();


        //DBCollection table = db.getCollection("user");



        try {
            Thread.sleep(50 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping Server");
        server.stop();
    }
}

