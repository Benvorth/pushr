package de.benvorth.pushr.pushService;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.push.PushSubscription;
import de.benvorth.pushr.model.push.PushSubscriptionRespository;
import de.benvorth.pushr.model.user.*;
import de.benvorth.pushr.pushService.dto.PushMessage;
import de.benvorth.pushr.pushService.dto.Subscription;
import de.benvorth.pushr.pushService.dto.SubscriptionEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping(path = "api")
public class PushMsgController {

    private final RestTemplate httpClient;
    private final ObjectMapper objectMapper;

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Subscription> subscriptionsById = new ConcurrentHashMap<>();
    private final Map<String, Subscription> tokenToSubscription = new ConcurrentHashMap<>();
    private String lastNumbersAPIFact = "";

    private final ServerKeys serverKeys;
    private final CryptoService cryptoService;
    private final Algorithm jwtAlgorithm;

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;
    PushSubscriptionRespository pushSubscriptionRespository;

    @Autowired
    public PushMsgController(ServerKeys serverKeys, CryptoService cryptoService,
                             ObjectMapper objectMapper,
                             UserRepository userRepository,
                             AccessTokenRepository accessTokenRepository,
                             PushSubscriptionRespository pushSubscriptionRespository
                          ) {
        this.serverKeys = serverKeys;
        this.cryptoService = cryptoService;
        this.httpClient = new RestTemplate();
        this.objectMapper = objectMapper;

        this.jwtAlgorithm = Algorithm.ECDSA256(this.serverKeys.getPublicKey(),
            this.serverKeys.getPrivateKey());

        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.pushSubscriptionRespository = pushSubscriptionRespository;
    }


