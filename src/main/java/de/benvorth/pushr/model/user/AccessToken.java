package de.benvorth.pushr.model.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
// @Table(name = "access_token")
@NoArgsConstructor
@Setter @Getter
public class AccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "access_token_id")
    private Long accessTokenId;

    private String token;
    private long created;
    private long expires;

    // mappedBy: To declare a side as not responsible for the FK-relationship.
    // Value: the variable name of this class instance on the owner side
    @OneToOne(
        // mappedBy = "accessToken",
        fetch = FetchType.LAZY
    )
    @JoinColumn(name="user_id") // uni-directional association
    private User user;

    public AccessToken (String token, long created, long expires, User user) {
        this.setToken(token);
        this.setCreated(created);
        this.setExpires(expires);
        this.setUser(user);
    }

    public String toJson () {
        return "{" +
            "\"access_token_id\":" + this.getAccessTokenId() + "," +
            "\"user_id\":" + this.getUser().getUserId() + "," +
            "\"token\":\"" + this.getToken() + "\"," +
            "\"created\":" + this.getCreated() + "," +
            "\"expires\":" + this.getExpires() + "" +
            "}";
    }

    public String getJsonResultForClient () {
        return "{" +
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
