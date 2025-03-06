package com.picpaybackend.services;

import com.picpaybackend.domain.user.User;
import com.picpaybackend.dtos.NotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    private final RestTemplate restTemplate;

    @Autowired
    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendNotification(User user, String message) throws Exception{
        String email = user.getEmail();
        NotificationDTO notificationRequest = new NotificationDTO(email, message);
        ResponseEntity<String> notificationResponse = restTemplate.postForEntity("https://util.devi.tools/api/v1/notify", notificationRequest, String.class);

        if(notificationResponse.getStatusCode() != HttpStatus.OK){
            System.out.println("Falha no envio de notificação");
            throw new Exception("Serviço de notificação indisponível");
        }
    }

}
