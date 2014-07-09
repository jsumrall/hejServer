package hejserver;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

/**
 * Created by max on 7/6/14.
 */
import static hejserver.Constants.GCM_SEND_ENDPOINT;
import static hejserver.Constants.JSON_CANONICAL_IDS;
import static hejserver.Constants.JSON_ERROR;
import static hejserver.Constants.JSON_FAILURE;
import static hejserver.Constants.JSON_MESSAGE_ID;
import static hejserver.Constants.JSON_MULTICAST_ID;
import static hejserver.Constants.JSON_PAYLOAD;
import static hejserver.Constants.JSON_REGISTRATION_IDS;
import static hejserver.Constants.JSON_RESULTS;
import static hejserver.Constants.JSON_SUCCESS;
import static hejserver.Constants.PARAM_COLLAPSE_KEY;
import static hejserver.Constants.PARAM_DELAY_WHILE_IDLE;
import static hejserver.Constants.PARAM_DRY_RUN;
import static hejserver.Constants.PARAM_PAYLOAD_PREFIX;
import static hejserver.Constants.PARAM_REGISTRATION_ID;
import static hejserver.Constants.PARAM_RESTRICTED_PACKAGE_NAME;
import static hejserver.Constants.PARAM_TIME_TO_LIVE;
import static hejserver.Constants.TOKEN_CANONICAL_REG_ID;
import static hejserver.Constants.TOKEN_ERROR;
import static hejserver.Constants.TOKEN_MESSAGE_ID;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Helper class to send messages to the GCM service using an API Key.
 */
public class GCMMessage {

    protected static final String UTF8 = "UTF-8";

    /**
     * Initial delay before first retry, without jitter.
     */
    protected static final int BACKOFF_INITIAL_DELAY = 1000;
    /**
     * Maximum delay before a retry.
     */
    protected static final int MAX_BACKOFF_DELAY = 1024000;

    protected final Random random = new Random();
    protected static final Logger logger =
            Logger.getLogger(GCMMessage.class.getName());

    private final String key = "AIzaSyAFNCTdeyppDc3x5Y54nL5LUSB7kc12uuw";

    /**
     * Default constructor.
     *
     */
    public GCMMessage(String targetID, String sender) {

        Message message = new hejserver.Message.Builder()
                .addData("sender", sender)
                .build();
        try {
            Result result = sendNoRetry(message, targetID);
            //System.out.println(result);

            //SimpleFormatter fmt = new SimpleFormatter();
            //StreamHandler sh = new StreamHandler(System.out, fmt);
            //logger.addHandler(sh);
        }
        catch (Exception e){System.out.println("Problem with GCM"); e.printStackTrace();}
    }

