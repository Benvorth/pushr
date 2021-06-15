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

    private String  name;
    private long created;
    private long lastTriggered;
    private String trigger;
    private Boolean triggerActive;

    // this is the creater (=owner) of the event.
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // do not set this to EAGER!
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    public Event(String name, long created, String trigger, Boolean triggerActive, User user) {
        this.name = name;
        this.created = created;
        this.lastTriggered = -1;
        this.trigger = trigger;
        this.triggerActive = triggerActive;
        this.user = user;
    }

    public String toJson () {
        return "{" +
            "\"event_id\":" + this.getEventId() + "," +
            "\"name\":\"" + this.getName() + "\"," +
            "\"created\":" + this.getCreated() + "," +
            "\"last_triggered\":" + this.getLastTriggered() + "," +
            "\"trigger\":\"" + this.getTrigger() + "\"," +
            "\"trigger_active\":" + this.getTriggerActive() + "" +
        "}";
    }

    public String toJson (boolean owned, boolean subscribed) {
        return "{" +
            "\"event_id\":" + this.getEventId() + "," +
            "\"name\":\"" + this.getName() + "\"," +
            "\"created\":" + this.getCreated() + "," +
            "\"last_triggered\":" + this.getLastTriggered() + "," +
            "\"trigger\":\"" + this.getTrigger() + "\"," +
            "\"trigger_active\":" + this.getTriggerActive() + "" +
            "\"owned\":" + owned + "" +
            "\"subscribed\":" + subscribed + "" +
            "}";
    }


}
