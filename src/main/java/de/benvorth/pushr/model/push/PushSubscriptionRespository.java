package de.benvorth.pushr.model.push;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PushSubscriptionRespository extends CrudRepository<PushSubscription, Long> {
    List<PushSubscription> findByUserId(Long userId);
}
