package com.picpaybackend.repositories;

import com.picpaybackend.domain.transaction.Transaction;
import com.picpaybackend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findTransactionsById(UUID id);
}