    @RequestMapping(
        method = RequestMethod.GET,
        path = "/publicSigningKey",
        produces = "application/octet-stream"
    )
    public byte[] publicSigningKey(
        @RequestHeader("x-pushr-access-token") String accessToken // if not present: result is 400 - Bad Request
    ) {
        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return null;
        }
        return this.serverKeys.getPublicKeyUncompressed();
    }

    /*
    @GetMapping(path = "/publicSigningKeyBase64")
    public String publicSigningKeyBase64() {
        return this.serverKeys.getPublicKeyBase64();
    }*/


    @RequestMapping(
        method = RequestMethod.POST,
        path = "/subscribe",
        produces = "application/json"
    )
    public ResponseEntity<String> subscribe(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestBody Subscription subscription
    ) {
        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }
        AccessToken accessTokenObj = accessTokenRepository.findByToken(accessToken).get(0);
        User user = accessTokenObj.getUser();

        PushSubscription pushSubscription;
        List<PushSubscription> existingSubscriptions =
            pushSubscriptionRespository.findByEndpoint(subscription.getEndpoint());
        if (existingSubscriptions.size() == 1) {
            pushSubscription = existingSubscriptions.get(0);
        } else if (existingSubscriptions.size() > 1) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "More than one pushMsg subscription found for this endpoint").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else {
            pushSubscription = new PushSubscription(
                subscription.getEndpoint(),
                (subscription.getExpirationTime() != null ? subscription.getExpirationTime() : 0),
                subscription.getKeys().getP256dh(),
                subscription.getKeys().getAuth()
            );
            pushSubscriptionRespository.save(pushSubscription); // save "passive" side first
        }
        user.setPushSubscription(pushSubscription); // update "owning" side
        userRepository.save(user); // save "owning" side


        this.subscriptions.put(subscription.getEndpoint(), subscription);
        String subscriptionId = createHash(subscription);
        this.subscriptionsById.put(subscriptionId, subscription);
        return new ResponseEntity<>(
            "{\"subscriptionId\":\"" + subscriptionId + "\"}",
            HttpStatus.CREATED
        );
    }


    @RequestMapping(
        method = RequestMethod.POST,
        path = "/claimToken",
        produces = "application/json"
    )
    public ResponseEntity<String> claimToken(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("token") String token,
        @RequestParam("subscriptionEndpoint") String subscriptionEndpoint
        // @RequestBody Subscription subscription
    ) {
        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        // todo: check if we know the token
        if (false) {

            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No or unknown token provided").getJSON(),
                HttpStatus.CREATED
            );
        }

        Subscription subscription = this.getSubscriptionByEndpoint(subscriptionEndpoint);
        if (subscription != null) {
            tokenToSubscription.put(token, subscription);
            PushrApplication.logger.info("Token {} claimed successfully", token);
            // return this.sendTextPushMessage(subscription, new PushMessage("Text Notification", message));
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Token claimed").getJSON(),
                HttpStatus.CREATED
            );
        }

        PushrApplication.logger.info("Cant claim token {}: No subscriptionEndpoint provided", token);
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "Cannot claim token, no or unknown subscriptionEndpoint provided").getJSON(),
            HttpStatus.BAD_REQUEST
        );
    }


    @RequestMapping(
        method = RequestMethod.GET,
        path = "/push",
        produces = "application/json"
    )
    public ResponseEntity<String> push(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam(name="token", required = true) String token,
        @RequestParam(name="bat", required = false) String bat, // Batteriespannung
        @RequestParam(name="per", required = false) String per, // Batteriekapazität in %
        @RequestParam(name="mac", required = false) String mac, // MAC Adresse des Buttons (WiFi)
        @RequestParam(name="bssid", required = false) String bssid, // MAC Adresse des WiFi Access Points
        @RequestParam(name="ssid", required = false) String ssid, // SSID des WiFi Access Points
        @RequestParam(name="psk", required = false) String psk, // Pass Key des WiFi Access Points
        @RequestParam(name="blm", required = false) String blm, // MAC Adresse des stärksten iBeacons
        @RequestParam(name="blu", required = false) String blu, // UUID Kennung des stärksten iBeacons
        @RequestParam(name="blv", required = false) String blv, // Batterie Spannung des stärksten iBeacons (TLM Paket aktiv)
        @RequestParam(name="cpu", required = false) String cpu, // Counter Push
        @RequestParam(name="cap", required = false) String cap, //  Counter AP
        @RequestParam(name="cst", required = false) String cst, // Counter STA
        @RequestParam(name="cwp", required = false) String cwp, // Counter WPS
        @RequestParam(name="swver", required = false) String swver, // SW-version
        @RequestParam(name="hwver", required = false) String hwver // HW-version
    ) {
        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        PushrApplication.logger.info(
            "++++received push: \n" +
                "    token {}\n" +
                "    Batteriespannung {}\n" +
                "    Batteriekapazität in % {}\n" +
                "    MAC Adresse des Buttons (WiFi) {}\n" +
                "    MAC Adresse des WiFi Access Points {}\n" +
                "    SSID des WiFi Access Points {}\n" +
                "    Pass Key des WiFi Access Points {}\n" +
                "    MAC Adresse des stärksten iBeacons {}\n" +
                "    UUID Kennung des stärksten iBeacons {}\n" +
                "    Batterie Spannung des stärksten iBeacons {}\n" +
                "    Counter Push {}\n" +
                "    Counter AP {}\n" +
                "    Counter STA {}\n" +
                "    Counter WPS {}\n" +
                "    SW-version {}\n" +
                "    HW-version {}",

            token,
            (bat != null ? bat : "null"),
            (per != null ? per : "null"),
            (mac != null ? mac : "null"),
            (bssid != null ? bssid : "null"),
            (ssid != null ? ssid : "null"),
            (psk != null ? psk : "null"),
            (blm != null ? blm : "null"),
            (blu != null ? blu : "null"),
            (blv != null ? blv : "null"),
            (cpu != null ? cpu : "null"),
            (cap != null ? cap : "null"),
            (cst != null ? cst : "null"),
            (cwp != null ? cwp : "null"),
            (swver != null ? swver : "null"),
            (hwver != null ? hwver : "null")
        );

        if (tokenToSubscription.containsKey(token)) {
            Subscription subscription = this.tokenToSubscription.get(token);
            // todo check if this msg was delivered
            this.sendTextPushMessage(subscription, new PushMessage("Text Notification", "Hi from token " + token));
        }

        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "push message sent").getJSON(),
            HttpStatus.OK
        );
    }

    private String createHash(Subscription subscription) {
        byte[] bytesOfMessage = new byte[0];
        String hashtext = "";
        try {
            bytesOfMessage = subscription.toString().getBytes("UTF-8");
            MessageDigest md = null;
            md = MessageDigest.getInstance("MD5");

            md.reset();
            md.update(bytesOfMessage);
            byte[] thedigest = md.digest();
            BigInteger bigInt = new BigInteger(1, thedigest);
            hashtext = bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }  catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Now we need to zero pad it if you actually want the full 32 chars.
        while(hashtext.length() < 32 ){
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }


    @RequestMapping(
        method = RequestMethod.POST,
        path = "/unsubscribe",
        produces = "application/json"
    )
    public ResponseEntity<String> unsubscribe(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestBody SubscriptionEndpoint subscription
    ) {
        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        // todo check if this was successful
        // todo check if subscription needs to be removed from google-server, too?
        this.subscriptions.remove(subscription.getEndpoint());

        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Subscription for push messages removed from server").getJSON(),
            HttpStatus.UNAUTHORIZED
        );
    }


    @RequestMapping(
        method = RequestMethod.POST,
        path = "/isSubscribed",
        produces = "application/json"
    )
    public ResponseEntity<String> isSubscribed(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestBody SubscriptionEndpoint subscription
    ) {
        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        boolean isSubscribed = this.subscriptions.containsKey(subscription.getEndpoint());

        return new ResponseEntity<>(
            "{\"status\":\"" + PushrHTTPresult.STATUS_SUCCESS + "\",\"msg\":" + (isSubscribed ? "true": "false") + "}",
            HttpStatus.OK
        );
    }

    @RequestMapping(
        method = RequestMethod.POST,
        path = "/sendTextNotification",
        produces = "application/json"
    )
    public ResponseEntity<String> sendTextNotification(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("subscriptionEndpoint") String subscriptionEndpoint,
        @RequestBody String message
    ) {

        if (UserUtils.isInvalidToken(accessToken, accessTokenRepository)) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        Subscription subscription = this.getSubscriptionByEndpoint(subscriptionEndpoint);
        if (subscription != null) {
            boolean hasFailed = this.sendTextPushMessage(subscription, new PushMessage("Text Notification", message));
            if (!hasFailed) {
                return new ResponseEntity<>(
                    new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Push text message sent").getJSON(),
                    HttpStatus.OK
                );
            } else {
                return new ResponseEntity<>(
                    new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Could not sent push message, subscription gone?").getJSON(),
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        } else {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No or unknown subscription endpoint provided").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        }
    }

    /*
    // @Scheduled(fixedDelay = 20_000)
    public void numberFact() {
        if (this.subscriptions.isEmpty()) {
            return;
        }

        if (true) {
            this.lastNumbersAPIFact = "This is a static fact.";
            sendPushMessageToAllSubscribers(this.subscriptions,
                new PushMessage("A Joke", "hahaha")
            );
            // sendPushMessageToAllSubscribersWithoutPayload();
            return;
        }

        ResponseEntity<String> response = this.httpClient.getForEntity(
            "http://numbersapi.com/random/date", String.class);

        if (response.getStatusCodeValue() == 200) {
            this.lastNumbersAPIFact = response.getBody();
            sendPushMessageToAllSubscribersWithoutPayload();
        } else {
            PushrApplication.logger.error("fetch number fact: {}", response.toString());
        }
    }

    // @Scheduled(fixedDelay = 30_000)
    public void chuckNorrisJoke() {
        if (this.subscriptions.isEmpty()) {
            return;
        }


        ResponseEntity<String> response = this.httpClient.getForEntity(
            "https://api.icndb.com/jokes/random", String.class);

        if (response.getStatusCodeValue() == 200) {
            Map<String, Object> jokeJson = null;
            try {
                jokeJson = this.objectMapper.readValue(response.getBody(), Map.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) jokeJson.get("value");
            int id = (int) value.get("id");
            String joke = (String) value.get("joke");

            sendPushMessageToAllSubscribers(this.subscriptions,
                new PushMessage("Chuck Norris Joke: " + id, joke));

            Notification notification = new Notification("Chuck Norris Joke: " + id);
            notification.setBody(joke);
            notification.setIcon("assets/chuck.png");

            Map<String, Notification> singletonMap = new HashMap<>();
            singletonMap.put("notification", notification);

            // sendPushMessageToAllSubscribers(this.subscriptionsAngular,
            //     singletonMap);

        } else {
            PushrApplication.logger.error("fetch chuck norris {}", response.toString());
        }
    }
    */

    /*
    private void sendPushMessageToAllSubscribersWithoutPayload() {
        Set<String> failedSubscriptions = new HashSet<>();
        for (Subscription subscription : this.subscriptions.values()) {
            boolean remove = sendPushMessage(subscription, null);
            if (remove) {
                failedSubscriptions.add(subscription.getEndpoint());
            }
        }
        failedSubscriptions.forEach(this.subscriptions::remove);
    }

    private void sendPushMessageToAllSubscribers(Map<String, Subscription> subs,
                                                 Object message) {

        Set<String> failedSubscriptions = new HashSet<>();

        for (Subscription subscription : subs.values()) {
            try {
                byte[] result = this.cryptoService.encrypt(
                    this.objectMapper.writeValueAsString(message),
                    subscription.getKeys().getP256dh(), subscription.getKeys().getAuth(), 0);
                boolean remove = sendPushMessage(subscription, result);
                if (remove) {
                    failedSubscriptions.add(subscription.getEndpoint());
                }
            } catch (InvalidKeyException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | IllegalStateException
                | InvalidKeySpecException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException
                | JsonProcessingException e) {
                PushrApplication.logger.error("send encrypted message", e);
            }
        }

        failedSubscriptions.forEach(subs::remove);
    }
     */

    private boolean sendTextPushMessage(Subscription subscription, PushMessage message) {

        /*
        Notification notification = new Notification("Custom Text Notification");
        notification.setBody(messageBody);
        // notification.setIcon("assets/chuck.png");

        Map<String, Notification> singletonMap = new HashMap<>();
        singletonMap.put("notification", notification);
        */
        byte[] result = null;
        try {
            /*result = this.cryptoService.encrypt(
                this.objectMapper.writeValueAsString(singletonMap),
                subscription.getKeys().getP256dh(), subscription.getKeys().getAuth(), 0);
             */
            result = this.cryptoService.encrypt(
                this.objectMapper.writeValueAsString(message),
                subscription.getKeys().getP256dh(), subscription.getKeys().getAuth(), 0);

        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | JsonProcessingException e) {
            PushrApplication.logger.error("Error encrypting message: ", e);
        }

        return sendPushMessage(subscription, result);
    }

    /**
     * @return true if the subscription is no longer valid and can be removed, false if
     * everything is okay
     */
    private boolean sendPushMessage(Subscription subscription, byte[] body) {
        String origin;
        try {
            URL url = new URL(subscription.getEndpoint());
            origin = url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            PushrApplication.logger.error("create origin", e);
            return true;
        }

        Date today = new Date();
        Date expires = new Date(today.getTime() + 12 * 60 * 60 * 1000);

        String token = JWT.create().withAudience(origin).withExpiresAt(expires)
            .withSubject("mailto:example@example.com").sign(this.jwtAlgorithm);

        URI endpointURI = URI.create(subscription.getEndpoint());

        // HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();

        HttpHeaders headers = new HttpHeaders();
        headers.add("TTL", "180");
        headers.add("Authorization",
            "vapid t=" + token + ", k=" + this.serverKeys.getPublicKeyBase64());
        HttpEntity<byte[]> entity; // contains headers & body
        if (body != null) {
            // httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body));
            // .header("Content-Type", "application/octet-stream")
            // .header("Content-Encoding", "aes128gcm");

            headers.add("Content-Type", "application/octet-stream");
            headers.add("Content-Encoding", "aes128gcm");

            entity = new HttpEntity<>(body, headers);
        } else {
            // httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(""));
            // httpRequestBuilder.header("Content-Length", "0");

            headers.add("Content-Length", "0");
            entity = new HttpEntity<>(new byte[0], headers);
        }

        // HttpRequest request = httpRequestBuilder.uri(endpointURI)// .header("TTL", "180")
        // .header("Authorization",
        //     "vapid t=" + token + ", k=" + this.serverKeys.getPublicKeyBase64())
        //    .build();


        ResponseEntity<String> response = null;
        try {
            response = this.httpClient.postForEntity(
                endpointURI, entity, String.class);
        } catch (HttpClientErrorException e) {

            switch (e.getRawStatusCode()) {

                case 404:
                case 410:
                    PushrApplication.logger.warn("Subscription not found or gone: {}",
                        subscription.getEndpoint());
                    // remove subscription from our collection of subscriptions
                    removeSubscription(subscription);
                    return true;
                case 429:
                    PushrApplication.logger.error("Too many requests: {}", response.toString());
                    break;
                case 400:
                    PushrApplication.logger.error("Invalid request: {}", response.toString());
                    break;
                case 413:
                    PushrApplication.logger.error("Payload size too large: {}", response.toString());
                    break;
                default:
                    PushrApplication.logger.error("Unhandled status code: {} / {}",
                        e.getRawStatusCode(), e.toString());
            }
            return false;
        }

        // HttpResponse<Void> response = this.httpClient.send(request,
        //     HttpResponse.BodyHandlers.discarding());

        switch (response.getStatusCodeValue()) {
            case 201:
                PushrApplication.logger.info("Push message successfully sent: {}",
                    subscription.getEndpoint());
                break;
            case 404:
            case 410:
                PushrApplication.logger.warn("Subscription not found or gone: {}",
                    subscription.getEndpoint());
                // remove subscription from our collection of subscriptions
                return true;
            case 429:
                PushrApplication.logger.error("Too many requests: {}", response.toString());
                break;
            case 400:
                PushrApplication.logger.error("Invalid request: {}", response.toString());
                break;
            case 413:
                PushrApplication.logger.error("Payload size too large: {}", response.toString());
                break;
            default:
                PushrApplication.logger.error("Unhandled status code: {} / {}",
                    response.getStatusCodeValue(), response.toString());
        }


        return false;
    }

    private void removeSubscription(Subscription subscription) {
        this.subscriptions.remove(subscription.getEndpoint());
    }

    private Subscription getSubscriptionByEndpoint(String subscriptionEndpoint) {
        for (Subscription subscription : this.subscriptions.values()) {
            if (subscription.getEndpoint().equals(subscriptionEndpoint)) {
                return subscription;
            }
        }
        PushrApplication.logger.error("no subscription found for endpoind {}", subscriptionEndpoint);
        return null;
    }
}
