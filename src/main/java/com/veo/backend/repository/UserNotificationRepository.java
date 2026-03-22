package com.veo.backend.repository;

import com.veo.backend.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    @Query("""
            select n from UserNotification n
            where n.user.id = :userId
              and n.expiresAt > :now
            order by n.createdAt desc, n.id desc
            """)
    List<UserNotification> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("""
            select count(n) from UserNotification n
            where n.user.id = :userId
              and n.isRead = false
              and n.expiresAt > :now
            """)
    long countUnreadByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("""
            select n from UserNotification n
            where n.id = :notificationId
              and n.user.id = :userId
              and n.expiresAt > :now
            """)
    Optional<UserNotification> findActiveByIdAndUserId(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("""
            update UserNotification n
            set n.isRead = true
            where n.user.id = :userId
              and n.isRead = false
              and n.expiresAt > :now
            """)
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from UserNotification n where n.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
