package de.benvorth.pushr.model.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// https://developer.mozilla.org/en-US/docs/Web/API/notification
// https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerRegistration/showNotification
@NoArgsConstructor
@Getter @Setter
public class PushMessage {

    enum Direction {
        auto, ltr, rtl
    }

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
     * The URL of the image used to represent the notification when
     * there is not enough space to display the notification itself.
     */
    private String badge;

    /**
     * The body string of the notification
     */
    private String body;

    /**
     * The direction of the notification; it can be auto,  ltr or rtl
     */
    private Direction dir;

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
    private Object data;

    /**
     * Specifies a vibration pattern for devices with vibration hardware to emit.
     */
    private List<Integer> vibrate;

    /**
     * Specifies the time at which a notification is created or applicable
     * (past, present, or future).
     */
    private Long timestamp;

    /**
     * An array of actions to display in the notification. The members of the array should be an object literal
     */
    private List<NotificationAction> actions;


    @NoArgsConstructor
    @Getter @Setter
    public class NotificationAction {

        /**
         *  A string identifying a user action to be displayed on the notification
         */
        private String action;

        /**
         * A string containing action text to be shown to the user.
         */
        private String title;

        /**
         * A string containing the URL of an icon to display with the action.
         */
        private String icon;

    }

}
