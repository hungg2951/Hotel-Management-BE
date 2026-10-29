package com.hotel.housekeeping.repository;

import com.hotel.common.enums.HousekeepingTaskStatus;
import com.hotel.common.enums.HousekeepingTaskType;
import com.hotel.housekeeping.entity.HousekeepingTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {

    List<HousekeepingTask> findByRoomId(Long roomId);

    long countByStatus(HousekeepingTaskStatus status);

    long countByAssignedTo(Long assignedTo);

    long countByAssignedToAndStatus(Long assignedTo, HousekeepingTaskStatus status);

    @Query("SELECT t.assignedTo, COUNT(t) FROM HousekeepingTask t " +
           "WHERE t.status = 'DONE' AND t.assignedTo IS NOT NULL " +
           "GROUP BY t.assignedTo ORDER BY COUNT(t) DESC")
    List<Object[]> findTopPerformers();

    @Query("SELECT t FROM HousekeepingTask t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:taskType IS NULL OR t.taskType = :taskType) AND " +
           "(:roomId IS NULL OR t.roomId = :roomId) AND " +
           "(:assignedTo IS NULL OR t.assignedTo = :assignedTo) " +
           "ORDER BY t.createdAt DESC")
    Page<HousekeepingTask> searchTasks(
            @Param("status") HousekeepingTaskStatus status,
            @Param("taskType") HousekeepingTaskType taskType,
            @Param("roomId") Long roomId,
            @Param("assignedTo") Long assignedTo,
            Pageable pageable
    );
}
