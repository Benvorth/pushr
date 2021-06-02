package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.user.AccessTokenRepository;
import de.benvorth.pushr.model.user.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path="api")
public class PushrController {

    AccessTokenRepository accessTokenRepository;

    @Autowired
    public PushrController (AccessTokenRepository accessTokenRepository) {
        this.accessTokenRepository = accessTokenRepository;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        path = "/is_alive",
        produces = "application/json"
    )
    public ResponseEntity<String> isAlive(
        @RequestHeader("x-pushr-access-token") String accessToken // if not present: result is 400 - Bad Request
    ) {
        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "Cannot claim token, no or unknown subscriptionEndpoint provided").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }
        return new ResponseEntity<>("\"result\":\"ok\",\"msg\":\"API is alive!\"}", HttpStatus.OK);

    }


}
