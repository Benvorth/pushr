package de.benvorth.pushr.model.message;

import de.benvorth.pushr.model.BaseRepository;

import java.util.List;

public interface PushMessageRepository extends BaseRepository<PushMessage, Long> {
    List<PushMessage> findByUserIdOwner(Long userIdOwner);
}
