package de.benvorth.pushr.model.user;

import lombok.*;

import javax.persistence.*;

// https://www.javatpoint.com/spring-boot-jpa
// https://bezkoder.com/spring-boot-jpa-h2-example/

@Entity
// @Table(name = "user")
@NoArgsConstructor
@Setter @Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id; // will be set when persisting

    private String userId;
    private String idProvider;
    private String name;
    private String avatarUrl;
    private long firstLogin;
    private long lastSeen;

    public User (String userId, String idProvider, String name, String avatarUrl) {
        this.userId = userId;
        this.idProvider = idProvider;
        this.name = name;
        this.avatarUrl = avatarUrl;
        long now = System.currentTimeMillis();
        this.firstLogin = now;
        this.lastSeen = now;
    }

    public String toJson () {
        return "{" +
            "\"id\":" + this.id + "," +
            "\"userId\":\"" + this.userId + "\"," +
            "\"idProvider\":\"" + this.idProvider + "\"," +
            "\"name\":\"" + this.name + "\"," +
            "\"avatarUrl\":\"" + this.avatarUrl + "\"," +
            "\"firstLogin\":\"" + this.firstLogin + "\"," +
            "\"lastSeen\":\"" + this.lastSeen + "\"" +
            "}";
    }

}
