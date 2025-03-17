package com.picpaybackend.services;

import com.picpaybackend.domain.transaction.Transaction;
import com.picpaybackend.domain.user.User;
import com.picpaybackend.dtos.TransactionDTO;
import com.picpaybackend.repositories.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionService {
    private final UserService userService;
    private final TransactionRepository repository;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private static final int MAX_RETRIES = 3;
    private static final int BACKOFF_TIME_MS = 2000;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    public TransactionService(UserService userService, TransactionRepository repository, RestTemplate restTemplate, NotificationService notificationService) {
        this.userService = userService;
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.notificationService = notificationService;
    }

    @Transactional
    public Transaction createTransaction(TransactionDTO transactionDTO) throws Exception {
        logger.info("Iniciando criação da transação. Remetente: {}, Destinatário: {}, Valor: {}",
            transactionDTO.senderId(), transactionDTO.receiverId(), transactionDTO.value());

        User sender = this.userService.findUserById(transactionDTO.senderId());
        User receiver = this.userService.findUserById(transactionDTO.receiverId());

        userService.validateTransaction(sender, transactionDTO.value());

        this.authorizeTransaction(sender, transactionDTO.value());

        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDTO.value());
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transactionDTO.value()));
        receiver.setBalance(receiver.getBalance().add(transactionDTO.value()));

        repository.save(transaction);
        logger.info("Transação salva com sucesso. ID da transação: {}, Valor: {}", transaction.getId(), transactionDTO.value());

        try {
            notificationService.sendNotification(sender, "Transação concluída com sucesso");
            notificationService.sendNotification(receiver, "Transação concluída com sucesso");
        } catch (Exception e) {
            logger.warn("Falha ao enviar notificação para usuários da transação ID: {}", transaction.getId());
        }

        return transaction;
    }

    @Transactional
    public void revertTransaction(Transaction transactionId) throws Exception {
        Transaction originalTransaction= findTransactionsById(transactionId.getId());

        if (originalTransaction.isReversed()) {
            throw new Exception("Esta transação já foi revertida.");
        }

        User sender = originalTransaction.getSender();
        User receiver = originalTransaction.getReceiver();
        BigDecimal amount = originalTransaction.getAmount();

        if (sender == null || receiver == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Transação inválida para reversão");
        }

        if (receiver.getBalance().compareTo(amount) < 0) {
            throw new Exception("O destinatário não tem saldo suficiente para devolver o valor");
        }

        receiver.setBalance(receiver.getBalance().subtract(amount));
        sender.setBalance(sender.getBalance().add(amount));

        originalTransaction.setReversed(true);
        repository.save(originalTransaction);

        Transaction reversalTransaction = new Transaction();
        reversalTransaction.setSender(receiver);
        reversalTransaction.setReceiver(sender);
        reversalTransaction.setAmount(amount);
        reversalTransaction.setTimestamp(LocalDateTime.now());
        reversalTransaction.setReversed(true);
        repository.save(reversalTransaction);

        logger.info("Transação de reversão criada. ID da transação original: {}, ID da reversão: {}",
            originalTransaction.getId(), reversalTransaction.getId());

        try {
            notificationService.sendNotification(sender, "Sua transação foi revertida com sucesso.");
            notificationService.sendNotification(receiver, "A transação foi revertida, o valor foi devolvido.");
        } catch (Exception e) {
            logger.warn("Falha ao enviar notificação para os usuários.");
        }
    }

    public Transaction findTransactionsById(final UUID id) {
        try {
            return repository.findTransactionsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrado com o ID: " + id));
        } catch (EntityNotFoundException e) {
            logger.error("Transação não encontrado com o ID: {}", id, e);
            throw e;
        }
    }

    public List<Transaction> getAllTransactions() {
        logger.info("Buscando todas transações cadastradas.");
        return repository.findAll();
    }

    public void authorizeTransaction(User sender, BigDecimal value) throws HttpClientErrorException, HttpServerErrorException {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                logger.info("Tentativa {}/{} de autorização da transação para o usuário {}.", attempt + 1, MAX_RETRIES, sender.getId());

                ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);

                if (authorizationResponse.getStatusCode().is5xxServerError()) {
                    logger.error("Erro 5xx ao autorizar transação. Tentativa {}/{}. Usuário: {}", attempt + 1, MAX_RETRIES, sender.getId());
                    throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado ao autorizar transação",
                        "{\"error\": \"Erro inesperado ao autorizar transação\"}".getBytes(), StandardCharsets.UTF_8);
                }

                Map<String, Object> body = authorizationResponse.getBody();

                if (body == null || !body.containsKey("data") || !"success".equals(body.get("status"))) {
                    logger.warn("Transação não autorizada pelo serviço externo. Tentativa {}/{}. Usuário: {}", attempt + 1, MAX_RETRIES, sender.getId());
                    throw new HttpClientErrorException(HttpStatus.FORBIDDEN,
                        "Transação não autorizada",
                        "{\"message\": \"Transação não autorizada\"}".getBytes(),
                        StandardCharsets.UTF_8);
                }

                Map<String, Object> data = (Map<String, Object>) body.get("data");

                if (!data.containsKey("authorization") || !Boolean.TRUE.equals(data.get("authorization"))) {
                    logger.warn("Transação rejeitada pelo serviço externo. Tentativa {}/{}. Usuário: {}", attempt + 1, MAX_RETRIES, sender.getId());
                    throw new HttpClientErrorException(HttpStatus.FORBIDDEN,
                        "Transação não autorizada",
                        "{\"message\": \"Transação não autorizada\"}".getBytes(),
                        StandardCharsets.UTF_8);
                }

                logger.info("Transação autorizada com sucesso para o usuário {}", sender.getId());
                return;

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                attempt++;

                if (attempt >= MAX_RETRIES) {
                    logger.error("Todas as tentativas de autorização falharam para o usuário {}.", sender.getId(), e);
                    throw e;
                }

                logger.warn("Erro ao autorizar transação, tentativa {}/{}. Aguardando {}ms antes de tentar novamente.", attempt, MAX_RETRIES, BACKOFF_TIME_MS);

                try {
                    Thread.sleep(BACKOFF_TIME_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Thread de espera interrompida durante a autorização da transação.", ie);
                }
            }
        }
    }
}
