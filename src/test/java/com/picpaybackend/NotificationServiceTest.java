package com.picpaybackend;

import com.picpaybackend.domain.user.User;
import com.picpaybackend.dtos.NotificationDTO;
import com.picpaybackend.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;
    private final static Logger logger = LoggerFactory.getLogger(NotificationServiceTest.class);

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("teste@email.com");
    }

    @Test
    void testSendNotificationSuccess() throws Exception {
        when(restTemplate.postForEntity(any(String.class), any(NotificationDTO.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("Notificação enviada", HttpStatus.OK));

        assertDoesNotThrow(() -> notificationService.sendNotification(user, "Transação concluída com sucesso"));
        logger.info("Transação concluída com sucesso");

        verify(restTemplate, times(1)).postForEntity(any(String.class), any(NotificationDTO.class), eq(String.class));
    }

    @Test
    void testSendNotificationFailure() {
        when(restTemplate.postForEntity(any(String.class), any(NotificationDTO.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("Erro", HttpStatus.SERVICE_UNAVAILABLE));

        Exception exception = assertThrows(Exception.class, () -> notificationService.sendNotification(user, "Serviço de notificação indisponível"));
        assertEquals("Serviço de notificação indisponível", exception.getMessage());

        verify(restTemplate, times(1)).postForEntity(any(String.class), any(NotificationDTO.class), eq(String.class));
    }
}
