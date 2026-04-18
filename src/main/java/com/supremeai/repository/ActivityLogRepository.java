package com.supremeai.repository;

import com.supremeai.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop100ByOrderByTimestampDesc();
    List<ActivityLog> findByCategoryOrderByTimestampDesc(String category);
    List<ActivityLog> findBySeverityOrderByTimestampDesc(String severity);
}
