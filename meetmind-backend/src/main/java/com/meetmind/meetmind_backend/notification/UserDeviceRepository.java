package com.meetmind.meetmind_backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);
    Optional<UserDevice> findByUserIdAndFcmToken(Long userId, String fcmToken);
    void deleteByFcmToken(String fcmToken);
}
