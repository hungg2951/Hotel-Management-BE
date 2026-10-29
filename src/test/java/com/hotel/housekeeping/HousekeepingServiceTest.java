package com.hotel.housekeeping;

import com.hotel.auth.entity.Employee;
import com.hotel.auth.repository.EmployeeRepository;
import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.HousekeepingTaskStatus;
import com.hotel.common.enums.HousekeepingTaskType;
import com.hotel.common.exception.BusinessException;
import com.hotel.housekeeping.dto.HousekeepingSummaryDto;
import com.hotel.housekeeping.dto.HousekeepingTaskAssignRequest;
import com.hotel.housekeeping.dto.HousekeepingTaskCreateRequest;
import com.hotel.housekeeping.dto.HousekeepingTaskResponse;
import com.hotel.housekeeping.entity.HousekeepingTask;
import com.hotel.housekeeping.repository.HousekeepingTaskRepository;
import com.hotel.housekeeping.service.HousekeepingService;
import com.hotel.room.api.RoomApi;
import com.hotel.room.entity.Room;
import com.hotel.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HousekeepingServiceTest {

    @Mock
    private HousekeepingTaskRepository taskRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoomApi roomApi;

    @InjectMocks
    private HousekeepingService housekeepingService;

    private Room room;
    private Employee employee;
    private HousekeepingTask task;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setRoomId(101L);
        room.setRoomNumber("101");
        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);

        employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setFullName("Nguyen Van A");
        employee.setIsActive(true);

        task = new HousekeepingTask();
        task.setTaskId(10L);
        task.setRoomId(101L);
        task.setAssignedTo(1L);
        task.setStatus(HousekeepingTaskStatus.PENDING);
        task.setTaskType(HousekeepingTaskType.CLEANING);
    }

    @Test
    void testCreateTask_Success() {
        HousekeepingTaskCreateRequest req = new HousekeepingTaskCreateRequest(101L, HousekeepingTaskType.CLEANING, 1L, "Dọn gấp");
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(taskRepository.save(any(HousekeepingTask.class))).thenAnswer(i -> {
            HousekeepingTask t = i.getArgument(0);
            t.setTaskId(10L);
            return t;
        });

        HousekeepingTaskResponse resp = housekeepingService.createTask(req);
        assertNotNull(resp);
        assertEquals(10L, resp.taskId());
        assertEquals(HousekeepingTaskStatus.PENDING, resp.status());
    }

    @Test
    void testAssignTask_Success() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(taskRepository.save(any(HousekeepingTask.class))).thenReturn(task);
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        HousekeepingTaskResponse resp = housekeepingService.assignTask(10L, new HousekeepingTaskAssignRequest(1L));
        assertNotNull(resp);
        assertEquals(1L, resp.assignedTo());
    }

    @Test
    void testStartTask_Success() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(HousekeepingTask.class))).thenReturn(task);
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        HousekeepingTaskResponse resp = housekeepingService.startTask(10L);
        assertNotNull(resp);
        assertEquals(HousekeepingTaskStatus.IN_PROGRESS, task.getStatus());
        verify(roomApi).updateHousekeepingStatus(101L, HousekeepingStatus.CLEANING);
    }

    @Test
    void testCompleteTask_Success() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(HousekeepingTask.class))).thenReturn(task);
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        HousekeepingTaskResponse resp = housekeepingService.completeTask(10L);
        assertNotNull(resp);
        assertEquals(HousekeepingTaskStatus.DONE, task.getStatus());
        verify(roomApi).updateHousekeepingStatus(101L, HousekeepingStatus.CLEAN);
    }

    @Test
    void testCompleteTask_AlreadyDone_ThrowsException() {
        task.setStatus(HousekeepingTaskStatus.DONE);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> housekeepingService.completeTask(10L));
    }

    @Test
    void testGetSummary_Success() {
        when(taskRepository.count()).thenReturn(10L);
        when(taskRepository.countByStatus(HousekeepingTaskStatus.PENDING)).thenReturn(4L);
        when(taskRepository.countByStatus(HousekeepingTaskStatus.IN_PROGRESS)).thenReturn(2L);
        when(taskRepository.countByStatus(HousekeepingTaskStatus.DONE)).thenReturn(4L);

        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.DIRTY)).thenReturn(3L);
        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEAN)).thenReturn(5L);
        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEANING)).thenReturn(2L);
        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.INSPECTED)).thenReturn(1L);

        HousekeepingSummaryDto summary = housekeepingService.getSummary();
        assertNotNull(summary);
        assertEquals(10L, summary.totalTasks());
        assertEquals(4L, summary.pendingTasks());
        assertEquals(3L, summary.dirtyRooms());
    }
}
