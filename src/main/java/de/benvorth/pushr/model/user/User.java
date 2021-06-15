package de.benvorth.pushr.model.user;

import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.event.Event;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    /*@OneToOne(
        mappedBy = "user",
        fetch = FetchType.LAZY,
        optional = true // avoid eager fetching, see https://thorben-janssen.com/hibernate-tip-lazy-loading-one-to-one/
    )
    @OneToOne(
        mappedBy = "user",
        fetch = FetchType.LAZY
    )
    @JoinColumn(name = "access_token_id", referencedColumnName = "access_token_id")
    private AccessToken accessToken;
*/

    /*
    // https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/
    @OneToMany(
        mappedBy="user", // var name in the "many" part
        // cascade = CascadeType.ALL, // all the JPA and Hibernate entity state transitions (e.g., persist, merge, remove) are passed from the parent "User" entity to the "Device" child entities.
        // orphanRemoval = true, //  instruct the JPA provider to trigger a remove entity state transition when a PostComment entity is no longer referenced by its parent Post entity.
        fetch = FetchType.LAZY
    )
    // @JoinColumn(name="device_id")
    private List<Device> devices = new ArrayList<>();
*/
    @OneToMany(
        mappedBy="user", // var name in the "many" part
        // cascade = CascadeType.ALL,
        // orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    // @OneToMany(mappedBy="user")
    private List<Event> events = new ArrayList<>();

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
            // "\"access_token_id\":" + this.getAccessToken().getAccessTokenId() + "" +
            // "\"device_ids\":" + this.getDevices().forEach(device -> {return device.getDeviceId();}).getDeviceId() + "" +
        "}";
    }


    // https://www.baeldung.com/spring-data-jpa-projections
    public interface UserView_userId {
        long getUserId();
    }
}
