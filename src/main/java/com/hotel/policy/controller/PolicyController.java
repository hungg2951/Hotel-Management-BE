package com.hotel.policy.controller;

import com.hotel.common.response.ApiResponse;
import com.hotel.policy.dto.*;
import com.hotel.policy.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    // ================== CANCELLATION POLICIES ==================

    @GetMapping("/cancellation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<CancellationPolicyResponse>>> getCancellationPolicies() {
        List<CancellationPolicyResponse> list = policyService.getAllCancellationPolicies();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách chính sách hủy thành công"));
    }

    @PostMapping("/cancellation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> createCancellationPolicy(
            @Valid @RequestBody CancellationPolicyCreateRequest request
    ) {
        CancellationPolicyResponse res = policyService.createCancellationPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(res, "Tạo chính sách hủy mới thành công"));
    }

    @PutMapping("/cancellation/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> updateCancellationPolicy(
            @PathVariable Long id,
            @Valid @RequestBody CancellationPolicyUpdateRequest request
    ) {
        CancellationPolicyResponse res = policyService.updateCancellationPolicy(id, request);
        return ResponseEntity.ok(ApiResponse.success(res, "Cập nhật chính sách hủy thành công"));
    }

    @PutMapping({"/cancellation/{id}/activate", "/cancellation/{id}/default"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> activateCancellationPolicy(@PathVariable Long id) {
        CancellationPolicyResponse res = policyService.activateCancellationPolicy(id);
        return ResponseEntity.ok(ApiResponse.success(res, "Kích hoạt chính sách hủy thành công"));
    }

    @DeleteMapping("/cancellation/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCancellationPolicy(@PathVariable Long id) {
        policyService.deleteCancellationPolicy(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa chính sách hủy thành công"));
    }

    // ================== EARLY CHECKOUT POLICIES ==================

    @GetMapping("/early-checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<EarlyCheckoutPolicyResponse>>> getEarlyCheckoutPolicies() {
        List<EarlyCheckoutPolicyResponse> list = policyService.getAllEarlyCheckoutPolicies();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách chính sách trả phòng sớm thành công"));
    }

    @PostMapping("/early-checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EarlyCheckoutPolicyResponse>> createEarlyCheckoutPolicy(
            @Valid @RequestBody EarlyCheckoutPolicyCreateRequest request
    ) {
        EarlyCheckoutPolicyResponse res = policyService.createEarlyCheckoutPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(res, "Tạo chính sách trả phòng sớm mới thành công"));
    }

    @PutMapping("/early-checkout/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EarlyCheckoutPolicyResponse>> updateEarlyCheckoutPolicy(
            @PathVariable Long id,
            @Valid @RequestBody EarlyCheckoutPolicyUpdateRequest request
    ) {
        EarlyCheckoutPolicyResponse res = policyService.updateEarlyCheckoutPolicy(id, request);
        return ResponseEntity.ok(ApiResponse.success(res, "Cập nhật chính sách trả phòng sớm thành công"));
    }

    @PutMapping({"/early-checkout/{id}/activate", "/early-checkout/{id}/default"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EarlyCheckoutPolicyResponse>> activateEarlyCheckoutPolicy(@PathVariable Long id) {
        EarlyCheckoutPolicyResponse res = policyService.activateEarlyCheckoutPolicy(id);
        return ResponseEntity.ok(ApiResponse.success(res, "Kích hoạt chính sách trả phòng sớm thành công"));
    }

    @DeleteMapping("/early-checkout/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteEarlyCheckoutPolicy(@PathVariable Long id) {
        policyService.deleteEarlyCheckoutPolicy(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa chính sách trả phòng sớm thành công"));
    }
}
