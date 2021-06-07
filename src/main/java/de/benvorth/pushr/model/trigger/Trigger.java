package de.benvorth.pushr.model.trigger;

import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name="trigger")
@NoArgsConstructor
@Setter
@Getter
public class Trigger {

    public enum TriggerType {
        token, timed, rest
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "trigger_id")
    private Long triggerId;

    private String  name;
    private long created;
    private long lastTriggered;
    private String token;
    private TriggerType triggerType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // do not set this to EAGER!
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    public Trigger(String name, long created, String token, TriggerType triggerType, User user) {
        this.name = name;
        this.created = created;
        this.lastTriggered = -1;
        this.token = token;
        this.triggerType = triggerType;
        this.user = user;
    }



}
