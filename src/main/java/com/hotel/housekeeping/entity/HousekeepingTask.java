package com.hotel.housekeeping.entity;

import com.hotel.common.enums.HousekeepingTaskStatus;
import com.hotel.common.enums.HousekeepingTaskType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "housekeeping_task")
public class HousekeepingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 15)
    private HousekeepingTaskType taskType = HousekeepingTaskType.CLEANING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private HousekeepingTaskStatus status = HousekeepingTaskStatus.PENDING;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public HousekeepingTask() {
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public HousekeepingTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(HousekeepingTaskType taskType) {
        this.taskType = taskType;
    }

    public HousekeepingTaskStatus getStatus() {
        return status;
    }

    public void setStatus(HousekeepingTaskStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
