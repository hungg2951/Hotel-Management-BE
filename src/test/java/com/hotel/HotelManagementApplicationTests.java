package com.hotel;

import com.hotel.auth.repository.AppRoleRepository;
import com.hotel.auth.repository.EmployeeRepository;
import com.hotel.auth.repository.UserRepository;
import com.hotel.billing.repository.*;
import com.hotel.booking.repository.*;
import com.hotel.frontdesk.repository.StayRepository;
import com.hotel.guest.repository.GuestRepository;
import com.hotel.housekeeping.repository.HousekeepingTaskRepository;
import com.hotel.room.repository.AmenityRepository;
import com.hotel.room.repository.RoomRepository;
import com.hotel.room.repository.RoomTypeAmenityRepository;
import com.hotel.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration,org.springframework.boot.orm.jpa.autoconfigure.HibernateJpaAutoConfiguration"
})
class HotelManagementApplicationTests {

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AppRoleRepository roleRepository;
    @MockitoBean private EmployeeRepository employeeRepository;

    @MockitoBean private RoomTypeRepository roomTypeRepository;
    @MockitoBean private RoomRepository roomRepository;
    @MockitoBean private AmenityRepository amenityRepository;
    @MockitoBean private RoomTypeAmenityRepository roomTypeAmenityRepository;

    @MockitoBean private GuestRepository guestRepository;

    @MockitoBean private ReservationRepository reservationRepository;
    @MockitoBean private ReservationRoomRepository reservationRoomRepository;
    @MockitoBean private RoomNightRepository roomNightRepository;

    @MockitoBean private StayRepository stayRepository;

    @MockitoBean private HotelServiceRepository hotelServiceRepository;
    @MockitoBean private FolioRepository folioRepository;
    @MockitoBean private FolioItemRepository folioItemRepository;
    @MockitoBean private PaymentRepository paymentRepository;
    @MockitoBean private InvoiceRepository invoiceRepository;

    @MockitoBean private HousekeepingTaskRepository housekeepingTaskRepository;

    @MockitoBean private com.hotel.policy.repository.CancellationPolicyRepository cancellationPolicyRepository;
    @MockitoBean private com.hotel.policy.repository.EarlyCheckoutPolicyRepository earlyCheckoutPolicyRepository;

    @Test
    void contextLoads() {
    }
}
