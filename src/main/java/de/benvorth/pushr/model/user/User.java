package de.benvorth.pushr.model.user;

import lombok.*;

import javax.persistence.*;

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
    private long userId; // will be set when persisting

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

    public String toJson () {
        return "{" +
            "\"userId\":" + this.getUserId() + "," +
            "\"providerId\":\"" + this.getProviderId() + "\"," +
            "\"idProvider\":\"" + this.getIdProvider() + "\"," +
            "\"name\":\"" + this.getName() + "\"," +
            "\"locale\":\"" + this.getLocale() + "\"," +
            "\"avatarUrl\":\"" + this.getAvatarUrl() + "\"," +
            "\"firstLogin\":\"" + this.getFirstLogin() + "\"," +
            "\"lastSeen\":\"" + this.getLastSeen() + "\"" +
            "}";
    }

}
