package de.benvorth.pushr.model;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.user.AccessToken;
import de.benvorth.pushr.model.user.AccessTokenRepository;
import de.benvorth.pushr.model.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.persistence.Access;
import java.util.List;

@Component
public class Housekeeping {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;

    @Autowired
    public Housekeeping(
        UserRepository userRepository,
        AccessTokenRepository accessTokenRepository
    ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
    }

    // @Scheduled(fixedDelay = 20_000)
    public void cleanupAccessTokens() {

        List<AccessToken> toBeDeleted = accessTokenRepository.findByExpiresLessThan(System.currentTimeMillis());
        if (toBeDeleted.size() > 0 ) {
            PushrApplication.logger.info("DB-Housekeeping: cleanup {} expired AccessTokens", toBeDeleted.size());
            accessTokenRepository.deleteAll(toBeDeleted);
        }

    }
}
