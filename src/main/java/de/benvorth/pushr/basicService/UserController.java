package de.benvorth.pushr.basicService;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional // run all database commands in this function in one transaction. Important to avoid unnecessary queries (eg in repo.save)
    @RequestMapping(
        method = RequestMethod.PUT,
        path = "/user/google",
        produces = "application/json"
    )
    public ResponseEntity<String> putUser (
        @RequestBody String idTokenString
    ) {
        PushrApplication.logger.info("++ api/user/google");

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

                // user already there?

                User user;
                boolean aNewUser = true;
                long now = System.currentTimeMillis();

                // providerIdUser has a unique constraint in model
                // List<User.UserView_userId> userIds = userRepository.getUserIdByProviderId(providerIdUser);
                List<User> users = userRepository.findByProviderId(providerIdUser);
                // boolean userAlreadyExistsInDatabase = userRepository.existsByProviderId(providerIdUser);
                if (users.size() > 0) { // userAlreadyExistsInDatabase
                    user = users.get(0);
                    aNewUser = false;
                    if (!providerIdUser.equals(user.getProviderId())) {
                        user.setProviderId(providerIdUser);
                    }
                    // todo update this
                    if (!UserUtils.ID_PROVIDER_GOOGLE.equals(user.getIdProvider())) {
                        user.setIdProvider(UserUtils.ID_PROVIDER_GOOGLE);
                    }
                    if (!name.equals(user.getName())) {
                        user.setName(name);
                    }
                    if (!locale.equals(user.getLocale())) {
                        user.setLocale(locale);
                    }
                    if (!pictureUrl.equals(user.getAvatarUrl())) {
                        user.setAvatarUrl(pictureUrl);
                    }
                } else { // a new user
                    user = new User();
                    user.setFirstLogin(now);
                    user.setProviderId(providerIdUser);
                    user.setIdProvider(UserUtils.ID_PROVIDER_GOOGLE);
                    user.setName(name);
                    user.setLocale(locale);
                    user.setAvatarUrl(pictureUrl);
                }
                user.setLastSeen(now);


                AccessToken accessToken = null;
                boolean createNewToken = true;
                if (!aNewUser) { // get existing access-token and check if it is expired. Create a new one if so.
                    // accessToken = user.getAccessToken(); // accessToken is fetched "EAGER"
                    List<AccessToken> tokens = accessTokenRepository.findByUser(user); // accessToken is fetched "EAGER"
                    if (tokens.size() != 1) {
                        PushrApplication.logger.info("No unambiguous access token found for existing user, fixing it by deleting all tokens and generate a new one");
                        if (tokens.size() > 1) {
                            accessTokenRepository.deleteAll(tokens);
                        }
                    } else {
                        accessToken = tokens.get(0);
                        if (accessToken.isExpired()) {
                            PushrApplication.logger.info("Token for user {} IS expired", user.getUserId());
                            PushrApplication.logger.info("Delete old token for user {}", user.getUserId());
                            accessTokenRepository.delete(accessToken);
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
                    accessToken = new AccessToken(UserUtils.generateToken(userId, exp), now, exp, user);
                    // user.setAccessToken(accessToken); // update "owning" side
                    // accessTokenRepository.save(accessToken); // save "passive" side first
                    // userRepository.save(user); // save "owning" side
                }

                if (!aNewUser) { // userAlreadyExistsInDatabase
                    // no need to persist user since we fetched the user (from the repo) in the same transaction.
                    // "dirty checking" will do this automatically for us once the transaction closes
                    // (after this function-call). This is done with ONE query (UPDATE, no SELECT)
                    // userRepository.persist(user);
                } else {
                    userRepository.save(user);
                }

                if (createNewToken) {
                    accessTokenRepository.save(accessToken);
                }

                PushrApplication.logger.info("++ api/user/google done");
                return new ResponseEntity<>(
                    accessToken.getJsonResultForClient(),
                    HttpStatus.OK
                );

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
