package hejserver;

import com.mongodb.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.LinkedList;

/**
 * Created by max on 6/23/14.
 */
public class DatabaseUtils {

    MongoClient mongoClient = null;
    DB db = null;
    DBCollection coll;
    private String SALT = "SALT";
    private String SECUREPASSWORD = "SECUREPASSWORD";
    private String GCMID = "GCMID";
    private String USERNAME = "USERNAME";
    //private String HEJ = "HEJ";
    private static final int ITERATIONS = 1000;
    private static final int KEY_LENGTH = 192; // bits
    SecureRandom secureRandom;

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
        secureRandom = new SecureRandom();//for generating salt
        BasicDBObject doc = new BasicDBObject(USERNAME, username);
                //.append("password", password)
                //.append("hej", "");
        DBCursor cursor = this.coll.find(doc);
        if(cursor.size() == 0){
            //generate salt
            String salt = new BigInteger(130, secureRandom).toString();
            String securePassword = hashPassword(password, salt);
            this.coll.insert(doc.append(SECUREPASSWORD, securePassword)
                    .append(SALT,salt)
                    .append(GCMID, gcmid));
            System.out.println("New User Registered: " + username);
            return true;
        }
        if(cursor.hasNext()) {
            System.out.println(cursor.next());
        }
        return false;
    }


    public boolean validateUserNamePasswordGCM(String username, String password, String gcmid){
        BasicDBObject doc = new BasicDBObject(USERNAME, username);
        DBCursor cursor = this.coll.find(doc);
        BasicDBObject updatedWithGCMID = new BasicDBObject();
        updatedWithGCMID.append("$set",new BasicDBObject().append(GCMID, gcmid));
        if(cursor.hasNext()){
            DBObject result = cursor.next();
            String salt = result.get(SALT).toString();
            String securePassword = result.get(SECUREPASSWORD).toString();
            if(securePassword.equals(hashPassword(password,salt))){
                //user is authenticated
                this.coll.update(doc, updatedWithGCMID);
                return true;
            }
        }
        return false;
    }
    public boolean validateUserNamePassword(String username, String password) {
        BasicDBObject doc = new BasicDBObject(USERNAME, username);
        DBCursor cursor = this.coll.find(doc);
        if (cursor.hasNext()) {
             DBObject result = cursor.next();
            String salt = result.get(SALT).toString();
            String securePassword = result.get(SECUREPASSWORD).toString();
            if (securePassword.equals(hashPassword(password, salt))) {
                //user is authenticated
                return true;
            }
        }
        return false;
    }
    public boolean userExists(String username){
        BasicDBObject doc = new BasicDBObject(USERNAME, username);
        DBCursor cursor = this.coll.find(doc);
        if(cursor.size() == 1){
            return true;
        }
        return false;
    }

    public boolean processHej(String target, String sender){
        //BasicDBObject newHej = new BasicDBObject();
        //newHej.append("$set", new BasicDBObject().append("hej", sender));
        //BasicDBObject searchQuery = new BasicDBObject().append("name", target);
        //this.coll.update(searchQuery, newHej);
        GCMMessage msg = new GCMMessage(getGcmID(target), sender);
        Result result = msg.sendHej();
        return true;
    }

    public boolean checkGCMID(String newGCMID){
        GCMMessage msg = new GCMMessage(newGCMID, "Hej Server: Welcome to Hej!");
        Result result = msg.sendHej();
        if(result.getErrorCodeName() != null) {
            return false;
        }
        return true;

    }



    /*public String reteriveHejs(String user){
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
         */

    private String getGcmID(String user){
        BasicDBObject searchQuery = new BasicDBObject().append(USERNAME, user);
        DBCursor cursor = this.coll.find(searchQuery);
        String gcmid = "";
        if(cursor.hasNext()) {
            Object id = cursor.next().get(GCMID);
            gcmid = id.toString();
        }
        return gcmid;
    }


    public static String hashPassword(String password, String salt){
        char[] passwordChars = password.toCharArray();
        byte[] saltBytes = salt.getBytes();

        PBEKeySpec spec = new PBEKeySpec(
                passwordChars,
                saltBytes,
                ITERATIONS,
                KEY_LENGTH
        );
        try {
            SecretKeyFactory key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hashedPassword = key.generateSecret(spec).getEncoded();
            return String.format("%x", new BigInteger(hashedPassword));
        }
        catch(Exception e){e.printStackTrace(); System.exit(2);}//this should only happen if the key algo is missing, which would be catastrophic

        return null;//only if we crashed. Never run since there is system.exit() above.
    }

    public String getSalt(String username){
        BasicDBObject searchQuery = new BasicDBObject().append(USERNAME, username);
        DBCursor cursor = this.coll.find(searchQuery);
        String salt = "";
        if(cursor.hasNext()) {
            Object id = cursor.next().get(SALT);
            salt = id.toString();
        }
        return salt;

}

}




