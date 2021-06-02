package de.benvorth.pushr.model.push;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
// @Table(name = "user")
@NoArgsConstructor
@Setter
@Getter
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private long userId;
    private String endpoint;
    private long expirationTime;
    private String p256dh;
    private String auth;

    public PushSubscription (long userId, String endpoint, long expirationTime, String p256dh, String auth) {
        this.userId = userId;
        this.endpoint = endpoint;
        this.expirationTime = expirationTime;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public String toJson () {
        return "{" +
            "\"id\":" + this.getId() + "," +
            "\"userId\":" + this.getUserId() + "," +
            "\"endpoint\":\"" + this.getEndpoint() + "\"," +
            "\"expirationTime\":" + this.getExpirationTime() + "," +
            "\"p256dh\":\"" + this.getP256dh() + "\"," +
            "\"auth\":\"" + this.getAuth() + "\"" +
        "}";
    }
}
