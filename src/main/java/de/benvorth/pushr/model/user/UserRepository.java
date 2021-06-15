package de.benvorth.pushr.model.user;

import de.benvorth.pushr.model.BaseRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Spring Data JPA
@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    boolean existsByProviderId(String providerId);
    List<User> findByProviderId(String providerId);
    // List<User> findByAccessToken_Token(String accessToken);

    // https://www.baeldung.com/spring-data-jpa-projections
    List<User.UserView_userId> getUserIdByProviderId(String providerId);

}