    /**
     * Sends a message to one device, retrying in case of unavailability.
     *
     * <p>
     * <strong>Note: </strong> this method uses exponential back-off to retry in
     * case of service unavailability and hence could block the calling thread
     * for many seconds.
     *
     * @param message message to be sent, including the device's registration id.
     * @param registrationId device where the message will be sent.
     * @param retries number of retries in case of service unavailability errors.
     *
     * @return result of the request (see its javadoc for more details).
     *
     * @throws IllegalArgumentException if registrationId is {@literal null}.
     * @throws InvalidRequestException if GCM didn't returned a 200 or 5xx status.
     * @throws IOException if message could not be sent.
     */
    public Result send(Message message, String registrationId, int retries)
            throws IOException {
        int attempt = 0;
        Result result = null;
        int backoff = BACKOFF_INITIAL_DELAY;
        boolean tryAgain;
        do {
            attempt++;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Attempt #" + attempt + " to send message " +
                        message + " to regIds " + registrationId);
            }
            result = sendNoRetry(message, registrationId);
            tryAgain = result == null && attempt <= retries;
            if (tryAgain) {
                int sleepTime = backoff / 2 + random.nextInt(backoff);
                sleep(sleepTime);
                if (2 * backoff < MAX_BACKOFF_DELAY) {
                    backoff *= 2;
                }
            }
        } while (tryAgain);
        if (result == null) {
            throw new IOException("Could not send message after " + attempt +
                    " attempts");
        }
        return result;
    }

    /**
     * Sends a message without retrying in case of service unavailability. See
     * {@link #send(Message, String, int)} for more info.
     *
     * @return result of the post, or {@literal null} if the GCM service was
     *         unavailable or any network exception caused the request to fail.
     *
     * @throws InvalidRequestException if GCM didn't returned a 200 or 5xx status.
     * @throws IllegalArgumentException if registrationId is {@literal null}.
     */
    public Result sendNoRetry(Message message, String registrationId)
            throws IOException {
        StringBuilder body = newBody(PARAM_REGISTRATION_ID, registrationId);
        Boolean delayWhileIdle = message.isDelayWhileIdle();
        if (delayWhileIdle != null) {
            addParameter(body, PARAM_DELAY_WHILE_IDLE, delayWhileIdle ? "1" : "0");
        }
        Boolean dryRun = message.isDryRun();
        if (dryRun != null) {
            addParameter(body, PARAM_DRY_RUN, dryRun ? "1" : "0");
        }
        String collapseKey = message.getCollapseKey();
        if (collapseKey != null) {
            addParameter(body, PARAM_COLLAPSE_KEY, collapseKey);
        }
        String restrictedPackageName = message.getRestrictedPackageName();
        if (restrictedPackageName != null) {
            addParameter(body, PARAM_RESTRICTED_PACKAGE_NAME, restrictedPackageName);
        }
        Integer timeToLive = message.getTimeToLive();
        if (timeToLive != null) {
            addParameter(body, PARAM_TIME_TO_LIVE, Integer.toString(timeToLive));
        }
        for (Entry<String, String> entry : message.getData().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null) {
                logger.warning("Ignoring payload entry that has null: " + entry);
            } else {
                key = PARAM_PAYLOAD_PREFIX + key;
                addParameter(body, key, URLEncoder.encode(value, UTF8));
            }
        }
        String requestBody = body.toString();
        logger.finest("Request body: " + requestBody);
        HttpURLConnection conn;
        int status;
        try {
            conn = post(GCM_SEND_ENDPOINT, requestBody);
            status = conn.getResponseCode();
        } catch (IOException e) {
            System.out.println( "IOException posting to GCM"+ e);
            logger.log(Level.FINE, "IOException posting to GCM", e);
            return null;
        }
        if (status / 100 == 5) {
            System.out.println("GCM service is unavailable (status " + status + ")");
            logger.fine("GCM service is unavailable (status " + status + ")");
            return null;
        }
        String responseBody;
        if (status != 200) {
            try {
                responseBody = getAndClose(conn.getErrorStream());
                logger.finest("Plain post error response: " + responseBody);
            } catch (IOException e) {
                // ignore the exception since it will thrown an InvalidRequestException
                // anyways
                responseBody = "N/A";
                logger.log(Level.FINE, "Exception reading response: ", e);
            }
            throw new InvalidRequestException(status, responseBody);
        } else {
            try {
                responseBody = getAndClose(conn.getInputStream());
            } catch (IOException e) {
                System.out.println("Exception reading response: "+ e);
                logger.log(Level.WARNING, "Exception reading response: ", e);
                // return null so it can retry
                return null;
            }
        }
        String[] lines = responseBody.split("\n");
        if (lines.length == 0 || lines[0].equals("")) {
            throw new IOException("Received empty response from GCM service.");
        }
        String firstLine = lines[0];
        String[] responseParts = split(firstLine);
        String token = responseParts[0];
        String value = responseParts[1];
        if (token.equals(TOKEN_MESSAGE_ID)) {
            Result.Builder builder = new Result.Builder().messageId(value);
            // check for canonical registration id
            if (lines.length > 1) {
                String secondLine = lines[1];
                responseParts = split(secondLine);
                token = responseParts[0];
                value = responseParts[1];
                if (token.equals(TOKEN_CANONICAL_REG_ID)) {
                    builder.canonicalRegistrationId(value);
                } else {
                    logger.warning("Invalid response from GCM: " + responseBody);
                }
            }
            Result result = builder.build();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("hejserver.Message created succesfully (" + result + ")");
            }
            return result;
        } else if (token.equals(TOKEN_ERROR)) {
            return new Result.Builder().errorCode(value).build();
        } else {
            throw new IOException("Invalid response from GCM: " + responseBody);
        }
    }



    private IOException newIoException(String responseBody, Exception e) {
        // log exception, as IOException constructor that takes a message and cause
        // is only available on Java 6
        String msg = "Error parsing JSON response (" + responseBody + ")";
        logger.log(Level.WARNING, msg, e);
        return new IOException(msg + ":" + e);
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore error
                logger.log(Level.FINEST, "IOException closing stream", e);
            }
        }
    }

    /**
     * Sets a JSON field, but only if the value is not {@literal null}.
     */
    private void setJsonField(Map<Object, Object> json, String field,
                              Object value) {
        if (value != null) {
            json.put(field, value);
        }
    }

    private Number getNumber(Map<?, ?> json, String field) {
        Object value = json.get(field);
        if (value == null) {
            throw new CustomParserException("Missing field: " + field);
        }
        if (!(value instanceof Number)) {
            throw new CustomParserException("Field " + field +
                    " does not contain a number: " + value);
        }
        return (Number) value;
    }

    class CustomParserException extends RuntimeException {
        CustomParserException(String message) {
            super(message);
        }
    }

    private String[] split(String line) throws IOException {
        String[] split = line.split("=", 2);
        if (split.length != 2) {
            throw new IOException("Received invalid response line from GCM: " + line);
        }
        return split;
    }

    /**
     * Make an HTTP post to a given URL.
     *
     * @return HTTP response.
     */
    protected HttpURLConnection post(String url, String body)
            throws IOException {
        return post(url, "application/x-www-form-urlencoded;charset=UTF-8", body);
    }

    /**
     * Makes an HTTP POST request to a given endpoint.
     *
     * <p>
     * <strong>Note: </strong> the returned connected should not be disconnected,
     * otherwise it would kill persistent connections made using Keep-Alive.
     *
     * @param url endpoint to post the request.
     * @param contentType type of request.
     * @param body body of the request.
     *
     * @return the underlying connection.
     *
     * @throws IOException propagated from underlying methods.
     */
    protected HttpURLConnection post(String url, String contentType, String body)
            throws IOException {
        if (url == null || body == null) {
            throw new IllegalArgumentException("arguments cannot be null");
        }
        if (!url.startsWith("https://")) {
            logger.warning("URL does not use https: " + url);
        }
        logger.fine("Sending POST to " + url);
        logger.finest("POST body: " + body);
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = getConnection(url);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setFixedLengthStreamingMode(bytes.length);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Authorization", "key=" + key);
        OutputStream out = conn.getOutputStream();
        try {
            out.write(bytes);
        } finally {
            close(out);
        }
        return conn;
    }

    /**
     * Creates a map with just one key-value pair.
     */
    protected static final Map<String, String> newKeyValues(String key,
                                                            String value) {
        Map<String, String> keyValues = new HashMap<String, String>(1);
        keyValues.put(nonNull(key), nonNull(value));
        return keyValues;
    }

    /**
     * Creates a {@link StringBuilder} to be used as the body of an HTTP POST.
     *
     * @param name initial parameter for the POST.
     * @param value initial value for that parameter.
     * @return StringBuilder to be used an HTTP POST body.
     */
    protected static StringBuilder newBody(String name, String value) {
        return new StringBuilder(nonNull(name)).append('=').append(nonNull(value));
    }

    /**
     * Adds a new parameter to the HTTP POST body.
     *
     * @param body HTTP POST body.
     * @param name parameter's name.
     * @param value parameter's value.
     */
    protected static void addParameter(StringBuilder body, String name,
                                       String value) {
        nonNull(body).append('&')
                .append(nonNull(name)).append('=').append(nonNull(value));
    }

    /**
     * Gets an {@link HttpURLConnection} given an URL.
     */
    protected HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        return conn;
    }

    /**
     * Convenience method to convert an InputStream to a String.
     * <p>
     * If the stream ends in a newline character, it will be stripped.
     * <p>
     * If the stream is {@literal null}, returns an empty string.
     */
    protected static String getString(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream));
        StringBuilder content = new StringBuilder();
        String newLine;
        do {
            newLine = reader.readLine();
            if (newLine != null) {
                content.append(newLine).append('\n');
            }
        } while (newLine != null);
        if (content.length() > 0) {
            // strip last newline
            content.setLength(content.length() - 1);
        }
        return content.toString();
    }

    private static String getAndClose(InputStream stream) throws IOException {
        try {
            return getString(stream);
        } finally {
            if (stream != null) {
                close(stream);
            }
        }
    }

    static <T> T nonNull(T argument) {
        if (argument == null) {
            throw new IllegalArgumentException("argument cannot be null");
        }
        return argument;
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}