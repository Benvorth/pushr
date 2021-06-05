package de.benvorth.pushr.model.user;

import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.trigger.Trigger;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// https://www.javatpoint.com/spring-boot-jpa
// https://bezkoder.com/spring-boot-jpa-h2-example/

// https://www.baeldung.com/jpa-one-to-one

@Entity
// @Table(name = "user")
@NoArgsConstructor
@Setter @Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private long userId;

    @Column(unique = true)
    private String providerId;
    private String idProvider;
    private String name;
    private String locale;
    private String avatarUrl;
    private long firstLogin;
    private long lastSeen;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "access_token_id", referencedColumnName = "access_token_id")
    private AccessToken accessToken;

    // https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/
    @OneToMany(
        mappedBy="user", // var name in the "many" part
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    // @JoinColumn(name="device_id")
    private List<Device> devices = new ArrayList<>();

    @OneToMany(
        mappedBy="user", // var name in the "many" part
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    // @OneToMany(mappedBy="user")
    private List<Trigger> triggers = new ArrayList<>();

    public User (String providerId, String idProvider, String name, String locale, String avatarUrl) {
        this.providerId = providerId;
        this.idProvider = idProvider;
        this.name = name;
        this.locale = locale;
        this.avatarUrl = avatarUrl;
        long now = System.currentTimeMillis();
        this.firstLogin = now;
        this.lastSeen = now;
    }

    /*
    public void addDevice (Device device) {
        devices.add(device);
        device.setUser(this);
    }
    public void removeDevice (Device device) {
        devices.remove(device);
        device.setUser(null);
    }

    public void addTrigger (Trigger trigger) {
        triggers.add(trigger);
        trigger.setUser(this);
    }
    public void removeTrigger (Device trigger) {
        triggers.remove(trigger);
        trigger.setUser(null);
    }

     */

    public String toJson () {
        return "{" +
            "\"userId\":" + this.getUserId() + "," +
            "\"providerId\":\"" + this.getProviderId() + "\"," +
            "\"idProvider\":\"" + this.getIdProvider() + "\"," +
            "\"name\":\"" + this.getName() + "\"," +
            "\"locale\":\"" + this.getLocale() + "\"," +
            "\"avatarUrl\":\"" + this.getAvatarUrl() + "\"," +
            "\"firstLogin\":" + this.getFirstLogin() + "," +
            "\"lastSeen\":" + this.getLastSeen() + "," +
            "\"access_token_id\":" + this.getAccessToken().getAccessTokenId() + "" +
            // "\"device_ids\":" + this.getDevices().forEach(device -> {return device.getDeviceId();}).getDeviceId() + "" +
        "}";
    }

}
