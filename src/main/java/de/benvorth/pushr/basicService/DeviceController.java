package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.device.client.ClientPushMsgSubscription;
import de.benvorth.pushr.model.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    @Transactional
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/register",
        produces = "application/json"
    )
    public ResponseEntity<String> register(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("device_name") String deviceName,
        @RequestParam("uuid") String uuid,
        @RequestParam("device_type") String deviceType,
        @RequestBody ClientPushMsgSubscription clientSubscription
    ) {
        PushrApplication.logger.info("++ This is api/device/register");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }
        List<Device> existingDevices =
            deviceRespository.findByUuidAndUserId(uuid, userId);
        if (existingDevices.size() == 0) {
            Device device = new Device(
                userId,
                uuid,
                deviceName,
                deviceType,
                clientSubscription.getEndpoint(),
                (clientSubscription.getExpirationTime() != null ? clientSubscription.getExpirationTime() : 0),
                clientSubscription.getKeys().getP256dh(),
                clientSubscription.getKeys().getAuth()
            );
            deviceRespository.save(device);
        } else if (existingDevices.size() > 1) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "More than one device found for this endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else {
            PushrApplication.logger.info("Device already exists, check for required updates");
            Device existingDevice = existingDevices.get(0);
            if (!deviceName.equals(existingDevice.getName())) {
                existingDevice.setName(deviceName);
            }
            if (!deviceType.equals(existingDevice.getDeviceType())) {
                existingDevice.setDeviceType(deviceType);
            }
            if (!clientSubscription.getEndpoint().equals(existingDevice.getEndpoint())) {
                existingDevice.setName(clientSubscription.getEndpoint());
            }
            if (!clientSubscription.getKeys().getP256dh().equals(existingDevice.getP256dh())) {
                existingDevice.setP256dh(clientSubscription.getKeys().getP256dh());
            }
            if (!clientSubscription.getKeys().getAuth().equals(existingDevice.getAuth())) {
                existingDevice.setAuth(clientSubscription.getKeys().getAuth());
            }
        }
        String subscriptionId = createHash(clientSubscription);

        PushrApplication.logger.info("++ api/device/register done");
        return new ResponseEntity<>(
            "{\"subscriptionId\":\"" + subscriptionId + "\"}",
            HttpStatus.CREATED
        );
    }

    @Transactional
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/unregister",
        produces = "application/json"
    )
    public ResponseEntity<String> unsubscribe(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestBody String subscriptionEndpoint
    ) {
        PushrApplication.logger.info("++ This is api/device/unregister");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        List<Device> existingDevices =
            deviceRespository.findByEndpointAndUserId(subscriptionEndpoint, userId);
        if (existingDevices.size() == 0) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "Unable to find subscription with the given endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else if (existingDevices.size() > 1) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "More than one device found for this endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else {

            Device device = existingDevices.get(0);
            deviceRespository.delete(device);

            // todo check if this was successful

            PushrApplication.logger.info("Subscription for push messages removed from server");
            // return this.sendTextPushMessage(subscription, new PushMessage("Text Notification", message));
            PushrApplication.logger.info("++ api/device/unregister done");
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Subscription for push messages removed from server").getJSON(),
                HttpStatus.OK
            );
        }
    }

    @Transactional(readOnly=true)
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/is_known",
        produces = "application/json"
    )
    public ResponseEntity<String> isSubscribed(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
//         @RequestBody SubscriptionEndpoint subscriptionEndpoint
        @RequestBody String deviceEndpoint
    ) {
        PushrApplication.logger.info("++ This is api/device/is_known");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        // User user = accessTokenRepository.findUserByToken(accessToken); // .findByAccessToken_Token(accessToken).get(0);
        boolean deviceIsKnown = deviceRespository.existsDeviceByEndpointAndUserId(deviceEndpoint, userId);
        // List<Device> devices = deviceRespository.findByEndpointAndUserId(deviceEndpoint, userId);
        /*if (devices.size() > 1) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "More than one device found for the given endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else if (devices.size() == 1) {
            isSubscribed = true;
        }*/

        PushrApplication.logger.info("++ api/device/is_known done");
        return new ResponseEntity<>(
            "{\"status\":\"" + PushrHTTPresult.STATUS_SUCCESS + "\",\"msg\":" + (deviceIsKnown ? "true": "false") + "}",
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
