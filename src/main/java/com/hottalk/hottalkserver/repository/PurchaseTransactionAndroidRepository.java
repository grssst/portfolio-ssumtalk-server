package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.PurchaseTransactionAndroid;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PurchaseTransactionAndroidRepository extends JpaRepository<PurchaseTransactionAndroid, Long>{

    boolean existsByOrderId(String orderId);

    PurchaseTransactionAndroid findByOrderId(String orderId);


}
