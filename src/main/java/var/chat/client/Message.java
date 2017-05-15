package var.chat.client;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

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
        return toJSON().toString();
    }

    public JSONObject toJSON() {
        SimpleDateFormat sdf = new SimpleDateFormat(Communication.ISO8601);

        JSONObject json = new JSONObject();
        json.put("from" , from);
        json.put("to" , to);
        json.put("date", sdf.format(new Date()));
        json.put("text", text);

        return json;
    }
}

