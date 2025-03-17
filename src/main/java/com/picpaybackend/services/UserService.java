package com.picpaybackend.services;

import com.picpaybackend.domain.user.User;
import com.picpaybackend.domain.user.UserType;
import com.picpaybackend.dtos.UserDTO;
import com.picpaybackend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public void validateTransaction(User sender, BigDecimal amount) throws Exception {
        if (sender.getUserType() == UserType.MERCHANT || sender.getUserType() == null) {
            logger.warn("Tentativa de transação não autorizada por um logista: {}", sender.getId());
            throw new Exception("Logista não está autorizado a realizar transações");
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            logger.warn("Tentativa de transação com saldo insuficiente. Usuário: {}, Saldo: {}, Valor: {}",
                sender.getId(), sender.getBalance(), amount);
            throw new Exception("Saldo insuficiente");
        }
    }

    public User findUserById(final UUID id) {
        try {
            return repository.findUserById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com o ID: " + id));
        } catch (EntityNotFoundException e) {
            logger.error("Usuário não encontrado com o ID: {}", id, e);
            throw e;
        }
    }

    public User createUser(UserDTO data) {
        User newUser = new User(data);
        logger.info("Criando novo usuário: {}", newUser.getEmail());
        saveUser(newUser);
        return newUser;
    }

    public List<User> getAllUsers() {
        logger.info("Buscando todos os usuários cadastrados.");
        return repository.findAll();
    }

    public void saveUser(User user) {
        try {
            logger.info("Salvando usuário com ID: {}", user.getId());
            repository.save(user);
            logger.info("Usuário salvo com sucesso: {}", user.getId());
        } catch (RuntimeException e) {
            logger.error("Erro ao salvar usuário: {}", user.getId(), e);
            throw e;
        }
    }
}
