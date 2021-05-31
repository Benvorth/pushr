package de.benvorth.pushr.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserIdRepository extends JpaRepository<User, Long> {
    List<User> findByUserId(String userId);
}
