package de.benvorth.pushr.model.event;

import de.benvorth.pushr.model.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends CrudRepository<Event, Long> {

    List<Event> findByTrigger(String trigger);
    List<Event> findByUser(User user);
    List<Event> findByTriggerAndUser(String trigger, User user);
}

