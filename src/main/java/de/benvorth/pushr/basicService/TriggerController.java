package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.trigger.Trigger;
import de.benvorth.pushr.model.trigger.TriggerRepository;
import de.benvorth.pushr.model.user.AccessTokenRepository;
import de.benvorth.pushr.model.user.User;
import de.benvorth.pushr.model.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "api/trigger")

public class TriggerController {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;
    DeviceRespository deviceRespository;
    TriggerRepository triggerRepository;
    ControllerUtil controllerUtil;

    @Autowired
    public TriggerController(UserRepository userRepository,
                            AccessTokenRepository accessTokenRepository,
                            DeviceRespository deviceRespository,
                            TriggerRepository triggerRepository,
                            ControllerUtil controllerUtil
    ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.deviceRespository = deviceRespository;
        this.triggerRepository = triggerRepository;
        this.controllerUtil = controllerUtil;
    }


    /**
     * Claim a token for the user identified by accessToken,
     * connect it with the given subscriptionEndpoint (=device)
     * @param accessToken
     * @param token
     * @param subscriptionEndpoint
     * @return
     */
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/claimToken",
        produces = "application/json"
    )
    public ResponseEntity<String> claimToken(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("token") String token,
        @RequestParam("trigger_name") String triggerName,
        @RequestParam("subscriptionEndpoint") String subscriptionEndpoint
        // @RequestBody Subscription subscription
    ) {
        if (controllerUtil.isInvalidToken(accessToken)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        User user = userRepository.findByAccessToken_Token(accessToken).get(0);

        Device device = controllerUtil.getSubscriptionByEndpoint(subscriptionEndpoint, accessToken);
        if (device == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "Unable to find subscription with the given endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else {

            List<Trigger> triggers = triggerRepository.findByTokenAndUser(token, user);
            if (triggers.size() > 0) {
                return new ResponseEntity<>(
                    new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Token already claimed for this user").getJSON(),
                    HttpStatus.BAD_REQUEST
                );
            }

            Trigger newTrigger = new Trigger(
                triggerName,
                System.currentTimeMillis(),
                token,
                Trigger.TriggerType.token,
                user
            );
            triggerRepository.save(newTrigger);

            PushrApplication.logger.info("Token {} claimed successfully", token);
            // return this.sendTextPushMessage(subscription, new PushMessage("Text Notification", message));
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Token claimed").getJSON(),
                HttpStatus.CREATED
            );

        }
    }
}
