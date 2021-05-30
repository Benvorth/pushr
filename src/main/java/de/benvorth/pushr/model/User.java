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

    private String id_provider;
    private String name;
    private String avatarUrl;

    public User (String id_provider, String name, String avatarUrl) {
        this.id_provider = id_provider;
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

    public String toJson () {
        return "{" +
            "\"id\":" + this.id + "," +
            "\"idProvider\":\"" + this.id_provider + "," +
            "\"name\":\"" + this.name + "," +
            "\"avatarUrl\":\"" + this.avatarUrl + "" +
            "}";
    }

}
