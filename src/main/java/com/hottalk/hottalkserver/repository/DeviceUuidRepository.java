package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.CallCenter;
import com.hottalk.hottalkserver.model.DeviceUuid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceUuidRepository extends JpaRepository<DeviceUuid, Long> {
    Optional<DeviceUuid> findByDeviceUuid(String uuid);
}

