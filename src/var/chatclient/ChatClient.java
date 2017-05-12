package var.chatclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Simple chat client UI to test the servers.
 */
public class ChatClient {

    /** String for date parsing in ISO 8601 format. */
    public static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** My handle. */
    private static String myId = "tom";

    /** Handle of the communication partner. */
    private static String otherId = "bob";

    /** Server URL. */
    private static String url = "http://localhost:5000";

    /** Style for my messages. */
    private SimpleAttributeSet styleSendMessages = new SimpleAttributeSet();

    /** Style for received messages. */
    private SimpleAttributeSet styleReceivedMessages = new SimpleAttributeSet();

    /** Style status messages. */
    private SimpleAttributeSet styleStatusMessages = new SimpleAttributeSet();

    /** Sequence number of last message seen. */
    private int lastSequence = 0;

    JFrame frame = new JFrame("Chat: " + myId + " <-> " + otherId + " | server: " + url);
    JTextPane log = new JTextPane();
    JTextField input = new JTextField(50);
    JPanel inputPanel = new JPanel();
    JButton submitButton = new JButton("Submit");
    JScrollPane logScroll = new JScrollPane(log);

    /** Create a new instance. */
    public ChatClient() {

        StyleConstants.setBold(styleReceivedMessages, true);
        StyleConstants.setForeground(styleReceivedMessages, new Color(128, 0, 0));
        StyleConstants.setBackground(styleReceivedMessages, Color.white);
        StyleConstants.setFontSize(styleReceivedMessages, 14);
        StyleConstants.setAlignment(styleReceivedMessages, StyleConstants.ALIGN_RIGHT);

        StyleConstants.setBold(styleSendMessages, true);
        StyleConstants.setForeground(styleSendMessages, new Color(0, 128, 0));
        StyleConstants.setBackground(styleSendMessages, Color.white);
        StyleConstants.setFontSize(styleSendMessages, 14);

        StyleConstants.setBold(styleStatusMessages, true);
        StyleConstants.setForeground(styleStatusMessages, new Color(128, 128, 128));
        StyleConstants.setBackground(styleStatusMessages, Color.white);
        StyleConstants.setFontSize(styleStatusMessages, 14);

        log.setPreferredSize(new Dimension(100, 300));
        frame.getContentPane().add(logScroll, BorderLayout.CENTER);
        inputPanel.add(input, BorderLayout.WEST);
        inputPanel.add(submitButton, BorderLayout.EAST);
        log.setEnabled(true); // otherwise we don't see colors
        log.setFocusable(false);
        frame.getContentPane().add(inputPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        submitButton.addActionListener((e) -> send());
        input.addActionListener((e) -> send());
    }

    /**
     * Print a message to the screen.
     *
     * @param text the text to be displayed.
     * @param style the style
     */
    private synchronized void print(String text, SimpleAttributeSet style) {
        try {
            Document doc = log.getStyledDocument();
            doc.insertString(doc.getLength(), "\n" + text, style);
        }
        catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        log.repaint();
        log.setCaretPosition(log.getDocument().getLength());
    }

    /**
     * Display a status message.
     *
     * @param text the text to be displayed.
     */
    private void printStatus(String text) {
        print(text, styleStatusMessages);
    }

    /**
     * Display a received message.
     *
     * @param message The message.
     */
    private void printReceivedMessage(Message message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        print("[" + sdf.format(message.date) + "] (" + message.from + ") " + message.text, styleReceivedMessages);
    }

    /**
     * Display a sent message.
     *
     * @param message The message.
     */
    private void printSentMessage(Message message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        print("[" + sdf.format(message.date) + "] (ich) " + message.text, styleSendMessages);
    }

    /**
     * Handle commands inside the client.
     *
     * @param command the command
     */
    private void handleCommand(String command) {
        switch (command) {
            case "!clear":
                cmdClear();
                break;
            case "!status":
                cmdShowStatus();
                break;
            default:
                printStatus(String.format("Unknown command '%s'", command));
        }
    }

    /**
     * Show status.
     */
    private void cmdShowStatus() {
        printStatus(String.format("Connected to %s.\nTalking with %s. Last message ID %d\n", url, otherId, lastSequence));
    }

    /**
     * Clear the screen.
     */
    private void cmdClear() {
        log.setText("");
    }

    /**
     * Event handler to send the message.
     */
    private void send() {

        try {
            String text = input.getText();
            input.setText("");

            if (text.length() == 0) {
                return;
            }

            if (text.startsWith("!")) {
                handleCommand(text);
                return;
            }

            Message message = new Message(myId, otherId, new Date(), text);
            postMessage(message);
            printSentMessage(message);
        }
        catch (ClientHandlerException ex) {
            printStatus("Error: " + ex.getMessage());
        }
    }

    /**
     * Fetch messages from the server and display them.
     *
     * @param user The user to fetch messages for.
     * @param sequence Sequence number of last message seen.
     */
    private void receiveAndPrintMessages(String user, int sequence) {
        Message[] m = readMessage(user, sequence);
        displayMessages(m);
    }

    /**
     * Fetch messages from the server and return them.
     *
     * @param user The user to fetch messages for.
     * @param sequence Sequence number of last message seen.
     * @return the messages.
     */
    private Message[] readMessage(String user, int sequence) {
        try {
            Client client = Client.create();
            String result = client.resource(
                        String.format("%s/messages/%s/%d", url, user, sequence))
                    .accept(MediaType.APPLICATION_JSON)
                    .get(String.class);

            JSONArray obj = new JSONArray(result);

            Message[] messages = new Message[obj.length()];

            SimpleDateFormat sdf = new SimpleDateFormat(ISO8601);
            for (int i = 0; i < obj.length(); i++) {
                JSONObject jo = obj.getJSONObject(i);
                Message message = new Message(jo.getString("from"),
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
            printStatus(e.toString());
            return new Message[0];
        } catch (ParseException e) {
            printStatus(e.toString());
            return new Message[0];
        }
    }


    /**
     * Post a message to the server.
     *
     * @param message the message to be posted.
     */
    private void postMessage(Message message) throws ClientHandlerException {

        Client client = Client.create();
        client.resource(url + "/send")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .put(message.toString());
        client.destroy();
    }

    /**
     * Display all messages.
     *
     * @param messages the messages to display.
     */
    private void displayMessages(Message[] messages) {
        for (Message m : messages) {
            printReceivedMessage(m);
            lastSequence = Math.max(lastSequence, m.sequence);
        }
    }

    /**
     * Main method.
     * @param args command line arguments.
     * @throws MalformedURLException URL not correctly given.
     */
    public static void main(String[] args) throws MalformedURLException {

        if (args.length < 3) {
            System.err.println("Missing option:");
            System.err.println("  Parameters: URL myID otherID");
            System.exit(1);
        }

        url = args[0];
        myId = args[1];
        otherId = args[2];

        ChatClient mw = new ChatClient();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    mw.receiveAndPrintMessages(myId, mw.lastSequence);
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();

        mw.cmdShowStatus();
    }
}
