package de.benvorth.pushr.model.device;

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
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "device_id")
    private Long deviceId;


    private String name;
    private String deviceType;
    @Column(unique = true)
    private String endpoint;
    private long expirationTime;
    private String p256dh;
    private String auth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // do not set this to EAGER!
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    public Device(String name, String deviceType, String endpoint, long expirationTime, String p256dh, String auth, User user) {
        this.name = name;
        this.deviceType = deviceType;
        this.endpoint = endpoint;
        this.expirationTime = expirationTime;
        this.p256dh = p256dh;
        this.auth = auth;
        this.user = user;
    }

    public String toJson () {
        return "{" +
            "\"device_id\":" + this.getDeviceId() + "," +
            "\"endpoint\":\"" + this.getEndpoint() + "\"," +
            "\"expirationTime\":" + this.getExpirationTime() + "," +
            "\"p256dh\":\"" + this.getP256dh() + "\"," +
            "\"auth\":\"" + this.getAuth() + "\"," +
            "\"user_id\":" + this.getUser().getUserId() + "" +
        "}";
    }
}
