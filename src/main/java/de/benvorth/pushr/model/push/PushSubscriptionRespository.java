package de.benvorth.pushr.model.push;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushSubscriptionRespository extends CrudRepository<PushSubscription, Long> {
    // List<PushSubscription> findByUserId(Long userId);
    List<PushSubscription> findByEndpoint(String endpoint);
}
