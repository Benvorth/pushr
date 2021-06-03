package de.benvorth.pushr.model.push;

import de.benvorth.pushr.model.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
// @Table(name = "user")
@NoArgsConstructor
@Setter
@Getter
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "push_subscription_id")
    private Long pushSubscriptionId;

    @Column(unique = true)
    private String endpoint;
    private long expirationTime;
    private String p256dh;
    private String auth;

    // mappedBy: To declare a side as not responsible for the FK-relationship.
    // Value: the variable name of this class instance on the owner side
    @OneToOne(mappedBy = "pushSubscription")
    private User user;

    public PushSubscription (String endpoint, long expirationTime, String p256dh, String auth) {
        this.endpoint = endpoint;
        this.expirationTime = expirationTime;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public String toJson () {
        return "{" +
            "\"push_subscription_id\":" + this.getPushSubscriptionId() + "," +
            "\"endpoint\":\"" + this.getEndpoint() + "\"," +
            "\"expirationTime\":" + this.getExpirationTime() + "," +
            "\"p256dh\":\"" + this.getP256dh() + "\"," +
            "\"auth\":\"" + this.getAuth() + "\"" +
        "}";
    }
}
