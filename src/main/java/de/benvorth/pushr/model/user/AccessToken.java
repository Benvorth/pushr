package de.benvorth.pushr.model.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
// @Table(name = "access_token")
@NoArgsConstructor
@Setter
@Getter
public class AccessToken {

    @Id
    private long id;

    private String token;
    private long created;
    private long expires;

    public AccessToken (long id, String token, long created, long expires) {
        this.setId(id);
        this.setToken(token);
        this.setCreated(created);
        this.setExpires(expires);
    }

    public String toJson () {
        return "{" +
            "\"id\":" + this.getId() + "," +
            "\"token\":\"" + this.getToken() + "\"," +
            "\"created\":" + this.getCreated() + "," +
            "\"expires\":" + this.getExpires() + "" +
            "}";
    }

    public boolean isExpired () {
        long now = System.currentTimeMillis();
        if (now > this.getExpires()) {
            return true;
        } else {
            return false;
        }
    }

}
