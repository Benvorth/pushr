package de.benvorth.pushr.model;

import com.google.api.client.googleapis.apache.v2.GoogleApacheHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import de.benvorth.pushr.PushrApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@RestController
public class UserController {

    UserRepository userRepository;

    @Value("${oauth.appId.google}")
    private String appId_google;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PutMapping("/user/google")
    public ResponseEntity<String> putUser (@RequestBody String idTokenString) {

        // validate provided User
        // https://developers.google.com/identity/sign-in/web/backend-auth
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            // Specify the CLIENT_ID of the app that accesses the backend:
            .setAudience(Collections.singletonList(appId_google))
            // Or, if multiple clients access the backend:
            //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
            .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();


                String userId = payload.getSubject();
                // Get profile information from payload
                String email = payload.getEmail();
                boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String locale = (String) payload.get("locale");
                String familyName = (String) payload.get("family_name");
                String givenName = (String) payload.get("given_name");

                PushrApplication.logger.info(
                    "++++Valid google token: " +
                    "    userId: {},\n" +
                    "    email: {},\n" +
                    "    emailVerified: {},\n" +
                    "    name: {},\n" +
                    "    pictureUrl: {},\n" +
                    "    locale: {},\n" +
                    "    familyName: {},\n" +
                    "    givenName: {}",
                    userId, email, emailVerified, name, pictureUrl, locale, familyName, givenName
                );

                // Use or store profile information
                // ...

            } else {
                PushrApplication.logger.info("Provided ID token '{}' is not a valid google token", idTokenString);
                return new ResponseEntity<>("{\"result\":\"error\",\"msg\":\"Provided ID token is not a valid google token\"}", HttpStatus.BAD_REQUEST);
            }

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>("{\"result\":\"success\",\"msg\":\"user saved/updated successfully\"}", HttpStatus.OK);

        /*
        List<User> userData = userRepository.findByToken(providedUser.getUserToken());

        if (userData != null && userData.size() > 0) {

            if (userData.size() > 1) {
                return new ResponseEntity<>("{\"result\":\"error\",\"msg\":\"more than one user found for provided token\"}", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            User user = userData.get(0);
            user.setIdProvider(providedUser.getIdProvider());
            user.setName(providedUser.getName());
            user.setUserToken(providedUser.getUserToken());
            user.setAvatarUrl(providedUser.getAvatarUrl());
            User savedElement = userRepository.save(user);
            return new ResponseEntity<>(savedElement.toJson(), HttpStatus.OK);
        } else {
            User savedElement = userRepository.save(providedUser);
            return new ResponseEntity<>(savedElement.toJson(), HttpStatus.CREATED);
        }
         */
    }


}
