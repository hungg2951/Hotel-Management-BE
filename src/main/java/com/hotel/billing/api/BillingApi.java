package com.hotel.billing.api;

/**
 * Public API của module Billing phục vụ các module khác (Booking, Frontdesk, Housekeeping, Report).
 */
public interface BillingApi {

    /**
     * Khởi tạo Folio mới cho lượt lưu trú khi check-in.
     *
     * @param stayId Mã lượt lưu trú
     * @return Mã Folio vừa tạo
     */
    Long openFolio(Long stayId);

    /**
     * Lấy tóm tắt công nợ của Folio theo mã Folio.
     */
    FolioSummaryDto getFolioSummary(Long folioId);

    /**
     * Lấy tóm tắt công nợ của Folio theo mã lượt lưu trú.
     */
    FolioSummaryDto getFolioSummaryByStayId(Long stayId);

    /**
     * Ghi nhận tiền phòng vào Folio (do hệ thống check-in/night-audit gọi).
     */
    void postRoomCharge(Long folioId, Long amount, String description, Long postedByUserId);

    /**
     * Ghi nhận khoản điều chỉnh (DISCOUNT hoặc PENALTY) vào Folio.
     */
    void postAdjustment(Long folioId, com.hotel.common.enums.FolioItemType type, Long amount, String description, Long postedByUserId);

    /**
     * Chuyển các khoản tiền đặt cọc từ Reservation sang liên kết vào Folio khi khách check-in.
     */
    void attachReservationDepositsToFolio(Long reservationId, Long folioId);

    /**
     * Kiểm tra Folio đã được thanh toán xong hoàn toàn chưa (balance == 0).
     */
    boolean isFolioSettled(Long folioId);

    /**
     * Đóng Folio tại thời điểm check-out. Chỉ đóng khi số dư đã cân bằng (balance == 0).
     */
    void closeFolio(Long folioId);

    /**
     * Ghi nhận một khoản đặt cọc cho đơn đặt phòng.
     */
    Long recordReservationDeposit(Long reservationId, Long amount, com.hotel.common.enums.PaymentMethod method, String referenceCode, Long receivedByUserId);

    /**
     * Tính tổng số tiền đặt cọc thành công (trừ hoàn cọc) của một đơn đặt phòng.
     */
    Long calculateTotalDepositByReservationId(Long reservationId);

    /**
     * Hoàn tiền đặt cọc cho một đơn đặt phòng.
     */
    void refundReservationDeposit(Long reservationId, Long refundAmount, String referenceCode, Long refundedByUserId);

    /**
     * Lấy danh sách các giao dịch thanh toán/đặt cọc của một đơn đặt phòng.
     */
    java.util.List<PaymentInfoDto> getReservationPayments(Long reservationId);
}
