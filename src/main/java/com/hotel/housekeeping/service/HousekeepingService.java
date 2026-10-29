package com.hotel.housekeeping.service;

import com.hotel.auth.entity.Employee;
import com.hotel.auth.repository.EmployeeRepository;
import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.HousekeepingTaskStatus;
import com.hotel.common.enums.HousekeepingTaskType;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.security.CurrentUserApi;
import com.hotel.common.security.CustomUserDetails;
import com.hotel.housekeeping.dto.*;
import com.hotel.housekeeping.entity.HousekeepingTask;
import com.hotel.housekeeping.repository.HousekeepingTaskRepository;
import com.hotel.room.api.RoomApi;
import com.hotel.room.entity.Room;
import com.hotel.room.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HousekeepingService {

    private final HousekeepingTaskRepository taskRepository;
    private final RoomRepository roomRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomApi roomApi;

    public HousekeepingService(
            HousekeepingTaskRepository taskRepository,
            RoomRepository roomRepository,
            EmployeeRepository employeeRepository,
            RoomApi roomApi
    ) {
        this.taskRepository = taskRepository;
        this.roomRepository = roomRepository;
        this.employeeRepository = employeeRepository;
        this.roomApi = roomApi;
    }

    @Transactional
    public HousekeepingTaskResponse createTask(HousekeepingTaskCreateRequest request) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng #" + request.roomId()));

        if (request.assignedTo() != null) {
            Employee employee = employeeRepository.findById(request.assignedTo())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy nhân viên #" + request.assignedTo()));
            if (!Boolean.TRUE.equals(employee.getIsActive())) {
                throw BusinessException.badRequest("Nhân viên " + employee.getFullName() + " hiện đang bị khóa/nghỉ việc");
            }
        }

        HousekeepingTask task = new HousekeepingTask();
        task.setRoomId(room.getRoomId());
        task.setAssignedTo(request.assignedTo());
        task.setTaskType(request.taskType() != null ? request.taskType() : HousekeepingTaskType.CLEANING);
        task.setStatus(HousekeepingTaskStatus.PENDING);
        task.setNote(request.note());
        task.setCreatedAt(LocalDateTime.now());

        HousekeepingTask saved = taskRepository.save(task);

        // Nếu tạo việc dọn dẹp mà phòng đang SẠCH, tự động chuyển phòng thành DIRTY
        if ((task.getTaskType() == HousekeepingTaskType.CLEANING || task.getTaskType() == HousekeepingTaskType.DEEP_CLEAN)
                && (room.getHousekeepingStatus() == HousekeepingStatus.CLEAN || room.getHousekeepingStatus() == HousekeepingStatus.INSPECTED)) {
            roomApi.updateHousekeepingStatus(room.getRoomId(), HousekeepingStatus.DIRTY);
        }

        return toResponse(saved, room);
    }

    @Transactional
    public HousekeepingTaskResponse assignTask(Long taskId, HousekeepingTaskAssignRequest request) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy công việc dọn phòng #" + taskId));

        if (task.getStatus() == HousekeepingTaskStatus.DONE) {
            throw BusinessException.invalidState("Không thể phân công công việc đã hoàn thành");
        }

        Employee employee = employeeRepository.findById(request.assignedTo())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy nhân viên #" + request.assignedTo()));
        if (!Boolean.TRUE.equals(employee.getIsActive())) {
            throw BusinessException.badRequest("Nhân viên " + employee.getFullName() + " hiện đang bị khóa/nghỉ việc");
        }

        task.setAssignedTo(request.assignedTo());
        HousekeepingTask saved = taskRepository.save(task);

        Room room = roomRepository.findById(saved.getRoomId()).orElse(null);
        return toResponse(saved, room);
    }

    @Transactional
    public HousekeepingTaskResponse startTask(Long taskId) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy công việc dọn phòng #" + taskId));

        if (task.getStatus() == HousekeepingTaskStatus.DONE) {
            throw BusinessException.invalidState("Công việc đã hoàn thành, không thể bắt đầu lại");
        }

        checkHousekeeperPermission(task);

        task.setStatus(HousekeepingTaskStatus.IN_PROGRESS);
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }

        // Tự gán cho nhân viên hiện tại nếu việc chưa phân công
        if (task.getAssignedTo() == null) {
            CurrentUserApi.getCurrentEmployeeId().ifPresent(task::setAssignedTo);
        }

        HousekeepingTask saved = taskRepository.save(task);

        // Cập nhật trạng thái buồng phòng thành CLEANING
        roomApi.updateHousekeepingStatus(saved.getRoomId(), HousekeepingStatus.CLEANING);

        Room room = roomRepository.findById(saved.getRoomId()).orElse(null);
        return toResponse(saved, room);
    }

    @Transactional
    public HousekeepingTaskResponse completeTask(Long taskId) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy công việc dọn phòng #" + taskId));

        if (task.getStatus() == HousekeepingTaskStatus.DONE) {
            throw BusinessException.invalidState("Công việc đã được hoàn thành trước đó");
        }

        checkHousekeeperPermission(task);

        task.setStatus(HousekeepingTaskStatus.DONE);
        task.setFinishedAt(LocalDateTime.now());
        if (task.getStartedAt() == null) {
            task.setStartedAt(task.getFinishedAt());
        }

        HousekeepingTask saved = taskRepository.save(task);

        // Cập nhật trạng thái buồng phòng: INSPECTION -> INSPECTED, còn lại -> CLEAN
        HousekeepingStatus targetStatus = (task.getTaskType() == HousekeepingTaskType.INSPECTION)
                ? HousekeepingStatus.INSPECTED
                : HousekeepingStatus.CLEAN;
        roomApi.updateHousekeepingStatus(saved.getRoomId(), targetStatus);

        Room room = roomRepository.findById(saved.getRoomId()).orElse(null);
        return toResponse(saved, room);
    }

    @Transactional(readOnly = true)
    public Page<HousekeepingTaskResponse> getTasks(
            HousekeepingTaskStatus status,
            HousekeepingTaskType taskType,
            Long roomId,
            Long assignedTo,
            Pageable pageable
    ) {
        Page<HousekeepingTask> page = taskRepository.searchTasks(status, taskType, roomId, assignedTo, pageable);

        // Nạp trước danh sách Room và Employee để map hiệu quả
        List<Long> roomIds = page.getContent().stream().map(HousekeepingTask::getRoomId).distinct().toList();
        Map<Long, Room> roomMap = roomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(Room::getRoomId, r -> r));

        List<Long> employeeIds = page.getContent().stream()
                .map(HousekeepingTask::getAssignedTo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Employee> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e));

        return page.map(task -> {
            Room room = roomMap.get(task.getRoomId());
            Employee employee = task.getAssignedTo() != null ? employeeMap.get(task.getAssignedTo()) : null;
            return toResponseWithEntities(task, room, employee);
        });
    }

    @Transactional(readOnly = true)
    public HousekeepingSummaryDto getSummary() {
        long totalTasks = taskRepository.count();
        long pendingTasks = taskRepository.countByStatus(HousekeepingTaskStatus.PENDING);
        long inProgressTasks = taskRepository.countByStatus(HousekeepingTaskStatus.IN_PROGRESS);
        long doneTasks = taskRepository.countByStatus(HousekeepingTaskStatus.DONE);

        long dirtyRooms = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.DIRTY);
        long cleanRooms = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEAN);
        long cleaningRooms = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEANING);
        long inspectedRooms = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.INSPECTED);

        return new HousekeepingSummaryDto(
                totalTasks,
                pendingTasks,
                inProgressTasks,
                doneTasks,
                dirtyRooms,
                cleanRooms,
                cleaningRooms,
                inspectedRooms
        );
    }

    @Transactional(readOnly = true)
    public List<HousekeeperEmployeeDto> getEmployees() {
        return employeeRepository.findByIsActiveTrue().stream()
                .map(e -> new HousekeeperEmployeeDto(
                        e.getEmployeeId(),
                        e.getFullName(),
                        e.getPhone(),
                        e.getPosition()
                ))
                .toList();
    }

    private void checkHousekeeperPermission(HousekeepingTask task) {
        CustomUserDetails user = CurrentUserApi.getCurrentUser().orElse(null);
        if (user == null) {
            return;
        }

        boolean isManagerOrAdmin = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_MANAGER"));

        if (!isManagerOrAdmin) {
            // Nếu là HOUSEKEEPER hoặc vai trò khác, chỉ được thao tác nếu task phân công cho chính nhân viên đó
            Long currentEmpId = user.getEmployeeId();
            if (currentEmpId == null || (task.getAssignedTo() != null && !task.getAssignedTo().equals(currentEmpId))) {
                throw BusinessException.forbidden("Bạn không có quyền thực hiện công việc được phân công cho nhân viên khác");
            }
        }
    }

    private HousekeepingTaskResponse toResponse(HousekeepingTask task, Room room) {
        Employee employee = null;
        if (task.getAssignedTo() != null) {
            employee = employeeRepository.findById(task.getAssignedTo()).orElse(null);
        }
        return toResponseWithEntities(task, room, employee);
    }

    private HousekeepingTaskResponse toResponseWithEntities(HousekeepingTask task, Room room, Employee employee) {
        return new HousekeepingTaskResponse(
                task.getTaskId(),
                task.getRoomId(),
                room != null ? room.getRoomNumber() : null,
                room != null ? room.getFloorNo() : null,
                room != null && room.getRoomType() != null ? room.getRoomType().getName() : null,
                room != null ? room.getHousekeepingStatus() : null,
                task.getAssignedTo(),
                employee != null ? employee.getFullName() : null,
                task.getTaskType(),
                task.getStatus(),
                task.getNote(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getFinishedAt()
        );
    }
}
