package com.picpaybackend.services;

import com.picpaybackend.domain.user.User;
import com.picpaybackend.domain.user.UserType;
import com.picpaybackend.dtos.UserDTO;
import com.picpaybackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repository;

    @Autowired
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public void validateTransaction(User sender, BigDecimal amount) throws Exception {
        if(sender.getUserType() == UserType.MERCHANT){
            throw new Exception("Logista não está autorizado a realizar transações");
        }
        if(sender.getBalance().compareTo(amount) < 0){
            throw new Exception("Saldo insuficiente");
        }
    }

    public User findUserById(final UUID id) {
        return this.repository.findUserById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public User createUser(UserDTO data){
        User newUser = new User(data);
        this.saveUser(newUser);
        return  newUser;
    }

    public List<User> getAllUsers(){
        return this.repository.findAll();
    }

    public void saveUser(User user) {
        try {
            repository.save(user);
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
