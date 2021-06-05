package de.benvorth.pushr.model.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Spring Data JPA
@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    List<User> findByProviderId(String providerId);
    List<User> findByAccessToken_Token(String accessToken);

}
