package de.benvorth.pushr.model.subscription;

import de.benvorth.pushr.model.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends BaseRepository<Subscription, Long> {

    boolean existsSubscriptionByUserIdAndEventId(long userId, long eventId);
    Subscription findSubscriptionByUserIdAndEventId(long userId, long eventId);

}
