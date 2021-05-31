package de.benvorth.pushr.model;

import lombok.*;

import javax.persistence.*;

// https://www.javatpoint.com/spring-boot-jpa
// https://bezkoder.com/spring-boot-jpa-h2-example/

@Entity
@Table(name = "user")
@NoArgsConstructor
@Setter @Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id; // will be set when persisting

    private String user_id;
    private String id_provider;
    private String name;
    private String avatar_url;
    private long first_login;
    private long last_seen;

    public User (String user_id, String id_provider, String name, String avatar_url) {
        this.user_id = user_id;
        this.id_provider = id_provider;
        this.name = name;
        this.avatar_url = avatar_url;
        long now = System.currentTimeMillis();
        this.first_login = now;
        this.last_seen = now;
    }

    public String toJson () {
        return "{" +
            "\"id\":" + this.id + "," +
            "\"user_id\":\"" + this.user_id + "\"," +
            "\"id_provider\":\"" + this.id_provider + "\"," +
            "\"name\":\"" + this.name + "\"," +
            "\"avatar_url\":\"" + this.avatar_url + "\"" +
            "}";
    }

}
