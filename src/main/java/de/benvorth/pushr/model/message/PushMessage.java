package de.benvorth.pushr.model.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.benvorth.pushr.model.device.client.ClientSubscriptionKeys;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

// https://developer.mozilla.org/en-US/docs/Web/API/notification
// https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerRegistration/showNotification
// https://firebase.google.com/docs/cloud-messaging/http-server-ref
@Entity
@NoArgsConstructor
@Getter @Setter
public class PushMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "message_id")
    private Long messageId;

    private Long eventId;

    private Long userIdOwner;


    private long created;

    /**
     * The title of the notification
     */
    private String title;

    /**
     * The URL of an image to be displayed as part of the notification,
     */
    private String image;

    /**
     * The URL of the image used as an icon of the notification
     */
    private String icon;

    /**
     * the URL of an image to represent the notification when there is
     * not enough space to display the notification itself such as for
     * example, the Android Notification Bar. On Android devices, the
     * badge should accommodate devices up to 4x resolution,
     * about 96 by 96 px, and the image will be automatically masked.
     */
    private String badge;

    /**
     * The body string of the notification
     */
    private String body;

    /**
     * The direction of the notification; it can be auto,  ltr or rtl
     */
    private String dir;

    /**
     * The language code of the notification
     */
    private String lang;

    /**
     * Specifies whether the user should be notified after a new
     * notification replaces an old one.
     */
    private Boolean renotify;

    /**
     * A Boolean indicating that a notification should remain active
     * until the user clicks or dismisses it, rather than closing automatically.
     */
    private Boolean requireInteraction;

    /**
     * Specifies whether the notification should be silent — i.e., no sounds or
     * vibrations should be issued, regardless of the device settings.
     */
    private Boolean silent;

    /**
     * The ID of the notification (if any)
     */
    private String tag;

    /**
     * @Unused
     * Arbitrary data that you want to be associated with the notification. This can be of any data type.
     */
    private String data;

    /**
     * Specifies a vibration pattern for devices with vibration hardware to emit.
     * "[200,150,200]"
     */
    // private List<Integer> vibrate;
    private String vibrate;

    /**
     * Specifies the time at which a notification is created or applicable
     * (past, present, or future).
     */
    private Long timestamp;

    /**
     * An array of actions to display in the notification. The members of the array should be an object literal
     */
    /*private List<NotificationAction> actions;


    @NoArgsConstructor
    @Getter @Setter
    public class NotificationAction {
*/
        /**
         *  A string identifying a user action to be displayed on the notification
         */
  //      private String action;

        /**
         * A string containing action text to be shown to the user.
         */
     //   private String title;

        /**
         * A string containing the URL of an icon to display with the action.
         */
       // private String icon;

    //}


    public String toJson () {
        return "{" +
            "\"messageId\":" + this.getMessageId() + "," +
            "\"eventId\":" + this.getEventId() + "," +
            "\"title\":\"" + this.getTitle() + "\"," +
            "\"body\":\"" + this.getBody() + "\"," +
            "\"icon\":\"" + this.getIcon() + "\"," +
            "\"badge\":\"" + this.getBadge() + "\"," +
            "\"created\":" + this.getCreated() + "," +
            "\"image\":\"" + this.getImage() + "\"," +
            "\"dir\":\"" + this.getDir() + "\"," +
            "\"lang\":\"" + this.getLang() + "\"," +
            "\"renotify\":" + this.getRenotify() + "," +
            "\"requireInteraction\":" + this.getRequireInteraction() + "," +
            "\"silent\":" + this.getSilent() + "," +
            "\"tag\":\"" + this.getTag() + "\"," +
            "\"data\":\"" + this.getData() + "\"," +
            "\"vibrate\":\"" + this.getVibrate() + "\"," +
            "\"timestamp\":" + this.getTimestamp() + "" +
        "}";
    }


    public PushMessage(
        String title, String body,
        String icon, String badge, String image,
        boolean renotify, boolean requireInteraction, boolean silent,
        String dir, String lang, String tag, String data,String vibrate,
        long timestamp, long created, long userIdOwner, long eventId
    ) {
        this.setTitle(title);
        this.setBody(body);
        this.setIcon(icon);
        this.setBadge(badge);
        this.setImage(image);
        this.setDir(dir);
        this.setLang(lang);
        this.setRenotify(renotify);
        this.setRequireInteraction(requireInteraction);
        this.setSilent(silent);
        this.setTag(tag);
        this.setData(data);
        this.setVibrate(vibrate);
        this.setTimestamp(timestamp);
        this.setCreated(created);
        this.setUserIdOwner(userIdOwner);
        this.setEventId(eventId);

    }

}
