package de.benvorth.pushr.basicService;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.device.client.ClientPushMsgSubscription;
import de.benvorth.pushr.model.user.*;
import de.benvorth.pushr.pushService.CryptoService;
import de.benvorth.pushr.pushService.ServerKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping(path = "api/device")
public class DeviceController {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;
    DeviceRespository deviceRespository;
    ControllerUtil controllerUtil;

    @Autowired
    public DeviceController(UserRepository userRepository,
                            AccessTokenRepository accessTokenRepository,
                            DeviceRespository deviceRespository,
                            ControllerUtil controllerUtil
    ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.deviceRespository = deviceRespository;
        this.controllerUtil = controllerUtil;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        path = "/register",
        produces = "application/json"
    )
    public ResponseEntity<String> register(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("device_name") String deviceName,
        @RequestParam("device_type") String deviceType,
        @RequestBody ClientPushMsgSubscription clientSubscription
    ) {
        if (controllerUtil.isInvalidToken(accessToken)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }
        AccessToken accessTokenObj = accessTokenRepository.findByToken(accessToken).get(0);
        User user = accessTokenObj.getUser();
        // User user = userRepository.findById(accessTokenObj.getUser().getUserId()).get();

        Device device;
        List<Device> existingDevices =
            deviceRespository.findByEndpointAndUser(clientSubscription.getEndpoint(), user);
        if (existingDevices.size() == 1) {
            device = existingDevices.get(0);
        } else if (existingDevices.size() > 1) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "More than one device found for this endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else {
            device = new Device(
                deviceName,
                deviceType,
                clientSubscription.getEndpoint(),
                (clientSubscription.getExpirationTime() != null ? clientSubscription.getExpirationTime() : 0),
                clientSubscription.getKeys().getP256dh(),
                clientSubscription.getKeys().getAuth(),
                user
            );
            deviceRespository.save(device); // save "passive" side first
        }
        // user.addDevice(device); // update "owning" side
        // userRepository.save(user); // save "owning" side

        String subscriptionId = createHash(clientSubscription);

        // this.subscriptions.put(subscription.getEndpoint(), subscription);
        // this.subscriptionsById.put(subscriptionId, subscription);
        return new ResponseEntity<>(
            "{\"subscriptionId\":\"" + subscriptionId + "\"}",
            HttpStatus.CREATED
        );
    }

    @RequestMapping(
        method = RequestMethod.POST,
        path = "/unregister",
        produces = "application/json"
    )
    public ResponseEntity<String> unsubscribe(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        // @RequestBody SubscriptionEndpoint subscription
        @RequestBody String subscriptionEndpoint
    ) {
        if (controllerUtil.isInvalidToken(accessToken)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        Device device = controllerUtil.getSubscriptionByEndpoint(subscriptionEndpoint, accessToken);
        if (device == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "Unable to find subscription with the given endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else {

            deviceRespository.delete(device);

            // todo check if this was successful

            PushrApplication.logger.info("Subscription for push messages removed from server");
            // return this.sendTextPushMessage(subscription, new PushMessage("Text Notification", message));
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Subscription for push messages removed from server").getJSON(),
                HttpStatus.OK
            );

        }
    }

    @RequestMapping(
        method = RequestMethod.POST,
        path = "/is_known",
        produces = "application/json"
    )
    public ResponseEntity<String> isSubscribed(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
//         @RequestBody SubscriptionEndpoint subscriptionEndpoint
        @RequestBody String subscriptionEndpoint
    ) {
        if (controllerUtil.isInvalidToken(accessToken)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        User user = userRepository.findByAccessToken_Token(accessToken).get(0);
        boolean isSubscribed = false;
        List<Device> devices = deviceRespository.findByEndpointAndUser(subscriptionEndpoint, user);
        if (devices.size() > 1) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "More than one subscriptions found for the given endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else if (devices.size() == 1) {
            isSubscribed = true;
        }

        return new ResponseEntity<>(
            "{\"status\":\"" + PushrHTTPresult.STATUS_SUCCESS + "\",\"msg\":" + (isSubscribed ? "true": "false") + "}",
            HttpStatus.OK
        );
    }




    private String createHash(ClientPushMsgSubscription clientSubscription) {
        byte[] bytesOfMessage = new byte[0];
        String hashtext = "";
        try {
            bytesOfMessage = clientSubscription.toString().getBytes("UTF-8");
            MessageDigest md = null;
            md = MessageDigest.getInstance("MD5");

            md.reset();
            md.update(bytesOfMessage);
            byte[] thedigest = md.digest();
            BigInteger bigInt = new BigInteger(1, thedigest);
            hashtext = bigInt.toString(16);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Now we need to zero pad it if you actually want the full 32 chars.
        while(hashtext.length() < 32 ){
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }


}
