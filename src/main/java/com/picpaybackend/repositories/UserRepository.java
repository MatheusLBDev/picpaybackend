package com.picpaybackend.repositories;

import com.picpaybackend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByDocument(String document);
    Optional<User> findUserById(UUID id);
}


