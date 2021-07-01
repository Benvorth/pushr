package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.event.Event;
import de.benvorth.pushr.model.event.EventRepository;
import de.benvorth.pushr.model.subscription.Subscription;
import de.benvorth.pushr.model.subscription.SubscriptionRepository;
import de.benvorth.pushr.model.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ControllerUtil {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;
    DeviceRespository deviceRespository;
    EventRepository eventRepository;
    SubscriptionRepository subscriptionRepository;

    @Autowired
    public ControllerUtil(UserRepository userRepository,
                          AccessTokenRepository accessTokenRepository,
                          DeviceRespository deviceRespository,
                          EventRepository eventRepository,
                          SubscriptionRepository subscriptionRepository
    ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.deviceRespository = deviceRespository;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
    }


    public boolean removeDevice(String deviceEndpoint, Long userId) {
        Device device = getDeviceByEndpoint(deviceEndpoint, userId);
        if (device != null) {
            deviceRespository.delete(device);
            return true;
        }
        return false;
    }

    public Device getDeviceByEndpoint(String deviceEndpoint, Long userId) {

        // User user = accessTokenRepository.findUserByToken(accessToken); // .findByAccessToken_Token(accessToken).get(0);
        // Long userId = getUserIdFromAccessToken(accessToken);
        List<Device> devices = deviceRespository.findByEndpointAndUserId(deviceEndpoint, userId);
        if (devices.size() == 1) {
            Device device = devices.get(0);
            return device;
        } else if (devices.size() > 1) {
            PushrApplication.logger.error("Multiple devices found for given endpoint {}", deviceEndpoint);
            return null;
        } else {
            PushrApplication.logger.error("No device found for given endpoint {}", deviceEndpoint);
            return null;
        }
    }

    public boolean isInvalidToken(String accessToken) {
        if (accessToken == null
            || UserUtils.isTokenExpired(accessToken)
            || !accessTokenRepository.existsAccessTokenByToken(accessToken)
        ) {
            return true;
        } else {
            return false;
        }
    }

    public Long getUserIdFromTokenValidation(String accessToken) {
        if (accessToken == null
            || UserUtils.isTokenExpired(accessToken)
        ) {
            return null;
        } else {
            return getUserIdFromAccessToken(accessToken);
        }
    }

    public String generateNewTrigger() {

        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 8;

        int attemtsForTrigger = 0;
        String newTrigger = null;
        while (attemtsForTrigger < 20) {

            Random random = new Random();

            newTrigger = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

            newTrigger = newTrigger.substring(0, 2) + "-" + newTrigger.substring(2, 4) + "" +
                "-" + newTrigger.substring(4, 6) + "-"+newTrigger.substring(6, 8);

            newTrigger = newTrigger.toUpperCase();

            if (!isTriggerAssociatedWithEvent(newTrigger)) {
                break;
            }
            attemtsForTrigger++;
        }
        return newTrigger;
    }

    public boolean isTriggerAssociatedWithEvent (String trigger) {
        List<Event> events = eventRepository.findByTrigger(trigger);
        if (events.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public static String generateNewToken_old() {
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

        String hexRandom = Integer.toHexString(randomNum);
        while (hexRandom.length() < 8) {
            hexRandom = "0" + hexRandom;
        }
        PushrApplication.logger.info("hex: " + hexRandom);

        hexRandom = hexRandom.substring(0, 2) + "-" + hexRandom.substring(2, 4) + "" +
            "-" + hexRandom.substring(4, 6) + "-"+hexRandom.substring(6, 8);

        PushrApplication.logger.info("token: " + hexRandom);
        return hexRandom.toUpperCase();
    }

    public Long getUserIdFromAccessToken(String accessToken) {
        List<AccessToken> tokens = accessTokenRepository.findByToken(accessToken);
        if (tokens.size() == 1) {
            return tokens.get(0).getUser().getUserId();
        } else {
            return null;
        }
    }

    public boolean subscribeUnsubscribeToEvent(boolean subscribe, long userId, long eventId) {

        Subscription subscription = this.subscriptionRepository.findSubscriptionByUserIdAndEventId(userId, eventId);
        if (!subscribe) {// unsubscribe
            // check if we are subscribed
            // boolean alreadySubscribed = this.subscriptionRepository.existsSubscriptionByUserIdAndEventId(userId, eventId);
            if (subscription != null) {
                this.subscriptionRepository.delete(subscription);
                return true;
            } else {
                PushrApplication.logger.error("Not subscribed to event");
                return false;
            }
        } else {
            if (subscription == null) {
                Subscription newSubscription = new Subscription(userId, eventId, System.currentTimeMillis());
                this.subscriptionRepository.save(newSubscription);
                return true;
            } else {
                PushrApplication.logger.error("Already subscribed to event");
                return false;
            }
        }
    }
}
