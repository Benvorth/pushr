package de.benvorth.pushr.pushService;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.pushService.dto.Notification;
import de.benvorth.pushr.pushService.dto.PushMessage;
import de.benvorth.pushr.pushService.dto.Subscription;
import de.benvorth.pushr.pushService.dto.SubscriptionEndpoint;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PushController {

    private final RestTemplate httpClient;
    private final ObjectMapper objectMapper;

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Subscription> subscriptionsAngular = new ConcurrentHashMap<>();
    private String lastNumbersAPIFact = "";

    private final ServerKeys serverKeys;
    private final CryptoService cryptoService;
    private final Algorithm jwtAlgorithm;

    public PushController(ServerKeys serverKeys, CryptoService cryptoService,
                          ObjectMapper objectMapper) {
        this.serverKeys = serverKeys;
        this.cryptoService = cryptoService;
        this.httpClient = new RestTemplate();
        this.objectMapper = objectMapper;

        this.jwtAlgorithm = Algorithm.ECDSA256(this.serverKeys.getPublicKey(),
            this.serverKeys.getPrivateKey());
    }

    @GetMapping(path = "/publicSigningKey", produces = "application/octet-stream")
    public byte[] publicSigningKey() {
        return this.serverKeys.getPublicKeyUncompressed();
    }

    @GetMapping(path = "/publicSigningKeyBase64")
    public String publicSigningKeyBase64() {
        return this.serverKeys.getPublicKeyBase64();
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public void subscribe(@RequestBody Subscription subscription) {
        this.subscriptions.put(subscription.getEndpoint(), subscription);
    }

    @PostMapping("/subscribeAngular")
    @ResponseStatus(HttpStatus.CREATED)
    public void subscribeAngular(@RequestBody Subscription subscription) {
        System.out.println("subscribe: " + subscription);
        this.subscriptionsAngular.put(subscription.getEndpoint(), subscription);
    }

    @PostMapping("/unsubscribe")
    public void unsubscribe(@RequestBody SubscriptionEndpoint subscription) {
        this.subscriptions.remove(subscription.getEndpoint());
    }

    @PostMapping("/unsubscribeAngular")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribeAngular(@RequestBody SubscriptionEndpoint subscription) {
        System.out.println("unsubscribe: " + subscription);
        this.subscriptionsAngular.remove(subscription.getEndpoint());
    }

    @PostMapping("/isSubscribed")
    public boolean isSubscribed(@RequestBody SubscriptionEndpoint subscription) {
        return this.subscriptions.containsKey(subscription.getEndpoint());
    }

    @PostMapping("/isSubscribedAngular")
    public boolean isSubscribedAngular(@RequestBody SubscriptionEndpoint subscription) {
        return this.subscriptionsAngular.containsKey(subscription.getEndpoint());
    }

    @GetMapping(path = "/lastNumbersAPIFact")
    public String lastNumbersAPIFact() {
        return this.lastNumbersAPIFact;
    }

    @Scheduled(fixedDelay = 20_000)
    public void numberFact() {
        if (this.subscriptions.isEmpty()) {
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

    @Scheduled(fixedDelay = 30_000)
    public void chuckNorrisJoke() {
        if (this.subscriptions.isEmpty() && this.subscriptionsAngular.isEmpty()) {
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

            sendPushMessageToAllSubscribers(this.subscriptionsAngular,
                singletonMap);
        } else {
            PushrApplication.logger.error("fetch chuck norris {}", response.toString());
        }
    }

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


        ResponseEntity<String> response = this.httpClient.postForEntity(
            endpointURI, entity, String.class);

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
}
