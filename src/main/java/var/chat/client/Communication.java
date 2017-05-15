package var.chat.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * Class encapsulating the communication with the servers.
 */
public class Communication {

    /** String for date parsing in ISO 8601 format. */
    public static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private final String loginServer;
    private final String chatServer;

    /**
     * Create a new instance.
     *
     * @param loginServer URL of the login server.
     * @param chatServer URL of the chat server.
     */
    public Communication(String loginServer, String chatServer) {
        this.loginServer = loginServer;
        this.chatServer = chatServer;
    }

    /**
     * Fetch messages from the server and return them.
     *
     * @param user The user to fetch messages for.
     * @param sequence Sequence number of last message seen.
     * @param theToken the token
     * @return the messages.
     */
    Message[] readMessage(String user, int sequence, String theToken,
            Consumer<String> printStatus) {
        try {
            Client client = Client.create();
            String result = client.resource(
                        String.format("%s/messages/%s/%d", chatServer, user, sequence))
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Token " + theToken)
                    .get(String.class);

            JSONArray obj = new JSONArray(result);

            Message[] messages = new Message[obj.length()];

            SimpleDateFormat sdf = new SimpleDateFormat(ISO8601);
            for (int i = 0; i < obj.length(); i++) {
                JSONObject jo = obj.getJSONObject(i);
                Message message = new Message(
                        jo.getString("from"),
                        jo.getString("to"),
                        sdf.parse(jo.getString("date")),
                        jo.getString("text"),
                        jo.getInt("sequence"));
                messages[i] = message;
            }

            client.destroy();

            return messages;
        }
        catch (UniformInterfaceException | ClientHandlerException e) {
            // ignore errors more or less
            return new Message[0];
        } catch (JSONException e) {
            printStatus.accept(e.toString());
            return new Message[0];
        } catch (ParseException e) {
            printStatus.accept(e.toString());
            return new Message[0];
        }
    }

    /**
     * Helper class to store the token and expiration date as one object.
     */
    class Token {
        /** The Token. */
        String token;

        /** Timestamp, the token expires. */
        long expires;
    }

    /**
     * Perform a login for the user.
     *
     * @param user User
     * @param password Password
     * @param pseudonym User's pseudonym
     * @return
     */
    String login(String user, String password, String pseudonym) {
        JSONObject json = new JSONObject();
        json.put("user", user);
        json.put("password", password);
        json.put("pseudonym", pseudonym);

        Client client = Client.create();
        WebResource webResource = client.resource(loginServer + "/login");
        ClientResponse response =
                webResource.type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, json.toString());

        if (response.getStatus() == 200) {
            JSONObject respJSON = new JSONObject(response.getEntity(String.class));
            String theToken = respJSON.getString("token");
            return theToken;
        }
        else {
            return null;
        }
    }

    /**
     * Call server and check token.
     *
     * @param pseudonym user's pseudonym.
     * @param theToken the token to be validated.
     * @return {@code null} if token is invalid, otherwise the token
     */
    Token retrieveAndValidateToken(String pseudonym, String theToken) {

        JSONObject json = new JSONObject();
        json.put("token", theToken);
        json.put("pseudonym", pseudonym);

        Client client = Client.create();
        WebResource webResource = client.resource(loginServer + "/auth");
        ClientResponse response =
                webResource.type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, json.toString());

        if (response.getStatus() == 200) {
            SimpleDateFormat sdf = new SimpleDateFormat(ISO8601);

            JSONObject respJSON = new JSONObject(response.getEntity(String.class));
            Token t = new Token();
            t.token = theToken;
            try {
                t.expires = sdf.parse(respJSON.getString("expire-date")).getTime();
            }
            catch (JSONException | ParseException e) {
                // do nothing
            }
            return t;
        }
        else {
            return null;
        }
    }
}
