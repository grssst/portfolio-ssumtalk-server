package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices_uuid", indexes = {
        @Index(name = "idx_deleted_at", columnList = "deleted_at"),
})
public class DeviceUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_uuid", nullable = false, unique = true)
    private String deviceUuid;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public DeviceUuid() {}

    public DeviceUuid(String deviceUuid, LocalDateTime deletedAt) {
        this.deviceUuid = deviceUuid;
        this.deletedAt = deletedAt;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }
    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
