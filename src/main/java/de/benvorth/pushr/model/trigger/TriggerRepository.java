package de.benvorth.pushr.model.trigger;

import de.benvorth.pushr.model.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriggerRepository extends CrudRepository<Trigger, Long> {

    List<Trigger> findByToken(String token);
    List<Trigger> findByTokenAndUser(String token, User user);
}

