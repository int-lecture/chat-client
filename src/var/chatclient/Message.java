package var.chatclient;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Chat message.
 */
class Message {

    /** From. */
    String from;

    /** To. */
    String to;

    /** Date. */
    Date date;

    /** Text. */
    String text;

    /** Sequence number. */
    int sequence;

    /**
     * Create a new message.
     *
     * @param from From.
     * @param to To.
     * @param date Date.
     * @param text Contents.
     * @param sequence Sequence-Number.
     */
    public Message(String from, String to, Date date, String text,
            int sequence) {
        this.from = from;
        this.to = to;
        this.date = date;
        this.text = text;
        this.sequence = sequence;
    }

    /**
     * Create a new message.
     *
     * @param from From.
     * @param to To.
     * @param date Date.
     * @param text Contents.
     */
    public Message(String from, String to, Date date, String text) {
        this(from, to, date, text, 0);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat(ChatClient.ISO8601);

        return String.format("{ 'from': '%s', 'to': '%s', 'date': '%s', 'text': '%s'}".replace('\'',  '"'),
                from, to, sdf.format(new Date()), text);
    }
}

