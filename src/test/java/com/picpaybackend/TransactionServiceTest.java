package com.picpaybackend;

import com.picpaybackend.domain.transaction.Transaction;
import com.picpaybackend.domain.user.User;
import com.picpaybackend.domain.user.UserType;
import com.picpaybackend.dtos.TransactionDTO;
import com.picpaybackend.repositories.TransactionRepository;
import com.picpaybackend.services.NotificationService;
import com.picpaybackend.services.TransactionService;
import com.picpaybackend.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository repository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TransactionService transactionService;

    private User sender;
    private User receiver;
    private TransactionDTO transactionDTO;
    private AutoCloseable closeable;
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceTest.class);

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        sender = new User();
        sender.setId(UUID.randomUUID());
        sender.setBalance(new BigDecimal("1000.00"));

        receiver = new User();
        receiver.setId(UUID.randomUUID());
        receiver.setBalance(new BigDecimal("500.00"));

        transactionDTO = new TransactionDTO(new BigDecimal("200.00"), sender.getId(), receiver.getId());

        logger.info("Configuração de teste concluída para o transactions");
    }

    @Test
    void testCreateTransaction_Success() throws Exception {
        logger.info("Iniciando teste de transação bem-sucedida...");

        when(userService.findUserById(sender.getId())).thenReturn(sender);
        when(userService.findUserById(receiver.getId())).thenReturn(receiver);
        doNothing().when(userService).validateTransaction(sender, transactionDTO.value());

        Map<String, Object> mockResponse = Map.of(
                "status", "success",
                "data", Map.of("authorization", true)
        );
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(mockResponse);

        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenReturn(responseEntity);

        Transaction transaction = transactionService.createTransaction(transactionDTO);
        when(repository.save(any(Transaction.class))).thenReturn(transaction);

        assertNotNull(transaction);
        assertEquals(sender, transaction.getSender());
        assertEquals(receiver, transaction.getReceiver());
        assertEquals(transactionDTO.value(), transaction.getAmount());

        logger.info("Transação concluída com sucesso!");

        verify(userService, times(1)).findUserById(sender.getId());
        verify(userService, times(1)).findUserById(receiver.getId());
        verify(userService, times(1)).validateTransaction(sender, transactionDTO.value());
        verify(restTemplate, atMost(3)).getForEntity(anyString(), eq(Map.class));
        verify(repository, times(1)).save(any(Transaction.class));
        verify(notificationService, times(1)).sendNotification(sender, "Transação concluída com sucesso");
        verify(notificationService, times(1)).sendNotification(receiver, "Transação concluída com sucesso");
    }

    @Test
    void testCreateTransaction_FailureUnauthorized() throws Exception {
        logger.info("Iniciando teste de transação não autorizada...");

        when(userService.findUserById(sender.getId())).thenReturn(sender);
        when(userService.findUserById(receiver.getId())).thenReturn(receiver);
        doNothing().when(userService).validateTransaction(any(User.class), any(BigDecimal.class));

        Map<String, Object> mockResponse = Map.of(
                "status", "failure",
                "data", Map.of("authorization", false)
        );
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(mockResponse);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> transactionService.createTransaction(transactionDTO));

        logger.info("Exceção capturada: " + exception.getStatusCode());

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getResponseBodyAsString().contains("Transação não autorizada"));

        verify(repository, never()).save(any(Transaction.class));
        verify(notificationService, never()).sendNotification(any(User.class), anyString());
    }

    @Test
    void testAuthorizeTransaction_AuthorizationNotPresent() {
        logger.info("Iniciando teste de autorização ausente...");

        Map<String, Object> mockResponse = Map.of(
                "status", "success",
                "data", Map.of("someOtherKey", true)
        );
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(mockResponse);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> transactionService.authorizeTransaction(sender, BigDecimal.TEN));

        logger.info("Mensagem da exceção: " + exception.getResponseBodyAsString());

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getResponseBodyAsString().contains("Transação não autorizada"));
    }

    @Test
    void testValidateTransactionThrowsExceptionForMerchant() throws Exception {
        logger.info("Testando validação de transação para lojista...");

        sender.setUserType(UserType.MERCHANT);
        doThrow(new Exception("Logista não está autorizado a realizar transações"))
                .when(userService).validateTransaction(any(User.class), any(BigDecimal.class));

        Exception exception = assertThrows(Exception.class, () -> userService.validateTransaction(sender, new BigDecimal(100)));

        logger.info("Exceção capturada: " + exception.getMessage());

        assertEquals("Logista não está autorizado a realizar transações", exception.getMessage());
    }

    @Test
    void testValidateTransactionThrowsExceptionForInsufficientBalance() throws Exception {
        logger.info("Testando validação de saldo insuficiente...");

        sender.setBalance(new BigDecimal("30.00"));
        doThrow(new Exception("Saldo insuficiente"))
                .when(userService).validateTransaction(any(User.class), any(BigDecimal.class));

        Exception exception = assertThrows(Exception.class, () -> userService.validateTransaction(sender, new BigDecimal("50.00")));

        logger.info("Exceção capturada: " + exception.getMessage());

        assertEquals("Saldo insuficiente", exception.getMessage());
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        logger.info("Limpeza após o teste concluída.");
    }

}
