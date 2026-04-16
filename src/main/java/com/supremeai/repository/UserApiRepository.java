package com.supremeai.repository;

import com.supremeai.model.UserApi;
import com.supremeai.model.UserTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserApiRepository extends JpaRepository<UserApi, Long> {
    List<UserApi> findByUserId(String userId);
    List<UserApi> findByUserIdAndIsActive(String userId, Boolean isActive);
    Optional<UserApi> findByApiKey(String apiKey);
    List<UserApi> findByUserTier(UserTier userTier);

    @Query("SELECT ua FROM UserApi ua WHERE ua.currentUsage >= ua.monthlyQuota AND ua.isActive = true")
    List<UserApi> findQuotaExceededApis();

    @Modifying
    @Query("UPDATE UserApi ua SET ua.currentUsage = 0 WHERE MONTH(ua.updatedAt) != MONTH(:currentDate)")
    void resetMonthlyUsage(@Param("currentDate") LocalDateTime currentDate);
}