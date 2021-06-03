package de.benvorth.pushr.model.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation

@Repository
public interface AccessTokenRepository extends CrudRepository<AccessToken, Long> {
    List<AccessToken> findByToken(String token);
    // List<AccessToken> findByUserId(Long userId);
    List<AccessToken> findByExpiresLessThan(Long timestamp);
}
