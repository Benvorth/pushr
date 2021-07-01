package de.benvorth.pushr.model.event;

import de.benvorth.pushr.model.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Setter
@Getter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_id")
    private Long eventId;

    private Long userId;

    private String  name;
    private long created;
    private long lastTriggered;
    private String trigger;
    private Boolean triggerActive;

    /*
    // this is the creater (=owner) of the event.
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // do not set this to EAGER!
    @JoinColumn(name="user_id", nullable = false)
    private User user;
     */

    public Event(String name, long userId, long created, String trigger, Boolean triggerActive) {
        this.name = name;
        this.userId = userId;
        this.created = created;
        this.lastTriggered = -1;
        this.trigger = trigger;
        this.triggerActive = triggerActive;
    }

    public String toJson () {
        return "{" +
            "\"eventId\":" + this.getEventId() + "," +
            "\"name\":\"" + this.getName() + "\"," +
            "\"created\":" + this.getCreated() + "," +
            "\"lastTriggered\":" + this.getLastTriggered() + "," +
            "\"trigger\":\"" + this.getTrigger() + "\"," +
            "\"triggerActive\":" + this.getTriggerActive() + "" +
        "}";
    }

    public String toJson (boolean owned, boolean subscribed) {
        return "{" +
            "\"eventId\":" + this.getEventId() + "," +
            "\"name\":\"" + this.getName() + "\"," +
            "\"created\":" + this.getCreated() + "," +
            "\"lastTriggered\":" + this.getLastTriggered() + "," +
            "\"trigger\":\"" + this.getTrigger() + "\"," +
            "\"triggerActive\":" + this.getTriggerActive() + "," +
            "\"owned\":" + owned + "," +
            "\"subscribed\":" + subscribed + "" +
            "}";
    }
}
