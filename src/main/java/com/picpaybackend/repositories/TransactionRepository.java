package com.picpaybackend.repositories;

import com.picpaybackend.domain.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}
