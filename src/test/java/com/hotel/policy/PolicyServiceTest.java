package com.hotel.policy;

import com.hotel.common.enums.CancellationPenaltyType;
import com.hotel.common.enums.EarlyCheckoutPenaltyType;
import com.hotel.policy.dto.CancellationCalculationResult;
import com.hotel.policy.dto.CancellationPolicySnapshot;
import com.hotel.policy.dto.EarlyCheckoutCalculationResult;
import com.hotel.policy.dto.EarlyCheckoutPolicySnapshot;
import com.hotel.policy.repository.CancellationPolicyRepository;
import com.hotel.policy.repository.EarlyCheckoutPolicyRepository;
import com.hotel.policy.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private CancellationPolicyRepository cancellationPolicyRepository;

    @Mock
    private EarlyCheckoutPolicyRepository earlyCheckoutPolicyRepository;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(cancellationPolicyRepository, earlyCheckoutPolicyRepository);
    }

    @Test
    @DisplayName("Hủy miễn phí khi trước hạn freeCancelHours")
    void testCancellation_Free() {
        CancellationPolicySnapshot snapshot = new CancellationPolicySnapshot(
                1L, "FLEXIBLE", "Linh hoạt", 24, CancellationPenaltyType.ONE_NIGHT, BigDecimal.ZERO
        );
        LocalDateTime checkIn = LocalDateTime.now().plusHours(30);
        LocalDateTime cancelTime = LocalDateTime.now();

        CancellationCalculationResult result = policyService.calculateCancellation(
                snapshot, checkIn, cancelTime, 2000000L, 1000000L, 1000000L, null, null
        );

        assertTrue(result.isFreeCancel());
        assertEquals(0L, result.cancellationFee());
        assertEquals(1000000L, result.refundAmount());
        assertEquals(0L, result.shortfallAmount());
    }

    @Test
    @DisplayName("Hủy muộn bị phạt 1 đêm theo chính sách MODERATE")
    void testCancellation_PenaltyOneNight() {
        CancellationPolicySnapshot snapshot = new CancellationPolicySnapshot(
                2L, "MODERATE", "Tiêu chuẩn", 72, CancellationPenaltyType.ONE_NIGHT, BigDecimal.ZERO
        );
        LocalDateTime checkIn = LocalDateTime.now().plusHours(12); // Sát giờ check-in (<72h)
        LocalDateTime cancelTime = LocalDateTime.now();

        CancellationCalculationResult result = policyService.calculateCancellation(
                snapshot, checkIn, cancelTime, 3000000L, 1000000L, 1600000L, null, null
        );

        assertFalse(result.isFreeCancel());
        assertEquals(1000000L, result.cancellationFee());
        assertEquals(600000L, result.refundAmount()); // 1.600.000 - 1.000.000 = 600.000 hoàn lại
        assertEquals(0L, result.shortfallAmount());
    }

    @Test
    @DisplayName("Hủy chính sách NON_REFUNDABLE giữ toàn bộ cọc")
    void testCancellation_NonRefundable() {
        CancellationPolicySnapshot snapshot = new CancellationPolicySnapshot(
                4L, "NON_REFUNDABLE", "Không hoàn hủy", 0, CancellationPenaltyType.FULL_DEPOSIT, BigDecimal.ZERO
        );
        LocalDateTime checkIn = LocalDateTime.now().plusDays(5);
        LocalDateTime cancelTime = LocalDateTime.now();

        CancellationCalculationResult result = policyService.calculateCancellation(
                snapshot, checkIn, cancelTime, 5000000L, 1000000L, 2000000L, null, null
        );

        assertEquals(2000000L, result.cancellationFee());
        assertEquals(0L, result.refundAmount());
    }

    @Test
    @DisplayName("Hủy có can thiệp ngoại lệ của Quản lý")
    void testCancellation_ManagerOverride() {
        CancellationPolicySnapshot snapshot = new CancellationPolicySnapshot(
                2L, "MODERATE", "Tiêu chuẩn", 72, CancellationPenaltyType.ONE_NIGHT, BigDecimal.ZERO
        );
        LocalDateTime checkIn = LocalDateTime.now().plusHours(10);
        LocalDateTime cancelTime = LocalDateTime.now();

        CancellationCalculationResult result = policyService.calculateCancellation(
                snapshot, checkIn, cancelTime, 3000000L, 1000000L, 1000000L, 300000L, "Khách hoãn chuyến bay bão"
        );

        assertEquals(300000L, result.cancellationFee());
        assertEquals(700000L, result.refundAmount());
    }

    @Test
    @DisplayName("Trả sớm chính sách FLEXIBLE miễn phạt")
    void testEarlyCheckout_Flexible() {
        EarlyCheckoutPolicySnapshot snapshot = new EarlyCheckoutPolicySnapshot(
                1L, "FLEXIBLE", "Linh hoạt", EarlyCheckoutPenaltyType.NONE, BigDecimal.ZERO
        );

        EarlyCheckoutCalculationResult result = policyService.calculateEarlyCheckout(
                snapshot, 3, 1, 1000000L, null, null
        );

        assertEquals(2, result.unusedNights());
        assertEquals(2000000L, result.unusedRoomAmount());
        assertEquals(0L, result.earlyCheckoutPenalty());
        assertEquals(2000000L, result.netAdjustment()); // Khấu trừ đủ 2 triệu
    }

    @Test
    @DisplayName("Trả sớm chính sách STANDARD phạt 1 đêm")
    void testEarlyCheckout_Standard_OneNightPenalty() {
        EarlyCheckoutPolicySnapshot snapshot = new EarlyCheckoutPolicySnapshot(
                2L, "STANDARD", "Tiêu chuẩn", EarlyCheckoutPenaltyType.ONE_NIGHT, BigDecimal.ZERO
        );

        EarlyCheckoutCalculationResult result = policyService.calculateEarlyCheckout(
                snapshot, 4, 1, 800000L, null, null
        );

        assertEquals(3, result.unusedNights());
        assertEquals(2400000L, result.unusedRoomAmount()); // 3 đêm * 800k = 2.400.000
        assertEquals(800000L, result.earlyCheckoutPenalty()); // Phạt 1 đêm = 800k
        assertEquals(1600000L, result.netAdjustment()); // Khấu trừ 1.600.000
    }

    @Test
    @DisplayName("Trả sớm chính sách FULL_REMAINING thu đủ 100% không khấu trừ")
    void testEarlyCheckout_FullRemaining() {
        EarlyCheckoutPolicySnapshot snapshot = new EarlyCheckoutPolicySnapshot(
                3L, "FULL_CHARGE", "Thu đủ", EarlyCheckoutPenaltyType.FULL_REMAINING, BigDecimal.ZERO
        );

        EarlyCheckoutCalculationResult result = policyService.calculateEarlyCheckout(
                snapshot, 3, 1, 1000000L, null, null
        );

        assertEquals(2, result.unusedNights());
        assertEquals(2000000L, result.unusedRoomAmount());
        assertEquals(2000000L, result.earlyCheckoutPenalty()); // Phạt = toàn bộ tiền chưa ở
        assertEquals(0L, result.netAdjustment()); // Không giảm đồng nào
    }
}

