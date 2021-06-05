package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ControllerUtil {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;
    DeviceRespository deviceRespository;

    @Autowired
    public ControllerUtil(UserRepository userRepository,
                          AccessTokenRepository accessTokenRepository,
                          DeviceRespository deviceRespository
    ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.deviceRespository = deviceRespository;
    }


    public boolean removeSubscription(String subscriptionEndpoint, String accessToken) {
        Device device = getSubscriptionByEndpoint(subscriptionEndpoint, accessToken);
        if (device != null) {
            deviceRespository.delete(device);
            return true;
        }
        return false;
    }

    public Device getSubscriptionByEndpoint(String subscriptionEndpoint, String accessToken) {

        User user = userRepository.findByAccessToken_Token(accessToken).get(0);
        List<Device> devices = deviceRespository.findByEndpointAndUser(subscriptionEndpoint, user);
        if (devices.size() == 1) {
            Device device = devices.get(0);
            return device;
        } else if (devices.size() > 1) {
            PushrApplication.logger.error("Multiple pushSubscriptions found for given endpoint {}", subscriptionEndpoint);
            return null;
        } else {
            PushrApplication.logger.error("No pushSubscription found for given endpoint {}", subscriptionEndpoint);
            return null;
        }
    }

    public boolean isInvalidToken(String accessToken) {
        if (accessToken == null
            || accessTokenRepository.findByToken(accessToken).size() == 0
            || UserUtils.isTokenExpired(accessToken)
        ) {
            return true;
        } else {
            return false;
        }
    }
}
