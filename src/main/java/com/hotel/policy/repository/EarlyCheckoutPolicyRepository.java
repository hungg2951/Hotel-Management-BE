package com.hotel.policy.repository;

import com.hotel.policy.entity.EarlyCheckoutPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EarlyCheckoutPolicyRepository extends JpaRepository<EarlyCheckoutPolicy, Long> {

    Optional<EarlyCheckoutPolicy> findByPolicyCode(String policyCode);

    Optional<EarlyCheckoutPolicy> findFirstByIsDefaultTrueAndIsActiveTrue();

    List<EarlyCheckoutPolicy> findByIsActiveTrue();

    List<EarlyCheckoutPolicy> findAllByOrderByPolicyIdAsc();
}

