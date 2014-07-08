package hejserver;

import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.LinkedList;

/**
 * Created by max on 6/23/14.
 */
public class DatabaseUtils {

    MongoClient mongoClient = null;
    DB db = null;
    DBCollection coll;

    public DatabaseUtils() {
        try {
            this.mongoClient = new MongoClient("localhost", 27017);

            this.db = mongoClient.getDB("test");
            coll = db.getCollection("testCollection");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    public boolean UserNameIsAvailable(String username, String password, String gcmid){
        BasicDBObject doc = new BasicDBObject("name", username);
                //.append("password", password)
                //.append("hej", "");
        DBCursor cursor = this.coll.find(doc);
        if(cursor.size() == 0){
            this.coll.insert(doc.append("password", password)
                    .append("hej","")
            .append("gcmid", gcmid));
            System.out.println("New User Registered: " + username);
            return true;
        }
        if(cursor.hasNext()) {
            System.out.println(cursor.next());
        }
        return false;
    }


    public boolean validateUserNamePasswordGCM(String username, String password, String gcmid){
        BasicDBObject doc = new BasicDBObject("name", username)
                .append("password", password);
        DBCursor cursor = this.coll.find(doc);
        BasicDBObject updatedWithGCMID = new BasicDBObject();
        updatedWithGCMID.append("$set",new BasicDBObject().append("gcmid", gcmid));
        if(cursor.size() == 1){
            this.coll.update(doc, updatedWithGCMID);
            return true;
        }
        return false;
    }
    public boolean validateUserNamePassword(String username, String password){
        BasicDBObject doc = new BasicDBObject("name", username)
                .append("password", password);
        DBCursor cursor = this.coll.find(doc);
        if(cursor.size() == 1){
            return true;
        }
        return false;
    }

    public boolean userExists(String username){
        BasicDBObject doc = new BasicDBObject("name", username);
        DBCursor cursor = this.coll.find(doc);
        if(cursor.size() == 1){
            return true;
        }
        return false;
    }

    public boolean processHej(String target, String sender){
        BasicDBObject newHej = new BasicDBObject();
        newHej.append("$set", new BasicDBObject().append("hej", sender));

        BasicDBObject searchQuery = new BasicDBObject().append("name", target);

        this.coll.update(searchQuery, newHej);
         new GCMMessage(getGcmID(target), sender);
        return true;
    }



    public String reteriveHejs(String user){
        BasicDBObject searchQuery = new BasicDBObject().append("name", user);
        DBCursor cursor = this.coll.find(searchQuery);
        String result = "";
        if(cursor.hasNext()) {
            Object hejs = cursor.next().get("hej");
            result = hejs.toString();
        }

        BasicDBObject newHej = new BasicDBObject();
        newHej.append("$set", new BasicDBObject().append("hej", ""));
        this.coll.update(searchQuery, newHej);        //remove the hej now that we have it.



        return result;
    }


    private String getGcmID(String user){
        BasicDBObject searchQuery = new BasicDBObject().append("name", user);
        DBCursor cursor = this.coll.find(searchQuery);
        String gcmid = "";
        if(cursor.hasNext()) {
            Object id = cursor.next().get("gcmid");
            gcmid = id.toString();
        }
        return gcmid;
    }


}




