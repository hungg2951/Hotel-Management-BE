package com.hotel.billing.service;

import com.hotel.billing.dto.*;
import com.hotel.billing.entity.Folio;
import com.hotel.billing.entity.FolioItem;
import com.hotel.billing.entity.HotelService;
import com.hotel.billing.entity.Invoice;
import com.hotel.billing.entity.Payment;
import com.hotel.billing.repository.FolioItemRepository;
import com.hotel.billing.repository.FolioRepository;
import com.hotel.billing.repository.HotelServiceRepository;
import com.hotel.billing.repository.InvoiceRepository;
import com.hotel.billing.repository.PaymentRepository;
import com.hotel.common.enums.FolioItemType;
import com.hotel.common.enums.FolioStatus;
import com.hotel.common.enums.PaymentStatus;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.security.CurrentUserApi;
import com.hotel.billing.api.FolioSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BillingService {

    private final FolioRepository folioRepository;
    private final FolioItemRepository folioItemRepository;
    private final HotelServiceRepository hotelServiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final BigDecimal defaultVatRate;

    public BillingService(
            FolioRepository folioRepository,
            FolioItemRepository folioItemRepository,
            HotelServiceRepository hotelServiceRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            @Value("${app.billing.vat-rate:8.00}") BigDecimal defaultVatRate
    ) {
        this.folioRepository = folioRepository;
        this.folioItemRepository = folioItemRepository;
        this.hotelServiceRepository = hotelServiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.defaultVatRate = defaultVatRate != null ? defaultVatRate : BigDecimal.valueOf(8.00);
    }

    @Transactional(readOnly = true)
    public Page<FolioSummaryDto> getFolios(FolioStatus status, Pageable pageable) {
        Page<Folio> page = (status != null)
                ? folioRepository.findByStatus(status, pageable)
                : folioRepository.findAll(pageable);

        return page.map(f -> {
            Long paid = calculateNetPaid(f.getFolioId());
            Long balance = f.getTotalAmount() - paid;
            return new FolioSummaryDto(
                    f.getFolioId(),
                    f.getStayId(),
                    f.getStatus(),
                    f.getTotalAmount(),
                    paid,
                    balance
            );
        });
    }

    @Transactional(readOnly = true)
    public FolioDetailResponse getFolioDetail(Long folioId) {
        Folio folio = findFolioEntityById(folioId);
        Long paidAmount = calculateNetPaid(folioId);
        Long balance = folio.getTotalAmount() - paidAmount;

        List<FolioItemResponse> items = folioItemRepository.findByFolioFolioIdOrderByPostedAtAsc(folioId)
                .stream()
                .map(FolioItemResponse::fromEntity)
                .toList();

        List<PaymentResponse> payments = paymentRepository.findByFolioFolioIdOrderByPaidAtAsc(folioId)
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();

        InvoiceResponse invoice = invoiceRepository.findByFolioFolioId(folioId)
                .map(InvoiceResponse::fromEntity)
                .orElse(null);

        return FolioDetailResponse.of(folio, paidAmount, balance, items, payments, invoice);
    }

    @Transactional(readOnly = true)
    public FolioDetailResponse getFolioDetailByStayId(Long stayId) {
        Folio folio = folioRepository.findByStayId(stayId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio cho lượt lưu trú: " + stayId));
        return getFolioDetail(folio.getFolioId());
    }

    @Transactional
    public FolioItemResponse addFolioItem(Long folioId, FolioItemRequest request) {
        Folio folio = folioRepository.findByIdWithLock(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));

        if (folio.getStatus() != FolioStatus.OPEN) {
            throw BusinessException.invalidState("Không thể thêm khoản phí vào Folio đã đóng (Trạng thái: " + folio.getStatus() + ")");
        }

        HotelService service = null;
        Long unitPrice;
        String description = request.description();

        if (request.itemType() == FolioItemType.SERVICE) {
            if (request.serviceId() == null) {
                throw BusinessException.badRequest("Cần chỉ định mã dịch vụ (serviceId) đối với khoản phí dịch vụ");
            }
            service = hotelServiceRepository.findById(request.serviceId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy dịch vụ khách sạn có mã: " + request.serviceId()));

            if (Boolean.FALSE.equals(service.getIsActive())) {
                throw BusinessException.badRequest("Dịch vụ '" + service.getName() + "' hiện đang tạm ngưng phục vụ");
            }

            unitPrice = request.unitPrice() != null ? request.unitPrice() : service.getUnitPrice();
            if (description == null || description.isBlank()) {
                description = service.getName();
            }
        } else {
            if (request.unitPrice() == null) {
                throw BusinessException.badRequest("Cần nhập đơn giá cho khoản phí");
            }
            unitPrice = request.unitPrice();
        }

        int quantity = (request.quantity() != null && request.quantity() > 0) ? request.quantity() : 1;

        // Tính amount: Đối với DISCOUNT amount âm (theo schema Oracle check), các loại khác amount dương
        long calculatedAmount;
        if (request.itemType() == FolioItemType.DISCOUNT) {
            calculatedAmount = -1L * Math.abs((long) quantity * unitPrice);
        } else {
            calculatedAmount = Math.abs((long) quantity * unitPrice);
        }

        FolioItem item = new FolioItem();
        item.setFolio(folio);
        item.setItemType(request.itemType());
        item.setService(service);
        item.setDescription(description != null ? description.trim() : null);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setAmount(calculatedAmount);
        item.setPostedAt(LocalDateTime.now());
        item.setPostedBy(CurrentUserApi.getCurrentUserId().orElse(null));

        FolioItem savedItem = folioItemRepository.save(item);

        // Cập nhật tổng tiền Folio
        folio.setTotalAmount(folio.getTotalAmount() + calculatedAmount);
        folioRepository.save(folio);

        return FolioItemResponse.fromEntity(savedItem);
    }

    @Transactional
    public void removeFolioItem(Long folioId, Long itemId) {
        Folio folio = folioRepository.findByIdWithLock(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));

        if (folio.getStatus() != FolioStatus.OPEN) {
            throw BusinessException.invalidState("Không thể xóa khoản phí trong Folio đã đóng");
        }

        FolioItem item = folioItemRepository.findByItemIdAndFolioFolioId(itemId, folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy khoản phí có mã " + itemId + " trong Folio #" + folioId));

        if (item.getItemType() == FolioItemType.ROOM) {
            throw BusinessException.badRequest("Không được phép xóa trực tiếp khoản tiền thuê phòng. Nếu cần điều chỉnh, vui lòng áp dụng khoản Giảm trừ (DISCOUNT) hoặc thực hiện thủ tục Trả phòng sớm.");
        }

        // Trừ bớt khoản phí đã xóa khỏi tổng tiền Folio
        folio.setTotalAmount(folio.getTotalAmount() - item.getAmount());
        folioRepository.save(folio);

        folioItemRepository.delete(item);
    }

    @Transactional
    public PaymentResponse recordPayment(Long folioId, PaymentRequest request) {
        Folio folio = folioRepository.findByIdWithLock(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));

        if (folio.getStatus() != FolioStatus.OPEN) {
            throw BusinessException.invalidState("Không thể thực hiện thanh toán cho Folio đã đóng");
        }

        PaymentStatus status = request.status() != null ? request.status() : PaymentStatus.SUCCESS;
        Long currentPaid = calculateNetPaid(folioId);
        Long currentBalance = folio.getTotalAmount() - currentPaid;

        if (status == PaymentStatus.SUCCESS) {
            if (request.amount() > currentBalance) {
                throw BusinessException.paymentExceedsBalance(
                        "Số tiền thanh toán (" + request.amount() + " VNĐ) vượt quá số dư còn lại của Folio (" + currentBalance + " VNĐ)"
                );
            }
        } else if (status == PaymentStatus.REFUNDED) {
            if (request.amount() > currentPaid) {
                throw BusinessException.badRequest(
                        "Số tiền hoàn trả (" + request.amount() + " VNĐ) vượt quá số tiền khách đã thanh toán (" + currentPaid + " VNĐ)"
                );
            }
        }

        Payment payment = new Payment();
        payment.setFolio(folio);
        payment.setReservationId(request.reservationId());
        payment.setMethod(request.method());
        payment.setAmount(request.amount());
        payment.setStatus(status);
        payment.setReferenceCode(request.referenceCode() != null ? request.referenceCode().trim() : null);
        payment.setPaidAt(LocalDateTime.now());
        payment.setReceivedBy(CurrentUserApi.getCurrentUserId().orElse(null));

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Transactional
    public InvoiceResponse createInvoice(Long folioId, InvoiceRequest request) {
        Folio folio = findFolioEntityById(folioId);

        if (invoiceRepository.existsByFolioFolioId(folioId)) {
            throw BusinessException.conflict("Hóa đơn cho Folio #" + folioId + " đã được xuất trước đó");
        }

        Long currentPaid = calculateNetPaid(folioId);
        Long currentBalance = folio.getTotalAmount() - currentPaid;

        if (currentBalance != 0L) {
            throw BusinessException.folioNotSettled(
                    "Folio chưa được thanh toán hết (số dư còn lại: " + currentBalance + " VNĐ), không thể xuất hóa đơn"
            );
        }

        Long totalAmount = folio.getTotalAmount();
        if (totalAmount < 0) {
            throw BusinessException.badRequest("Tổng tiền Folio không hợp lệ để xuất hóa đơn VAT");
        }

        // Tính VAT: Giá niêm yết đã bao gồm VAT 8% (Rule 11)
        // vat_amount = round(totalAmount * vatRate / (100 + vatRate))
        // subtotal = totalAmount - vat_amount
        BigDecimal vatRate = defaultVatRate;
        double rateValue = vatRate.doubleValue();
        long vatAmount = Math.round(totalAmount * rateValue / (100.0 + rateValue));
        long subtotal = totalAmount - vatAmount;

        String invoiceNo = generateInvoiceNo();

        Invoice invoice = new Invoice();
        invoice.setFolio(folio);
        invoice.setInvoiceNo(invoiceNo);
        invoice.setBuyerName(request.buyerName() != null ? request.buyerName().trim() : null);
        invoice.setBuyerTaxCode(request.buyerTaxCode() != null ? request.buyerTaxCode().trim() : null);
        invoice.setBuyerAddress(request.buyerAddress() != null ? request.buyerAddress().trim() : null);
        invoice.setSubtotal(subtotal);
        invoice.setVatRate(vatRate.setScale(2, RoundingMode.HALF_UP));
        invoice.setVatAmount(vatAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setIssuedBy(CurrentUserApi.getCurrentUserId().orElse(null));

        // CHÚ Ý: Không đóng folio ở đây! Folio chỉ được đóng khi check-out tại Giai đoạn 5.
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(savedInvoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy hóa đơn có mã: " + invoiceId));
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByFolioId(Long folioId) {
        Invoice invoice = invoiceRepository.findByFolioFolioId(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy hóa đơn cho Folio: " + folioId));
        return InvoiceResponse.fromEntity(invoice);
    }

    public Folio findFolioEntityById(Long folioId) {
        return folioRepository.findById(folioId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy Folio có mã: " + folioId));
    }

    public Long calculateNetPaid(Long folioId) {
        Long success = paymentRepository.calculatePaidSuccessByFolioId(folioId);
        Long refunded = paymentRepository.calculateRefundedByFolioId(folioId);
        return (success != null ? success : 0L) - (refunded != null ? refunded : 0L);
    }

    private String generateInvoiceNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String invoiceNo;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            invoiceNo = "INV-" + datePart + "-" + suffix;
        } while (invoiceRepository.existsByInvoiceNo(invoiceNo));
        return invoiceNo;
    }
}
