package com.hotel.policy.repository;

import com.hotel.policy.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    Optional<CancellationPolicy> findByPolicyCode(String policyCode);

    Optional<CancellationPolicy> findFirstByIsDefaultTrueAndIsActiveTrue();

    List<CancellationPolicy> findByIsActiveTrue();

    List<CancellationPolicy> findAllByOrderByPolicyIdAsc();
}

