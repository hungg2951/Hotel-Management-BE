package com.hotel.auth.service;

import com.hotel.auth.dto.*;
import com.hotel.auth.entity.AppRole;
import com.hotel.auth.entity.Employee;
import com.hotel.auth.entity.User;
import com.hotel.auth.repository.AppRoleRepository;
import com.hotel.auth.repository.EmployeeRepository;
import com.hotel.auth.repository.UserRepository;
import com.hotel.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AppRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            AppRoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void adminResetPassword(Long userId, AdminResetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng", "USER_NOT_FOUND"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessException.badRequest("Tên đăng nhập đã tồn tại trong hệ thống", "USERNAME_EXISTS");
        }

        AppRole role = roleRepository.findByRoleCode(request.role())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy vai trò: " + request.role(), "ROLE_NOT_FOUND"));

        Employee employee = new Employee();
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setPosition(request.position() != null ? request.position() : role.getRoleName());
        employee.setHireDate(LocalDate.now());
        employee.setIsActive(true);
        Employee savedEmployee = employeeRepository.save(employee);

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setIsActive(true);
        user.setRole(role);
        user.setEmployee(savedEmployee);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng", "USER_NOT_FOUND"));

        AppRole role = roleRepository.findByRoleCode(request.role())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy vai trò: " + request.role(), "ROLE_NOT_FOUND"));
        user.setRole(role);

        Employee employee = user.getEmployee();
        if (employee == null) {
            employee = new Employee();
            employee.setHireDate(LocalDate.now());
            employee.setIsActive(true);
        }
        employee.setFullName(request.fullName());
        if (request.email() != null) employee.setEmail(request.email());
        if (request.phone() != null) employee.setPhone(request.phone());
        if (request.position() != null) employee.setPosition(request.position());
        Employee savedEmployee = employeeRepository.save(employee);
        user.setEmployee(savedEmployee);

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng", "USER_NOT_FOUND"));

        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw BusinessException.badRequest("Không thể khóa tài khoản Quản trị viên tối cao (admin)", "CANNOT_LOCK_ADMIN");
        }

        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng", "USER_NOT_FOUND"));

        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw BusinessException.badRequest("Không thể xóa tài khoản Quản trị viên tối cao (admin)", "CANNOT_DELETE_ADMIN");
        }

        userRepository.delete(user);
    }

    private UserResponse mapToResponse(User user) {
        String fullName = user.getEmployee() != null ? user.getEmployee().getFullName() : user.getUsername();
        String email = user.getEmployee() != null ? user.getEmployee().getEmail() : "";
        String phone = user.getEmployee() != null ? user.getEmployee().getPhone() : "";
        String position = user.getEmployee() != null ? user.getEmployee().getPosition() : "";
        String roleCode = user.getRole() != null ? user.getRole().getRoleCode() : "STAFF";

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                fullName,
                email,
                phone,
                roleCode,
                position,
                Boolean.TRUE.equals(user.getIsActive()),
                user.getCreatedAt()
        );
    }
}
