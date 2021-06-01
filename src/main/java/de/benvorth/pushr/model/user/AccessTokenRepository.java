package de.benvorth.pushr.model.user;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccessTokenRepository extends CrudRepository<AccessToken, Long> {
//     List<AccessToken> findById(Long id);
}
