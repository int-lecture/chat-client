package var.chat.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

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

import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Simple chat client UI to test the servers.
 */
public class ChatClient {

    /** My handle. */
    private static String myId = "tom";

    /** Handle of the communication partner. */
    private static String otherId = "bob";

    /** Chat-Server URL. */
    private static String chatServer = "http://localhost:5000";

    /** Login-Server URL. */
    private static String loginServer = "http://localhost:5001";

    /** Style for my messages. */
    private SimpleAttributeSet styleSendMessages = new SimpleAttributeSet();

    /** Style for received messages. */
    private SimpleAttributeSet styleReceivedMessages = new SimpleAttributeSet();

    /** Style status messages. */
    private SimpleAttributeSet styleStatusMessages = new SimpleAttributeSet();

    /** Sequence number of last message seen. */
    private int lastSequence = 0;

    /** Token received at login. */
    private static String loginToken;

    /**
     * Communication helper.
     */
    private Communication com = new Communication(loginServer, chatServer);

    JFrame frame = new JFrame("Chat: " + myId + " <-> " + otherId + " | server: " + chatServer);
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
        if (command.startsWith("!login")) {
            cmdLogin(command);
            return;
        }

        switch (command) {
            case "!clear":
                cmdClear();
                break;
            case "!status":
                cmdShowStatus();
                break;
            case "!logout":
                loginToken = null;
                printStatus("Logged out");
                break;
            default:
                printStatus(String.format("Unknown command '%s'", command));
        }
    }

    /**
     * Log in the user to the server.
     * @param command
     */
    private void cmdLogin(String command) {
        String payLoad = command.replace("!login ", "");
        StringTokenizer st = new StringTokenizer(payLoad, ":");
        String user = st.nextToken();
        String pwd = st.nextToken();
        String theToken = com.login(user, pwd, myId);
        if (theToken == null) {
            printStatus("Falscher User/Password");
        }
        else {
            loginToken = theToken;
            printStatus("Logged in. Token=" + theToken);
        }
    }

    /**
     * Show status.
     */
    private void cmdShowStatus() {
        printStatus(String.format("Connected to %s.\nTalking with %s. Last message ID %d", chatServer, otherId, lastSequence));
        printStatus(String.format("Token: %s", loginToken));

        if (loginToken == null) {
            printStatus("Use '!login username:password' to log in");
        }
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
            postMessage(message, loginToken);
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
     * @param theToken the security token
     */
    private void receiveAndPrintMessages(String user, int sequence, String theToken) {
        Message[] m = com.readMessage(user, sequence, theToken, this::printStatus);
        displayMessages(m);
    }


    /**
     * Post a message to the server.
     *
     * @param message the message to be posted.
     * @param loginToken the security token
     */
    private void postMessage(Message message, String theToken)
            throws ClientHandlerException {

        JSONObject json = message.toJSON();
        json.put("token", loginToken);

        try {
            Client client = Client.create();
            client.resource(chatServer + "/send")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .put(json.toString());
            client.destroy();
        }
        catch (UniformInterfaceException e) {
            printStatus(e.getMessage());
        }
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

        if (args.length < 4) {
            System.err.println("Missing option:");
            System.err.println("  Parameters: ChatURL LoginURL myID otherID");
            System.exit(1);
        }

        chatServer = args[0];
        loginServer = args[1];
        myId = args[2];
        otherId = args[3];

        ChatClient mw = new ChatClient();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    mw.receiveAndPrintMessages(myId, mw.lastSequence, loginToken);
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
