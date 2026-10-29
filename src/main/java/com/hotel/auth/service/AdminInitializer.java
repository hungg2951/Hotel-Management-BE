package com.hotel.auth.service;

import com.hotel.auth.entity.AppRole;
import com.hotel.auth.entity.Employee;
import com.hotel.auth.entity.User;
import com.hotel.auth.repository.AppRoleRepository;
import com.hotel.auth.repository.EmployeeRepository;
import com.hotel.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.username:admin}")
    private String adminUsername;

    @Value("${app.initial-admin.password:Admin@Hotel2026!}")
    private String adminPassword;

    @Value("${app.initial-admin.email:admin@demohotel.vn}")
    private String adminEmail;

    public AdminInitializer(
            UserRepository userRepository,
            AppRoleRepository roleRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            // Lấy hoặc tạo vai trò ADMIN
            AppRole adminRole = roleRepository.findByRoleCode("ADMIN")
                    .orElseGet(() -> {
                        AppRole newRole = new AppRole("ADMIN", "Quản trị hệ thống");
                        AppRole saved = roleRepository.save(newRole);
                        return saved != null ? saved : newRole;
                    });

            // Tạo hoặc lấy Employee cho admin
            Employee adminEmployee = employeeRepository.findByEmail(adminEmail)
                    .orElseGet(() -> {
                        Employee emp = new Employee();
                        emp.setFullName("Admin User");
                        emp.setEmail(adminEmail);
                        emp.setPhone("0241234567");
                        emp.setPosition("Administrator");
                        emp.setHireDate(LocalDate.now());
                        emp.setSalary(20000000L);
                        emp.setIsActive(true);
                        return employeeRepository.save(emp);
                    });

            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            adminUser.setIsActive(true);
            adminUser.setRole(adminRole);
            adminUser.setEmployee(adminEmployee);
            adminUser.setCreatedAt(LocalDateTime.now());

            userRepository.save(adminUser);
            log.info("Khởi tạo thành công tài khoản quản trị viên mặc định: [{}]", adminUsername);
        } else {
            log.info("Tài khoản quản trị viên [{}] đã tồn tại trong hệ thống.", adminUsername);
        }

        // Khởi tạo các tài khoản nhân viên mẫu cho các bộ phận nếu chưa có
        createSampleUserIfNotExists("manager_ha", "Manager@2026!", "MANAGER", "Trần Thu Hà", "ha.tran@demohotel.vn", "0912345678", "Giám đốc Điều hành");
        createSampleUserIfNotExists("recept_nam", "Recept@2026!", "RECEPTIONIST", "Lê Văn Nam", "nam.le@demohotel.vn", "0988776655", "Nhân viên Lễ tân");
        createSampleUserIfNotExists("accountant_lan", "Account@2026!", "ACCOUNTANT", "Phạm Thị Lan", "lan.pham@demohotel.vn", "0933445566", "Kế toán trưởng");
        createSampleUserIfNotExists("housekeeper_mai", "House@2026!", "HOUSEKEEPER", "Hoàng Thị Mai", "mai.hoang@demohotel.vn", "0977889900", "Nhân viên Buồng phòng");
    }

    private void createSampleUserIfNotExists(
            String username,
            String password,
            String roleCode,
            String fullName,
            String email,
            String phone,
            String position
    ) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        AppRole role = roleRepository.findByRoleCode(roleCode)
                .orElseGet(() -> roleRepository.save(new AppRole(roleCode, roleCode)));

        Employee employee = employeeRepository.findByEmail(email)
                .orElseGet(() -> {
                    Employee emp = new Employee();
                    emp.setFullName(fullName);
                    emp.setEmail(email);
                    emp.setPhone(phone);
                    emp.setPosition(position);
                    emp.setHireDate(LocalDate.now());
                    emp.setSalary(12000000L);
                    emp.setIsActive(true);
                    return employeeRepository.save(emp);
                });

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setRole(role);
        user.setEmployee(employee);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Khởi tạo tài khoản nhân viên mẫu: [{}] ({})", username, roleCode);
    }
}
