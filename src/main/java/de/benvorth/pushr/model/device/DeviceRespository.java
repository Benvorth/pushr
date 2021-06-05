package de.benvorth.pushr.model.device;

import de.benvorth.pushr.model.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRespository extends CrudRepository<Device, Long> {

    List<Device> findByUser(User user);
    List<Device> findByEndpointAndUser(String endpoint, User user);

}
