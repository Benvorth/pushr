package de.benvorth.pushr.model.subscription;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Setter
@Getter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    private Long userId;
    private Long eventId;

    private Long subscribed;

    public Subscription (Long userId, Long eventId, Long subscribed) {
        this.setUserId(userId);
        this.setEventId(eventId);
        this.setSubscribed(subscribed);
    }

}
