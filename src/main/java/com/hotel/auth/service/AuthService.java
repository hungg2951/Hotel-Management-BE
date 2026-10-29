package com.hotel.auth.service;

import com.hotel.auth.dto.ChangePasswordRequest;
import com.hotel.auth.dto.LoginRequest;
import com.hotel.auth.dto.LoginResponse;
import com.hotel.auth.dto.UpdateProfileRequest;
import com.hotel.auth.entity.Employee;
import com.hotel.auth.entity.User;
import com.hotel.auth.repository.EmployeeRepository;
import com.hotel.auth.repository.UserRepository;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.security.CustomUserDetails;
import com.hotel.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final long expirationMs;

    public AuthService(
            @Lazy AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Kiểm tra trước: Nếu tài khoản tồn tại nhưng đã bị khóa, báo thẳng ngay lập tức
        var checkUserOpt = userRepository.findByUsername(request.username());
        if (checkUserOpt.isPresent()) {
            User existingUser = checkUserOpt.get();
            if (Boolean.FALSE.equals(existingUser.getIsActive())) {
                throw BusinessException.forbidden("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên để được hỗ trợ.", "ACCOUNT_LOCKED");
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (org.springframework.security.authentication.DisabledException | org.springframework.security.authentication.LockedException ex) {
            throw BusinessException.forbidden("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên để được hỗ trợ.", "ACCOUNT_LOCKED");
        } catch (BadCredentialsException ex) {
            throw BusinessException.unauthorized("Tên đăng nhập hoặc mật khẩu không chính xác", "INVALID_CREDENTIALS");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        Long employeeId = userDetails.getEmployeeId();
        String fullName = null;
        String email = null;
        String phone = null;

        // Cập nhật thời điểm đăng nhập gần nhất & lấy thông tin nhân viên
        var userOpt = userRepository.findById(userDetails.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            if (user.getEmployee() != null) {
                if (employeeId == null) {
                    employeeId = user.getEmployee().getEmployeeId();
                }
                fullName = user.getEmployee().getFullName();
                email = user.getEmployee().getEmail();
                phone = user.getEmployee().getPhone();
            }
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(
                token,
                "Bearer",
                expirationMs,
                userDetails.getUsername(),
                userDetails.getUserId(),
                roles,
                employeeId,
                fullName,
                email,
                phone
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse getCurrentUserProfile(CustomUserDetails userDetails) {
        Long employeeId = userDetails.getEmployeeId();
        String fullName = null;
        String email = null;
        String phone = null;

        var userOpt = userRepository.findById(userDetails.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getEmployee() != null) {
                if (employeeId == null) {
                    employeeId = user.getEmployee().getEmployeeId();
                }
                fullName = user.getEmployee().getFullName();
                email = user.getEmployee().getEmail();
                phone = user.getEmployee().getPhone();
            }
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(
                null,
                "Bearer",
                expirationMs,
                userDetails.getUsername(),
                userDetails.getUserId(),
                roles,
                employeeId,
                fullName,
                email,
                phone
        );
    }

    @Transactional
    public LoginResponse updateCurrentUserProfile(CustomUserDetails userDetails, UpdateProfileRequest request) {
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy thông tin tài khoản người dùng", "USER_NOT_FOUND"));

        Employee employee = user.getEmployee();
        if (employee == null) {
            employee = new Employee();
            employee.setHireDate(LocalDate.now());
            employee.setIsActive(true);
        }
        employee.setFullName(request.fullName().trim());
        if (request.email() != null) {
            employee.setEmail(request.email().trim());
        }
        if (request.phone() != null) {
            employee.setPhone(request.phone().trim());
        }
        Employee savedEmployee = employeeRepository.save(employee);
        user.setEmployee(savedEmployee);
        userRepository.save(user);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(
                null,
                "Bearer",
                expirationMs,
                user.getUsername(),
                user.getUserId(),
                roles,
                savedEmployee.getEmployeeId(),
                savedEmployee.getFullName(),
                savedEmployee.getEmail(),
                savedEmployee.getPhone()
        );
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy thông tin tài khoản người dùng", "USER_NOT_FOUND"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Mật khẩu hiện tại không chính xác", "INVALID_CURRENT_PASSWORD");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Mật khẩu mới không được trùng với mật khẩu hiện tại", "PASSWORD_UNCHANGED");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
