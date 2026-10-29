package com.hotel.billing.service;

import com.hotel.billing.api.BillingApi;
import com.hotel.billing.api.FolioSummaryDto;
import com.hotel.billing.entity.Folio;
import com.hotel.billing.entity.FolioItem;
import com.hotel.billing.entity.Payment;
import com.hotel.billing.repository.FolioItemRepository;
import com.hotel.billing.repository.FolioRepository;
import com.hotel.billing.repository.PaymentRepository;
import com.hotel.common.enums.FolioItemType;
import com.hotel.common.enums.FolioStatus;
import com.hotel.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingApiImpl implements BillingApi {

    private final FolioRepository folioRepository;
    private final FolioItemRepository folioItemRepository;
    private final PaymentRepository paymentRepository;
    private final BillingService billingService;

    public BillingApiImpl(
            FolioRepository folioRepository,
            FolioItemRepository folioItemRepository,
            PaymentRepository paymentRepository,
            BillingService billingService
    ) {
        this.folioRepository = folioRepository;
        this.folioItemRepository = folioItemRepository;
        this.paymentRepository = paymentRepository;
        this.billingService = billingService;
    }

    @Override
    @Transactional
    public Long openFolio(Long stayId) {
        if (folioRepository.existsByStayId(stayId)) {
            throw BusinessException.duplicate("Folio cho lượt lưu trú #" + stayId + " đã tồn tại");
        }

        Folio folio = new Folio();
        folio.setStayId(stayId);
        folio.setStatus(FolioStatus.OPEN);
        folio.setTotalAmount(0L);
        folio.setCreatedAt(LocalDateTime.now());

        Folio saved = folioRepository.save(folio);
        return saved.getFolioId();
    }

    @Override
    @Transactional(readOnly = true)
    public FolioSummaryDto getFolioSummary(Long folioId) {
        Folio folio = billingService.findFolioEntityById(folioId);
        Long paid = billingService.calculateNetPaid(folioId);
        Long balance = folio.getTotalAmount() - paid;
        return new FolioSummaryDto(
                folio.getFolioId(),
                folio.getStayId(),
                folio.getStatus(),
                folio.getTotalAmount(),
                paid,
                balance
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FolioSummaryDto getFolioSummaryByStayId(Long stayId) {
        Folio folio = folioRepository.findByStayId(stayId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio cho lượt lưu trú #" + stayId));
        return getFolioSummary(folio.getFolioId());
    }

    @Override
    @Transactional
    public void postRoomCharge(Long folioId, Long amount, String description, Long postedByUserId) {
        Folio folio = folioRepository.findByIdWithLock(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));

        if (folio.getStatus() != FolioStatus.OPEN) {
            throw BusinessException.invalidState("Không thể ghi nhận tiền phòng vào Folio đã đóng");
        }

        long roomCharge = Math.abs(amount);

        FolioItem item = new FolioItem();
        item.setFolio(folio);
        item.setItemType(FolioItemType.ROOM);
        item.setDescription(description != null ? description.trim() : "Tiền phòng");
        item.setQuantity(1);
        item.setUnitPrice(roomCharge);
        item.setAmount(roomCharge);
        item.setPostedAt(LocalDateTime.now());
        item.setPostedBy(postedByUserId);

        folioItemRepository.save(item);

        folio.setTotalAmount(folio.getTotalAmount() + roomCharge);
        folioRepository.save(folio);
    }

    @Override
    @Transactional
    public void postAdjustment(Long folioId, FolioItemType type, Long amount, String description, Long postedByUserId) {
        Folio folio = folioRepository.findByIdWithLock(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));

        if (folio.getStatus() != FolioStatus.OPEN) {
            throw BusinessException.invalidState("Không thể ghi nhận điều chỉnh vào Folio đã đóng");
        }

        long absAmount = Math.abs(amount);
        long signedAmount = (type == FolioItemType.DISCOUNT) ? -absAmount : absAmount;

        FolioItem item = new FolioItem();
        item.setFolio(folio);
        item.setItemType(type != null ? type : FolioItemType.DISCOUNT);
        item.setDescription(description != null ? description.trim() : (type == FolioItemType.PENALTY ? "Phạt" : "Giảm trừ"));
        item.setQuantity(1);
        item.setUnitPrice(absAmount);
        item.setAmount(signedAmount);
        item.setPostedAt(LocalDateTime.now());
        item.setPostedBy(postedByUserId);

        folioItemRepository.save(item);

        folio.setTotalAmount(Math.max(0L, folio.getTotalAmount() + signedAmount));
        folioRepository.save(folio);
    }

    @Override
    @Transactional
    public void attachReservationDepositsToFolio(Long reservationId, Long folioId) {
        Folio folio = folioRepository.findByIdWithLock(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));

        List<Payment> deposits = paymentRepository.findByReservationIdAndFolioIsNull(reservationId);
        for (Payment deposit : deposits) {
            deposit.setFolio(folio);
            paymentRepository.save(deposit);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFolioSettled(Long folioId) {
        FolioSummaryDto summary = getFolioSummary(folioId);
        return summary.isSettled();
    }

    @Override
    @Transactional
    public void closeFolio(Long folioId) {
        Folio folio = folioRepository.findByIdWithLock(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));

        if (folio.getStatus() == FolioStatus.CLOSED) {
            return;
        }

        Long paid = billingService.calculateNetPaid(folioId);
        Long balance = folio.getTotalAmount() - paid;
        if (balance != 0L) {
            throw BusinessException.folioNotSettled(
                    "Không thể đóng Folio #" + folioId + " do công nợ chưa cân bằng (số dư còn lại: " + balance + " VNĐ)"
            );
        }

        folio.setStatus(FolioStatus.CLOSED);
        folio.setClosedAt(LocalDateTime.now());
        folioRepository.save(folio);
    }

    @Override
    @Transactional
    public Long recordReservationDeposit(Long reservationId, Long amount, com.hotel.common.enums.PaymentMethod method, String referenceCode, Long receivedByUserId) {
        if (amount == null || amount <= 0) {
            throw BusinessException.badRequest("Số tiền đặt cọc phải lớn hơn 0");
        }

        Payment payment = new Payment();
        payment.setReservationId(reservationId);
        payment.setMethod(method != null ? method : com.hotel.common.enums.PaymentMethod.TRANSFER);
        payment.setAmount(amount);
        payment.setStatus(com.hotel.common.enums.PaymentStatus.SUCCESS);
        payment.setReferenceCode(referenceCode != null ? referenceCode.trim() : null);
        payment.setPaidAt(LocalDateTime.now());
        payment.setReceivedBy(receivedByUserId);

        Payment saved = paymentRepository.save(payment);
        return saved.getPaymentId();
    }

    @Override
    @Transactional(readOnly = true)
    public Long calculateTotalDepositByReservationId(Long reservationId) {
        Long success = paymentRepository.calculateDepositSuccessByReservationId(reservationId);
        Long refunded = paymentRepository.calculateDepositRefundedByReservationId(reservationId);
        return (success != null ? success : 0L) - (refunded != null ? refunded : 0L);
    }

    @Override
    @Transactional
    public void refundReservationDeposit(Long reservationId, Long refundAmount, String referenceCode, Long refundedByUserId) {
        if (refundAmount == null || refundAmount <= 0) {
            return;
        }

        Long currentNetDeposit = calculateTotalDepositByReservationId(reservationId);
        if (refundAmount > currentNetDeposit) {
            throw BusinessException.badRequest("Số tiền hoàn cọc (" + refundAmount + " VNĐ) vượt quá số tiền cọc hiện có (" + currentNetDeposit + " VNĐ)");
        }

        Payment refund = new Payment();
        refund.setReservationId(reservationId);
        refund.setMethod(com.hotel.common.enums.PaymentMethod.TRANSFER);
        refund.setAmount(refundAmount);
        refund.setStatus(com.hotel.common.enums.PaymentStatus.REFUNDED);
        refund.setReferenceCode(referenceCode != null ? referenceCode.trim() : "HOAN_COC");
        refund.setPaidAt(LocalDateTime.now());
        refund.setReceivedBy(refundedByUserId);

        paymentRepository.save(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.hotel.billing.api.PaymentInfoDto> getReservationPayments(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).stream()
                .map(p -> new com.hotel.billing.api.PaymentInfoDto(
                        p.getPaymentId(),
                        p.getFolio() != null ? p.getFolio().getFolioId() : null,
                        p.getReservationId(),
                        p.getMethod(),
                        p.getAmount(),
                        p.getStatus(),
                        p.getReferenceCode(),
                        p.getPaidAt(),
                        p.getReceivedBy()
                ))
                .toList();
    }
}
