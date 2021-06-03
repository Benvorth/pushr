package de.benvorth.pushr.basicService;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api")
public class UserController {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;

    @Value("${oauth.appId.google}")
    private String appId_google;

    @Autowired
    public UserController(
        UserRepository userRepository,
        AccessTokenRepository accessTokenRepository
        ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        path = "/user/google",
        produces = "application/json"
    )
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


                String providerIdUser = payload.getSubject();
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
                    "    providerIdUser: {},\n" +
                    "    email: {},\n" +
                    "    emailVerified: {},\n" +
                    "    name: {},\n" +
                    "    pictureUrl: {},\n" +
                    "    locale: {},\n" +
                    "    familyName: {},\n" +
                    "    givenName: {}",
                    providerIdUser, email, emailVerified, name, pictureUrl, locale, familyName, givenName
                );

                User user;
                long now = System.currentTimeMillis();
                List<User> userData = userRepository.findByProviderId(providerIdUser);
                if (userData != null && userData.size() > 0) {
                    // user already in database
                    if (userData.size() > 1) {
                        return new ResponseEntity<>(
                            "{\"result\":\"error\",\"msg\":\"more than one user found for provided token\"}",
                            HttpStatus.INTERNAL_SERVER_ERROR
                        );
                    }
                    user = userData.get(0);

                } else {
                    // a new user
                    user = new User();
                    user.setFirstLogin(now);
                }
                user.setLastSeen(now);
                user.setProviderId(providerIdUser);
                user.setIdProvider(UserUtils.ID_PROVIDER_GOOGLE);
                user.setName(name);
                user.setLocale(locale);
                user.setAvatarUrl(pictureUrl);
                User savedElement = userRepository.save(user);

                // all fine?
                User u = userRepository.findByProviderId(providerIdUser).get(0);
                PushrApplication.logger.info("Found user {} in database", u.getProviderId());

                AccessToken token = new AccessToken();
                boolean createNewToken = true;
                if (user.getAccessToken() != null) {
                    Optional<AccessToken> tokenResult = accessTokenRepository.findById(user.getAccessToken().getAccessTokenId());
                    if (tokenResult.isPresent()) {
                        PushrApplication.logger.info("Found token for user {} in database", user.getUserId());

                        token = tokenResult.get();
                        if (token.isExpired()) {
                            PushrApplication.logger.info("Token for user {} IS expired", user.getUserId());
                            PushrApplication.logger.info("Delete old token for user {}", user.getUserId());
                            accessTokenRepository.delete(token);
                        } else {
                            PushrApplication.logger.info("Token for user {} is not expired", user.getUserId());
                            createNewToken = false;
                        }
                    }
                }

                if (createNewToken) {
                    PushrApplication.logger.info("Create new token for user {}", user.getUserId());
                    long userId = user.getUserId();
                    long exp = now + UserUtils.TOKEN_TTL;
                    token = new AccessToken(UserUtils.generateToken(userId, exp), now, exp);
                    accessTokenRepository.save(token); // save "passive" side first
                    user.setAccessToken(token); // update "owning" side
                    userRepository.save(user); // save "owning" side
                }

                // all fine?
                Optional<AccessToken> tokenResultTest = accessTokenRepository.findById(user.getAccessToken().getAccessTokenId());
                if (!tokenResultTest.isPresent()) {
                    PushrApplication.logger.error("Token for user {} is NOT in database", u.getUserId());
                    return new ResponseEntity<>(
                        "{\"result\":\"error\",\"msg\":\"Unable to generate user token\"}",
                        HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                PushrApplication.logger.info("Token for user {} is in database", u.getUserId());

                return new ResponseEntity<>(token.getJsonResultForClient(), HttpStatus.OK);

            } else {
                PushrApplication.logger.info("Provided ID token '{}' is not a valid google token", idTokenString);
                return new ResponseEntity<>(
                    "{\"result\":\"error\",\"msg\":\"Provided ID token is not a valid google token\"}",
                    HttpStatus.BAD_REQUEST
                );
            }

        } catch (GeneralSecurityException | IOException e) {
            PushrApplication.logger.error("Error while parsing the provided google token", e);
            return new ResponseEntity<>(
                "{\"result\":\"error\",\"msg\":\"Error while parsing the provided google token\"}",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }




}
