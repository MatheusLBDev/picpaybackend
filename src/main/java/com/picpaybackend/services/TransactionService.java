package com.picpaybackend.services;


import com.picpaybackend.domain.transaction.Transaction;
import com.picpaybackend.domain.user.User;
import com.picpaybackend.dtos.TransactionDTO;
import com.picpaybackend.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionService {
    private final UserService userService;
    private final TransactionRepository repository;
    private final RestTemplate restTemplate;

    @Autowired
    public TransactionService(UserService userService, TransactionRepository repository, RestTemplate restTemplate) {
        this.userService = userService;
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    public void createTransaction(TransactionDTO transaction) throws Exception{
        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());

        userService.validateTransaction(sender, transaction.value());

        boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());

        if(!isAuthorized){
            throw new Exception("Transação não autorizada");
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        repository.save(newTransaction);
        userService.saveUser(sender);
        userService.saveUser(receiver);

    }

    public boolean authorizeTransaction(User sender, BigDecimal value) {
        ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);
        if (authorizationResponse.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = authorizationResponse.getBody();
            if (body != null && body.containsKey("data") && "success".equals(body.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                return data.containsKey("authorization") && Boolean.TRUE.equals(data.get("authorization"));
            }
        } return false;
    }
}
