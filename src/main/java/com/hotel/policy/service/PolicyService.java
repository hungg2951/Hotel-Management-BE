package com.hotel.policy.service;

import com.hotel.booking.repository.ReservationRepository;
import com.hotel.common.enums.CancellationPenaltyType;
import com.hotel.common.enums.EarlyCheckoutPenaltyType;
import com.hotel.common.exception.BusinessException;
import com.hotel.policy.dto.*;
import com.hotel.policy.entity.CancellationPolicy;
import com.hotel.policy.entity.EarlyCheckoutPolicy;
import com.hotel.policy.repository.CancellationPolicyRepository;
import com.hotel.policy.repository.EarlyCheckoutPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PolicyService {

    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final EarlyCheckoutPolicyRepository earlyCheckoutPolicyRepository;
    private final ReservationRepository reservationRepository;

    @Autowired
    public PolicyService(
            CancellationPolicyRepository cancellationPolicyRepository,
            EarlyCheckoutPolicyRepository earlyCheckoutPolicyRepository,
            ReservationRepository reservationRepository
    ) {
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.earlyCheckoutPolicyRepository = earlyCheckoutPolicyRepository;
        this.reservationRepository = reservationRepository;
    }

    public PolicyService(
            CancellationPolicyRepository cancellationPolicyRepository,
            EarlyCheckoutPolicyRepository earlyCheckoutPolicyRepository
    ) {
        this(cancellationPolicyRepository, earlyCheckoutPolicyRepository, null);
    }

    // ------------------- CANCELLATION POLICY CRUD -------------------

    @Transactional
    public List<CancellationPolicyResponse> getAllCancellationPolicies() {
        List<CancellationPolicy> all = cancellationPolicyRepository.findAllByOrderByPolicyIdAsc();
        boolean hasDefault = all.stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsDefault()));
        if (hasDefault) {
            all.forEach(p -> {
                boolean shouldBeActive = Boolean.TRUE.equals(p.getIsDefault());
                if (p.getIsActive() == null || p.getIsActive() != shouldBeActive) {
                    p.setIsActive(shouldBeActive);
                    cancellationPolicyRepository.save(p);
                }
            });
        }
        return all.stream()
                .map(CancellationPolicyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CancellationPolicy getCancellationPolicyEntity(Long id) {
        return cancellationPolicyRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chính sách hủy #" + id));
    }

    @Transactional(readOnly = true)
    public CancellationPolicy getActiveCancellationPolicy() {
        return cancellationPolicyRepository.findFirstByIsDefaultTrueAndIsActiveTrue()
                .orElseGet(() -> cancellationPolicyRepository.findByIsActiveTrue().stream().findFirst()
                        .orElseGet(() -> cancellationPolicyRepository.findAll().stream().findFirst()
                                .orElseGet(() -> {
                                    CancellationPolicy fallback = new CancellationPolicy();
                                    fallback.setPolicyCode("MODERATE");
                                    fallback.setName("Hủy Tiêu chuẩn (3 ngày)");
                                    fallback.setFreeCancelHours(72);
                                    fallback.setPenaltyType(CancellationPenaltyType.ONE_NIGHT);
                                    fallback.setPenaltyValue(BigDecimal.ZERO);
                                    fallback.setIsDefault(true);
                                    fallback.setIsActive(true);
                                    return fallback;
                                })));
    }

    @Transactional(readOnly = true)
    public CancellationPolicy getDefaultCancellationPolicy() {
        return getActiveCancellationPolicy();
    }

    @Transactional
    public CancellationPolicyResponse createCancellationPolicy(CancellationPolicyCreateRequest req) {
        cancellationPolicyRepository.findByPolicyCode(req.policyCode().trim().toUpperCase())
                .ifPresent(p -> {
                    throw BusinessException.badRequest("Mã chính sách hủy đã tồn tại: " + req.policyCode());
                });

        CancellationPolicy p = new CancellationPolicy();
        p.setPolicyCode(req.policyCode().trim().toUpperCase());
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setFreeCancelHours(req.freeCancelHours());
        p.setPenaltyType(req.penaltyType());
        p.setPenaltyValue(req.penaltyValue() != null ? req.penaltyValue() : BigDecimal.ZERO);

        boolean makeActive = Boolean.TRUE.equals(req.isActive()) || Boolean.TRUE.equals(req.isDefault());
        if (makeActive) {
            cancellationPolicyRepository.findAll().forEach(old -> {
                old.setIsDefault(false);
                old.setIsActive(false);
                cancellationPolicyRepository.save(old);
            });
            p.setIsDefault(true);
            p.setIsActive(true);
        } else {
            p.setIsDefault(false);
            p.setIsActive(false);
        }

        return CancellationPolicyResponse.from(cancellationPolicyRepository.save(p));
    }

    @Transactional
    public CancellationPolicyResponse activateCancellationPolicy(Long id) {
        CancellationPolicy target = getCancellationPolicyEntity(id);
        cancellationPolicyRepository.findAll().forEach(p -> {
            p.setIsDefault(false);
            p.setIsActive(false);
            cancellationPolicyRepository.save(p);
        });
        target.setIsDefault(true);
        target.setIsActive(true);
        return CancellationPolicyResponse.from(cancellationPolicyRepository.save(target));
    }

    @Transactional
    public CancellationPolicyResponse setDefaultCancellationPolicy(Long id) {
        return activateCancellationPolicy(id);
    }

    @Transactional
    public CancellationPolicyResponse updateCancellationPolicy(Long id, CancellationPolicyUpdateRequest req) {
        CancellationPolicy target = getCancellationPolicyEntity(id);
        target.setName(req.name().trim());
        target.setDescription(req.description());
        target.setFreeCancelHours(req.freeCancelHours());
        target.setPenaltyType(req.penaltyType());
        target.setPenaltyValue(req.penaltyValue() != null ? req.penaltyValue() : BigDecimal.ZERO);
        return CancellationPolicyResponse.from(cancellationPolicyRepository.save(target));
    }

    @Transactional
    public void deleteCancellationPolicy(Long id) {
        CancellationPolicy target = getCancellationPolicyEntity(id);
        if (Boolean.TRUE.equals(target.getIsActive())) {
            throw BusinessException.badRequest("Không thể xóa chính sách đang hoạt động. Vui lòng kích hoạt một chính sách khác trước khi xóa.");
        }
        if (reservationRepository != null) {
            reservationRepository.detachCancellationPolicy(id);
        }
        cancellationPolicyRepository.delete(target);
    }

    // ------------------- EARLY CHECKOUT POLICY CRUD -------------------

    @Transactional
    public List<EarlyCheckoutPolicyResponse> getAllEarlyCheckoutPolicies() {
        List<EarlyCheckoutPolicy> all = earlyCheckoutPolicyRepository.findAllByOrderByPolicyIdAsc();
        boolean hasDefault = all.stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsDefault()));
        if (hasDefault) {
            all.forEach(p -> {
                boolean shouldBeActive = Boolean.TRUE.equals(p.getIsDefault());
                if (p.getIsActive() == null || p.getIsActive() != shouldBeActive) {
                    p.setIsActive(shouldBeActive);
                    earlyCheckoutPolicyRepository.save(p);
                }
            });
        }
        return all.stream()
                .map(EarlyCheckoutPolicyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EarlyCheckoutPolicy getEarlyCheckoutPolicyEntity(Long id) {
        return earlyCheckoutPolicyRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chính sách trả phòng sớm #" + id));
    }

    @Transactional(readOnly = true)
    public EarlyCheckoutPolicy getActiveEarlyCheckoutPolicy() {
        return earlyCheckoutPolicyRepository.findFirstByIsDefaultTrueAndIsActiveTrue()
                .orElseGet(() -> earlyCheckoutPolicyRepository.findByIsActiveTrue().stream().findFirst()
                        .orElseGet(() -> earlyCheckoutPolicyRepository.findAll().stream().findFirst()
                                .orElseGet(() -> {
                                    EarlyCheckoutPolicy fallback = new EarlyCheckoutPolicy();
                                    fallback.setPolicyCode("FLEXIBLE");
                                    fallback.setName("Trả sớm Linh hoạt (Miễn phạt)");
                                    fallback.setPenaltyType(EarlyCheckoutPenaltyType.NONE);
                                    fallback.setPenaltyValue(BigDecimal.ZERO);
                                    fallback.setIsDefault(true);
                                    fallback.setIsActive(true);
                                    return fallback;
                                })));
    }

    @Transactional(readOnly = true)
    public EarlyCheckoutPolicy getDefaultEarlyCheckoutPolicy() {
        return getActiveEarlyCheckoutPolicy();
    }

    @Transactional
    public EarlyCheckoutPolicyResponse createEarlyCheckoutPolicy(EarlyCheckoutPolicyCreateRequest req) {
        earlyCheckoutPolicyRepository.findByPolicyCode(req.policyCode().trim().toUpperCase())
                .ifPresent(p -> {
                    throw BusinessException.badRequest("Mã chính sách trả phòng sớm đã tồn tại: " + req.policyCode());
                });

        EarlyCheckoutPolicy p = new EarlyCheckoutPolicy();
        p.setPolicyCode(req.policyCode().trim().toUpperCase());
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setPenaltyType(req.penaltyType());
        p.setPenaltyValue(req.penaltyValue() != null ? req.penaltyValue() : BigDecimal.ZERO);

        boolean makeActive = Boolean.TRUE.equals(req.isActive()) || Boolean.TRUE.equals(req.isDefault());
        if (makeActive) {
            earlyCheckoutPolicyRepository.findAll().forEach(old -> {
                old.setIsDefault(false);
                old.setIsActive(false);
                earlyCheckoutPolicyRepository.save(old);
            });
            p.setIsDefault(true);
            p.setIsActive(true);
        } else {
            p.setIsDefault(false);
            p.setIsActive(false);
        }

        return EarlyCheckoutPolicyResponse.from(earlyCheckoutPolicyRepository.save(p));
    }

    @Transactional
    public EarlyCheckoutPolicyResponse activateEarlyCheckoutPolicy(Long id) {
        EarlyCheckoutPolicy target = getEarlyCheckoutPolicyEntity(id);
        earlyCheckoutPolicyRepository.findAll().forEach(p -> {
            p.setIsDefault(false);
            p.setIsActive(false);
            earlyCheckoutPolicyRepository.save(p);
        });
        target.setIsDefault(true);
        target.setIsActive(true);
        return EarlyCheckoutPolicyResponse.from(earlyCheckoutPolicyRepository.save(target));
    }

    @Transactional
    public EarlyCheckoutPolicyResponse setDefaultEarlyCheckoutPolicy(Long id) {
        return activateEarlyCheckoutPolicy(id);
    }

    @Transactional
    public EarlyCheckoutPolicyResponse updateEarlyCheckoutPolicy(Long id, EarlyCheckoutPolicyUpdateRequest req) {
        EarlyCheckoutPolicy target = getEarlyCheckoutPolicyEntity(id);
        target.setName(req.name().trim());
        target.setDescription(req.description());
        target.setPenaltyType(req.penaltyType());
        target.setPenaltyValue(req.penaltyValue() != null ? req.penaltyValue() : BigDecimal.ZERO);
        return EarlyCheckoutPolicyResponse.from(earlyCheckoutPolicyRepository.save(target));
    }

    @Transactional
    public void deleteEarlyCheckoutPolicy(Long id) {
        EarlyCheckoutPolicy target = getEarlyCheckoutPolicyEntity(id);
        if (Boolean.TRUE.equals(target.getIsActive())) {
            throw BusinessException.badRequest("Không thể xóa chính sách đang hoạt động. Vui lòng kích hoạt một chính sách khác trước khi xóa.");
        }
        if (reservationRepository != null) {
            reservationRepository.detachEarlyCheckoutPolicy(id);
        }
        earlyCheckoutPolicyRepository.delete(target);
    }

    // ------------------- DYNAMIC POLICY SNAPSHOT RESOLVER -------------------

    /**
     * Lấy thông tin chính sách hủy mới nhất (đồng bộ với entity đang hoạt động)
     */
    public CancellationPolicySnapshot resolveLatestCancellationSnapshot(Long policyId, String existingSnapshotJson) {
        CancellationPolicy policy = null;
        if (policyId != null) {
            Optional<CancellationPolicy> opt = cancellationPolicyRepository.findById(policyId);
            if (opt.isPresent() && Boolean.TRUE.equals(opt.get().getIsActive())) {
                policy = opt.get();
            }
        }
        if (policy == null) {
            policy = getActiveCancellationPolicy();
        }
        return new CancellationPolicySnapshot(
                policy.getPolicyId(),
                policy.getPolicyCode(),
                policy.getName(),
                policy.getFreeCancelHours(),
                policy.getPenaltyType(),
                policy.getPenaltyValue()
        );
    }

    /**
     * Lấy thông tin chính sách trả phòng sớm mới nhất (đồng bộ với entity đang hoạt động)
     */
    public EarlyCheckoutPolicySnapshot resolveLatestEarlyCheckoutSnapshot(Long policyId, String existingSnapshotJson) {
        EarlyCheckoutPolicy policy = null;
        if (policyId != null) {
            Optional<EarlyCheckoutPolicy> opt = earlyCheckoutPolicyRepository.findById(policyId);
            if (opt.isPresent() && Boolean.TRUE.equals(opt.get().getIsActive())) {
                policy = opt.get();
            }
        }
        if (policy == null) {
            policy = getActiveEarlyCheckoutPolicy();
        }
        return new EarlyCheckoutPolicySnapshot(
                policy.getPolicyId(),
                policy.getPolicyCode(),
                policy.getName(),
                policy.getPenaltyType(),
                policy.getPenaltyValue()
        );
    }

    // ------------------- SNAPSHOT SERIALIZATION & PARSING -------------------

    public String createCancellationSnapshotJson(CancellationPolicy policy) {
        return String.format(
                "{\"policyId\":%d,\"policyCode\":\"%s\",\"name\":\"%s\",\"freeCancelHours\":%d,\"penaltyType\":\"%s\",\"penaltyValue\":%s}",
                policy.getPolicyId() != null ? policy.getPolicyId() : 0L,
                escapeJson(policy.getPolicyCode()),
                escapeJson(policy.getName()),
                policy.getFreeCancelHours() != null ? policy.getFreeCancelHours() : 72,
                policy.getPenaltyType() != null ? policy.getPenaltyType().name() : CancellationPenaltyType.ONE_NIGHT.name(),
                policy.getPenaltyValue() != null ? policy.getPenaltyValue().toPlainString() : "0"
        );
    }

    public CancellationPolicySnapshot parseCancellationSnapshot(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            CancellationPolicy def = getDefaultCancellationPolicy();
            return new CancellationPolicySnapshot(
                    def.getPolicyId(),
                    def.getPolicyCode(),
                    def.getName(),
                    def.getFreeCancelHours(),
                    def.getPenaltyType(),
                    def.getPenaltyValue()
            );
        }
        try {
            Long policyId = extractLong(json, "policyId");
            String policyCode = extractString(json, "policyCode");
            String name = extractString(json, "name");
            Integer freeCancelHours = extractInt(json, "freeCancelHours");
            String penaltyTypeStr = extractString(json, "penaltyType");
            CancellationPenaltyType penaltyType = CancellationPenaltyType.valueOf(penaltyTypeStr);
            BigDecimal penaltyValue = extractBigDecimal(json, "penaltyValue");

            return new CancellationPolicySnapshot(policyId, policyCode, name, freeCancelHours, penaltyType, penaltyValue);
        } catch (Exception e) {
            CancellationPolicy def = getDefaultCancellationPolicy();
            return new CancellationPolicySnapshot(
                    def.getPolicyId(),
                    def.getPolicyCode(),
                    def.getName(),
                    def.getFreeCancelHours(),
                    def.getPenaltyType(),
                    def.getPenaltyValue()
            );
        }
    }

    public String createEarlyCheckoutSnapshotJson(EarlyCheckoutPolicy policy) {
        return String.format(
                "{\"policyId\":%d,\"policyCode\":\"%s\",\"name\":\"%s\",\"penaltyType\":\"%s\",\"penaltyValue\":%s}",
                policy.getPolicyId() != null ? policy.getPolicyId() : 0L,
                escapeJson(policy.getPolicyCode()),
                escapeJson(policy.getName()),
                policy.getPenaltyType() != null ? policy.getPenaltyType().name() : EarlyCheckoutPenaltyType.NONE.name(),
                policy.getPenaltyValue() != null ? policy.getPenaltyValue().toPlainString() : "0"
        );
    }

    public EarlyCheckoutPolicySnapshot parseEarlyCheckoutSnapshot(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            EarlyCheckoutPolicy def = getDefaultEarlyCheckoutPolicy();
            return new EarlyCheckoutPolicySnapshot(
                    def.getPolicyId(),
                    def.getPolicyCode(),
                    def.getName(),
                    def.getPenaltyType(),
                    def.getPenaltyValue()
            );
        }
        try {
            Long policyId = extractLong(json, "policyId");
            String policyCode = extractString(json, "policyCode");
            String name = extractString(json, "name");
            String penaltyTypeStr = extractString(json, "penaltyType");
            EarlyCheckoutPenaltyType penaltyType = EarlyCheckoutPenaltyType.valueOf(penaltyTypeStr);
            BigDecimal penaltyValue = extractBigDecimal(json, "penaltyValue");

            return new EarlyCheckoutPolicySnapshot(policyId, policyCode, name, penaltyType, penaltyValue);
        } catch (Exception e) {
            EarlyCheckoutPolicy def = getDefaultEarlyCheckoutPolicy();
            return new EarlyCheckoutPolicySnapshot(
                    def.getPolicyId(),
                    def.getPolicyCode(),
                    def.getName(),
                    def.getPenaltyType(),
                    def.getPenaltyValue()
            );
        }
    }

    // ------------------- CORE CALCULATION ENGINES -------------------

    /**
     * Tính toán phí hủy và số tiền hoàn trả khách theo Snapshot Policy của Booking
     */
    public CancellationCalculationResult calculateCancellation(
            CancellationPolicySnapshot snapshot,
            LocalDateTime checkInDateTime,
            LocalDateTime cancelDateTime,
            long totalAmount,
            long oneNightTotal,
            long totalDeposit,
            Long customFee,
            String overrideReason
    ) {
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(cancelDateTime, checkInDateTime);
        boolean isFreeCancel = snapshot.freeCancelHours() != null && snapshot.freeCancelHours() > 0 && hoursUntilCheckIn >= snapshot.freeCancelHours();

        long fee;
        String explanation;

        if (customFee != null) {
            fee = Math.max(0, customFee);
            explanation = "Phí hủy được điều chỉnh bởi Quản lý: " + fee + " VNĐ" +
                    (overrideReason != null && !overrideReason.isBlank() ? " (Lý do: " + overrideReason.trim() + ")" : "");
        } else if (isFreeCancel) {
            fee = 0L;
            explanation = "Hủy trước giờ nhận phòng " + hoursUntilCheckIn + " giờ (chính sách yêu cầu trước " + snapshot.freeCancelHours() + "h). Miễn phí hủy hoàn toàn.";
        } else {
            CancellationPenaltyType type = snapshot.penaltyType() != null ? snapshot.penaltyType() : CancellationPenaltyType.ONE_NIGHT;
            switch (type) {
                case NONE -> {
                    fee = 0L;
                    explanation = "Chính sách miễn phí hủy phòng.";
                }
                case ONE_NIGHT -> {
                    fee = oneNightTotal;
                    explanation = "Hủy muộn sát ngày: Phạt 1 đêm tiền phòng (" + fee + " VNĐ).";
                }
                case FULL_DEPOSIT -> {
                    fee = totalDeposit;
                    explanation = "Hủy muộn sát ngày: Giữ lại toàn bộ số tiền đã đặt cọc (" + fee + " VNĐ).";
                }
                case PERCENT_TOTAL -> {
                    double pct = snapshot.penaltyValue() != null ? snapshot.penaltyValue().doubleValue() : 0.0;
                    fee = Math.round(totalAmount * (pct / 100.0));
                    explanation = "Hủy muộn sát ngày: Phạt " + pct + "% trên tổng tiền đơn (" + fee + " VNĐ).";
                }
                case PERCENT_DEPOSIT -> {
                    double pct = snapshot.penaltyValue() != null ? snapshot.penaltyValue().doubleValue() : 0.0;
                    fee = Math.round(totalDeposit * (pct / 100.0));
                    explanation = "Hủy muộn sát ngày: Phạt " + pct + "% trên tiền cọc (" + fee + " VNĐ).";
                }
                case FIXED_AMOUNT -> {
                    fee = snapshot.penaltyValue() != null ? snapshot.penaltyValue().longValue() : 0L;
                    explanation = "Hủy muộn sát ngày: Phạt số tiền cố định (" + fee + " VNĐ).";
                }
                default -> {
                    fee = oneNightTotal;
                    explanation = "Phạt 1 đêm tiền phòng (" + fee + " VNĐ).";
                }
            }
        }

        long refundAmount = Math.max(0L, totalDeposit - fee);
        long shortfallAmount = Math.max(0L, fee - totalDeposit);

        return new CancellationCalculationResult(
                isFreeCancel,
                fee,
                totalDeposit,
                refundAmount,
                shortfallAmount,
                snapshot.policyCode(),
                snapshot.name(),
                explanation
        );
    }

    /**
     * Tính toán điều chỉnh tiền phòng & phạt trả phòng sớm theo Snapshot Policy
     */
    public EarlyCheckoutCalculationResult calculateEarlyCheckout(
            EarlyCheckoutPolicySnapshot snapshot,
            long originalNights,
            long actualNights,
            long pricePerNight,
            Long customPenalty,
            String overrideReason
    ) {
        long unusedNights = Math.max(0L, originalNights - actualNights);
        long unusedRoomAmount = unusedNights * pricePerNight;

        if (unusedNights <= 0) {
            return new EarlyCheckoutCalculationResult(
                    originalNights,
                    actualNights,
                    0,
                    pricePerNight,
                    0,
                    0,
                    0,
                    snapshot.policyCode(),
                    snapshot.name(),
                    "Thời gian lưu trú thực tế đạt hoặc vượt thời gian dự kiến. Không tính phí trả sớm."
            );
        }

        long penalty;
        String explanation;

        if (customPenalty != null) {
            penalty = Math.max(0L, customPenalty);
            explanation = "Phí phạt trả sớm được điều chỉnh bởi Quản lý: " + penalty + " VNĐ" +
                    (overrideReason != null && !overrideReason.isBlank() ? " (Lý do: " + overrideReason.trim() + ")" : "");
        } else {
            EarlyCheckoutPenaltyType type = snapshot.penaltyType() != null ? snapshot.penaltyType() : EarlyCheckoutPenaltyType.NONE;
            switch (type) {
                case NONE -> {
                    penalty = 0L;
                    explanation = "Chính sách miễn phạt trả sớm. Khấu trừ toàn bộ " + unusedNights + " đêm chưa ở (" + unusedRoomAmount + " VNĐ).";
                }
                case ONE_NIGHT -> {
                    penalty = Math.min(pricePerNight, unusedRoomAmount);
                    explanation = "Phạt 1 đêm tiền phòng (" + penalty + " VNĐ). Khấu trừ số tiền phòng còn lại (" + (unusedRoomAmount - penalty) + " VNĐ).";
                }
                case PERCENT_REMAINING -> {
                    double pct = snapshot.penaltyValue() != null ? snapshot.penaltyValue().doubleValue() : 0.0;
                    penalty = Math.round(unusedRoomAmount * (pct / 100.0));
                    explanation = "Phạt " + pct + "% tiền phòng các đêm chưa ở (" + penalty + " VNĐ).";
                }
                case FULL_REMAINING -> {
                    penalty = unusedRoomAmount;
                    explanation = "Chính sách không hoàn tiền (Non-refundable). Thu đủ 100% tiền phòng như ban đầu (Không khấu trừ).";
                }
                default -> {
                    penalty = 0L;
                    explanation = "Miễn phí phạt trả phòng sớm.";
                }
            }
        }

        long netAdjustment = Math.max(0L, unusedRoomAmount - penalty);

        return new EarlyCheckoutCalculationResult(
                originalNights,
                actualNights,
                unusedNights,
                pricePerNight,
                unusedRoomAmount,
                penalty,
                netAdjustment,
                snapshot.policyCode(),
                snapshot.name(),
                explanation
        );
    }

    // ------------------- HELPERS -------------------

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (m.find()) return m.group(1);
        return "";
    }

    private Long extractLong(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (m.find()) return Long.parseLong(m.group(1));
        return 0L;
    }

    private Integer extractInt(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private BigDecimal extractBigDecimal(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9.]+)").matcher(json);
        if (m.find()) return new BigDecimal(m.group(1));
        return BigDecimal.ZERO;
    }
}
