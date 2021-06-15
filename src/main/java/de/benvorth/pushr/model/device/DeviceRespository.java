package de.benvorth.pushr.model.device;

import de.benvorth.pushr.model.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRespository extends CrudRepository<Device, Long> {

    boolean existsDeviceByEndpointAndUserId(String endpoint, Long userId);

    List<Device> findByUserId(Long userId);
    List<Device> findByUuidAndUserId(String uuid, Long userId);
    List<Device> findByEndpointAndUserId(String endpoint, Long userId);

}
