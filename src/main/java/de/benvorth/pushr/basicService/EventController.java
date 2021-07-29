package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import de.benvorth.pushr.model.PushrHTTPresult;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.event.Event;
import de.benvorth.pushr.model.event.EventRepository;
import de.benvorth.pushr.model.subscription.SubscriptionRepository;
import de.benvorth.pushr.model.user.AccessTokenRepository;
import de.benvorth.pushr.model.user.User;
import de.benvorth.pushr.model.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/event")

public class EventController {

    UserRepository userRepository;
    AccessTokenRepository accessTokenRepository;
    DeviceRespository deviceRespository;
    EventRepository eventRepository;
    SubscriptionRepository subscriptionRepository;

    ControllerUtil controllerUtil;

    @Autowired
    public EventController(UserRepository userRepository,
                           AccessTokenRepository accessTokenRepository,
                           DeviceRespository deviceRespository,
                           EventRepository eventRepository,
                           SubscriptionRepository subscriptionRepository,
                           ControllerUtil controllerUtil
    ) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.deviceRespository = deviceRespository;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.controllerUtil = controllerUtil;
    }

    @Transactional(readOnly = true)
    @RequestMapping(
        method = RequestMethod.GET,
        path = "/get_new_trigger",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_getNewTrigger(
        @RequestHeader("x-pushr-access-token") String accessToken
    ) {
        PushrApplication.logger.info("++ api/event/get_new_trigger");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        String newTrigger = controllerUtil.generateNewTrigger();

        if (newTrigger.length() == 0) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "Cannot generate new trigger").getJSON(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        } else {
            PushrApplication.logger.info("++ api/event/get_new_trigger done");
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, newTrigger).getJSON(),
                HttpStatus.OK
            );
        }
    }

    /**
     * Create or update an event
     * @param accessToken
     * @param eventId
     * @param trigger
     * @param eventName
     * @param triggerActive
     * @param subscribe
     * @return
     */
    @Transactional
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/save_event",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_createEvent(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam(name="event_id", required = false) Long eventId,
        @RequestParam(name="trigger", required = false) String trigger,
        @RequestParam("event_name") String eventName,
        @RequestParam("trigger_active") Boolean triggerActive,
        @RequestParam("subscribe") Boolean subscribe
    ) {
        PushrApplication.logger.info("++ api/event/save_event");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        // todo fill event_permission

        Event newEvent;
        if (eventId == null) { // new Event
            if (controllerUtil.isTriggerAssociatedWithEvent(trigger)) {
                return new ResponseEntity<>(
                    new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "given trigger already associated with an event").getJSON(),
                    HttpStatus.BAD_REQUEST
                );
            }

            if (trigger == null) {
                trigger = controllerUtil.generateNewTrigger();
            }

            newEvent = new Event(
                eventName,
                userId,
                System.currentTimeMillis(),
                trigger,
                triggerActive
            );
            eventRepository.save(newEvent);
            PushrApplication.logger.info("Event {} created successfully", newEvent.getEventId());

        } else { // updat existing event
            Optional<Event> optionalEvent = eventRepository.findById(eventId);
            if (!optionalEvent.isPresent()) {
                return new ResponseEntity<>(
                    new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No event found by the given eventId").getJSON(),
                    HttpStatus.BAD_REQUEST
                );
            } else {
                newEvent = optionalEvent.get();
                if (newEvent.getUserIdOwner() != userId) {
                    return new ResponseEntity<>(
                        new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "This user is not allowed to modify the given event").getJSON(),
                        HttpStatus.UNAUTHORIZED
                    );
                }

                if (trigger != null && !trigger.equals(newEvent.getTrigger())) {
                    newEvent.setTrigger(trigger);
                }
                if(!eventName.equals(newEvent.getName())) {
                    newEvent.setName(eventName);
                }
                if (triggerActive != newEvent.getTriggerActive()) {
                    newEvent.setTriggerActive(triggerActive);
                }
            }

        }

        // handle subscription of user to event
        controllerUtil.subscribeUnsubscribeToEvent(subscribe, userId, newEvent.getEventId());

        PushrApplication.logger.info("++ api/event/save_event done");
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS,
                (eventId == null ? "Event created" : "Event updated"),
                newEvent.toJson(true, subscribe)
            ).getJSON(),
            HttpStatus.CREATED
        );
    }

    @Transactional
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/delete_event",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_deleteEvent(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("event_id") Long eventId
    ) {
        PushrApplication.logger.info("++ api/event/delete_event");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (!eventOptional.isPresent()) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR,
                    "given event_id not found").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        }

        Event event = eventOptional.get();
        if (event.getUserIdOwner() != userId) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR,
                    "This user may not delete this event").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        eventRepository.delete(event);

        PushrApplication.logger.info("Event {} deleted successfully", event.getEventId());


        PushrApplication.logger.info("++ api/event/delete_event done");
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS,
                "Event deleted",
                "{\"event_id\":" + event.getEventId() + "}"
            ).getJSON(),
            HttpStatus.CREATED
        );
    }

    @RequestMapping(
        method = RequestMethod.GET,
        path = "/get_all_events",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_getAllEvents(
        @RequestHeader("x-pushr-access-token") String accessToken // if not present: result is 400 - Bad Request
    ) {
        PushrApplication.logger.info("++ api/event/get_all_events");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        // owned events
        List<Event> events_owned = eventRepository.findByUserIdOwner(userId);
        PushrApplication.logger.info("Found {} events owned by this user", events_owned.size());

        // todo get events_subscribed

        StringBuffer result = new StringBuffer();
        result.append("[");
        for (Event event : events_owned) {
            boolean subscribed = this.subscriptionRepository.existsSubscriptionByUserIdAndEventId(userId, event.getEventId());
            result.append(event.toJson(true, subscribed)); // todo check if user is subscribed to this events
            result.append(",");
        }
        if (events_owned.size() > 0) {
            result.setLength(result.length() - 1);
        }
        result.append("]");

        PushrApplication.logger.info("++ api/event/get_all_events done");
        return new ResponseEntity<>(
            result.toString(),
            HttpStatus.OK
        );
    }


    @RequestMapping(
        method = RequestMethod.GET,
        path = "/get_event_name_from_trigger",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_getEventNameFromTrigger(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("trigger") String trigger
    ) {
        PushrApplication.logger.info("++ api/event/get_event_name_from_trigger");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }
        User user = accessTokenRepository.findUserByToken(accessToken); // .findByAccessToken_Token(accessToken).get(0);

        // todo check permissions
        List<Event> events = eventRepository.findByTrigger(trigger);
        if (events.size() == 0) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No event found for this token").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else if (events.size() > 1) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "More than one event found for this token").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        } else {
            Event event = events.get(0);
            PushrApplication.logger.info("++ api/event/get_event_name_from_trigger done");
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, event.getName()).getJSON(),
                HttpStatus.OK
            );
        }
    }

    @RequestMapping(
        method = RequestMethod.GET,
        path = "/can_i_pull_this_trigger",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_canIPullThisTrigger(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("trigger") String trigger
    ) {
        PushrApplication.logger.info("++ api/event/can_i_pull_this_trigger");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        User user = accessTokenRepository.findUserByToken(accessToken); // .findByAccessToken_Token(accessToken).get(0);

        boolean result = false;
        // can't pull a trigger that does not exist
        if (controllerUtil.isTriggerAssociatedWithEvent(trigger)) {
            // todo implement logic
            // do I have TRIGGER permission to this event?
            result = true;
        }

        PushrApplication.logger.info("++ api/event/can_i_pull_this_trigger done");
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, result + "").getJSON(),
            HttpStatus.OK
        );
    }

    @RequestMapping(
        method = RequestMethod.GET,
        path = "/can_i_subscribe_to_this_event",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_canISubscribeToThisTrigger(
        @RequestHeader("x-pushr-access-token") String accessToken, // if not present: result is 400 - Bad Request
        @RequestParam("trigger") String trigger
    ) {
        PushrApplication.logger.info("++ api/event/can_i_subscribe_to_this_event");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        User user = accessTokenRepository.findUserByToken(accessToken); // .findByAccessToken_Token(accessToken).get(0);

        boolean result = false;
        // can't subscribe to an event that does not exist
        if (controllerUtil.isTriggerAssociatedWithEvent(trigger)) {
            // todo implement logic
            // do I have SUBSCRIBE permission to this event?
            result = true;
        }

        PushrApplication.logger.info("++ api/event/can_i_subscribe_to_this_event done");
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, result + "").getJSON(),
            HttpStatus.OK
        );
    }

    @Transactional
    @RequestMapping(
        method = RequestMethod.POST,
        path = "/subscribe_unsubscribe_to_event",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_subscribeUnsubscribeToEvent(
        @RequestHeader("x-pushr-access-token") String accessToken,
        @RequestParam(name="event_id") Long eventId,
        @RequestParam(name="subscribe") boolean subscribe
    ) {
        PushrApplication.logger.info("++ api/event/subscribe_unsubscribe_to_event");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        // todo check event_permission

        Event event;
        Optional<Event> optionalEvent = eventRepository.findById(eventId);
        if (!optionalEvent.isPresent()) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No event found by the given eventId").getJSON(),
                HttpStatus.BAD_REQUEST
            );
        }

        event = optionalEvent.get();
        if (event.getUserIdOwner() != userId) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "This user is not allowed to modify the given event").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        boolean success = controllerUtil.subscribeUnsubscribeToEvent(subscribe, userId, eventId);

        if (!success) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR,
                    "Error during " + (subscribe ? " subscribe" : " unsubscribed")
                ).getJSON(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        PushrApplication.logger.info("++ api/event/subscribe_unsubscribe_to_event done");
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS,
                (subscribe ? "Subscribed to" : "Unsubscribed from") + " event"
            ).getJSON(),
            HttpStatus.OK
        );
    }

    @RequestMapping(
        method = RequestMethod.GET,
        path = "/is_trigger_associated_with_event",
        produces = "application/json"
    )
    public ResponseEntity<String> rest_isTriggerAssociatedWithEvent(
        @RequestHeader("x-pushr-access-token") String accessToken,
        @RequestParam("trigger") String trigger
    ) {
        PushrApplication.logger.info("++ api/event/is_trigger_associated_with_event");
        Long userId = controllerUtil.getUserIdFromTokenValidation(accessToken);
        if (userId == null) {
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_ERROR, "No 'x-pushr-access-token' in header or unknown token").getJSON(),
                HttpStatus.UNAUTHORIZED
            );
        }

        PushrApplication.logger.info("++ api/event/is_trigger_associated_with_event done");
        return new ResponseEntity<>(
            new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, controllerUtil.isTriggerAssociatedWithEvent(trigger) + "").getJSON(),
            HttpStatus.OK
        );

    }

    /*
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

            List<Event> events = eventRepository.findByTokenAndUser(token, user);
            if (events.size() > 0) {
                return new ResponseEntity<>(
                    new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Token already claimed for this user").getJSON(),
                    HttpStatus.BAD_REQUEST
                );
            }

            Event newEvent = new Event(
                triggerName,
                System.currentTimeMillis(),
                token,
                Event.TriggerPermission.everyone,
                user
            );
            eventRepository.save(newEvent);

            PushrApplication.logger.info("Token {} claimed successfully", token);
            // return this.sendTextPushMessage(subscription, new PushMessage("Text Notification", message));
            return new ResponseEntity<>(
                new PushrHTTPresult(PushrHTTPresult.STATUS_SUCCESS, "Token claimed").getJSON(),
                HttpStatus.CREATED
            );

        }
    }*/

}
