package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.event.Event;
import de.benvorth.pushr.model.event.EventRepository;
import de.benvorth.pushr.model.message.PushMessage;
import de.benvorth.pushr.model.message.PushMessageRepository;
import de.benvorth.pushr.model.subscription.SubscriptionRepository;
import de.benvorth.pushr.model.user.AccessTokenRepository;
import de.benvorth.pushr.model.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api")
public class MessageController {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;
    DeviceRespository deviceRespository;
    EventRepository eventRepository;
    SubscriptionRepository subscriptionRepository;
    PushMessageRepository pushMessageRepository;

    ControllerUtil controllerUtil;

    public MessageController (
        UserRepository userRepository,
        AccessTokenRepository accessTokenRepository,
        DeviceRespository deviceRespository,
        EventRepository eventRepository,
        SubscriptionRepository subscriptionRepository,
        PushMessageRepository pushMessageRepository,
        ControllerUtil controllerUtil
    ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.deviceRespository = deviceRespository;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pushMessageRepository = pushMessageRepository;

        this.controllerUtil = controllerUtil;
    }

    /**
     * Read all messages for user associated with access-token
     * @param accessToken
     * @return
     */
    @Transactional
    @RequestMapping(
        method = RequestMethod.GET,
        path = "/message",
        produces = "application/json"
    )
    public ResponseEntity<String> getMessages(
        @RequestHeader("x-pushr-access-token") String accessToken
    ) {
        PushrApplication.logger.info("++ GET api/message");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        List<PushMessage> myMsgs = this.pushMessageRepository.findByUserIdOwner(userId);

        StringBuffer result = new StringBuffer();
        result.append("[");
        for (PushMessage msg : myMsgs) {
            result.append(msg.toJson());
            result.append(",");
        }
        if (myMsgs.size() > 0) {
            result.setLength(result.length() - 1);
        }
        result.append("]");

        PushrApplication.logger.info("++ GET api/message done");
        return new ResponseEntity<>(
            result.toString(),
            HttpStatus.OK
        );
    }

    /**
     * create or update a message
     * @param accessToken
     * @param messageId
     * @param eventId
     * @param title
     * @param body
     * @param icon
     * @param badge
     * @return
     */
    @Transactional
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/message",
        produces = "application/json"
    )
    public ResponseEntity<String> saveMessage(
        @RequestHeader("x-pushr-access-token") String accessToken,
        @RequestParam(name="message_id", required = false) Long messageId,
        @RequestParam(name="event_id", required = false) Long eventId,
        @RequestParam("title") String title,
        @RequestParam("body") String body,
        @RequestParam("icon") String icon,
        @RequestParam("badge") String badge

        ) {
        PushrApplication.logger.info("++ POST api/message");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        PushMessage newPushMessage;
        if (messageId == null) { // new message
            newPushMessage = new PushMessage(
                title, body,
                icon, badge, "",
                true, false, false,
                "auto", "en-en", "", "", "[200,80, 150]",
                System.currentTimeMillis(), System.currentTimeMillis(), userId,
                (eventId == null ? -1 : eventId)
            );
            this.pushMessageRepository.save(newPushMessage);

        } else { // update existing message

            Optional<PushMessage> pm = this.pushMessageRepository.findById(messageId);
            if (!pm.isPresent()) {
                return new ResponseEntity<>(
                    new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No message found by the given messageId").getJSON(),
                    HttpStatus.BAD_REQUEST
                );
            } else {
                newPushMessage = pm.get();
                if (newPushMessage.getUserIdOwner() != userId) {
                    return new ResponseEntity<>(
                        new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "This user is not allowed to modify the given message").getJSON(),
                        HttpStatus.UNAUTHORIZED
                    );
                }

                if (eventId != null && newPushMessage.getEventId() != eventId) {
                    newPushMessage.setEventId(eventId);
                }
                if (!newPushMessage.getTitle().equals(title)) {
                    newPushMessage.setTitle(title);
                }
                if (!newPushMessage.getBody().equals(body)) {
                    newPushMessage.setBody(body);
                }
                if (!newPushMessage.getIcon().equals(icon)) {
                    newPushMessage.setIcon(icon);
                }
                if (!newPushMessage.getBadge().equals(badge)) {
                    newPushMessage.setBadge(badge);
                }

            }
        }

        PushrApplication.logger.info("++ POST api/message done");

        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS,
                (messageId == null ? "Message created" : "Message updated"),
                newPushMessage.toJson()
            ).getJSON(),
            HttpStatus.CREATED
        );
    }

    @Transactional
    @RequestMapping(
        method = RequestMethod.DELETE,
        path = "/message",
        produces = "application/json"
    )
    public ResponseEntity<String> deleteMessage(
        @RequestHeader("x-pushr-access-token") String accessToken,
        @RequestParam("message_id") Long messageId
    ) {
        PushrApplication.logger.info("++ DELETE api/message");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        Optional<PushMessage> pm = this.pushMessageRepository.findById(messageId);
        if (!pm.isPresent()) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR,
                    "given message_id not found").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        }

        PushMessage pushMessage = pm.get();
        if (pushMessage.getUserIdOwner() != userId) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR,
                    "This user may not delete this message").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        this.pushMessageRepository.delete(pushMessage);

        PushrApplication.logger.info("Message {} deleted successfully", pushMessage.getMessageId());
        
        PushrApplication.logger.info("++ DELETE api/message done");
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS,
                "Message deleted",
                "{\"messageId\":" + pushMessage.getMessageId() + "}"
            ).getJSON(),
            HttpStatus.CREATED
        );

    }
}
