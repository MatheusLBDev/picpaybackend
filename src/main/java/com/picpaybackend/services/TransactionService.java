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
    private final NotificationService notificationService;

    @Autowired
    public TransactionService(UserService userService, TransactionRepository repository, RestTemplate restTemplate, NotificationService notificationService) {
        this.userService = userService;
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.notificationService = notificationService;
    }

    public Transaction createTransaction(TransactionDTO transactionDTO) throws Exception{
        User sender = this.userService.findUserById(transactionDTO.senderId());
        User receiver = this.userService.findUserById(transactionDTO.receiverId());

        userService.validateTransaction(sender, transactionDTO.value());

        boolean isAuthorized = this.authorizeTransaction(sender, transactionDTO.value());

        if(!isAuthorized){
            throw new Exception("Transação não autorizada");
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDTO.value());
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transactionDTO.value()));
        receiver.setBalance(receiver.getBalance().add(transactionDTO.value()));

        repository.save(transaction);
        userService.saveUser(sender);
        userService.saveUser(receiver);

        notificationService.sendNotification(sender, "Transação concluída com sucesso" );
        notificationService.sendNotification(receiver, "Transação concluída com sucesso" );

        return transaction;
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
