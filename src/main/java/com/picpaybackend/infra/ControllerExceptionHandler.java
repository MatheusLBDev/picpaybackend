package com.picpaybackend.infra;

import com.picpaybackend.dtos.ExceptionDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;



@RestControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity duplicateEntry(DataIntegrityViolationException exception){
        ExceptionDTO exceptionDTO = new ExceptionDTO("Usuário já cadastrado", "400");
        return ResponseEntity.badRequest().body(exceptionDTO);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionDTO> threat404(EntityNotFoundException exception) {
        ExceptionDTO exceptionDTO = new ExceptionDTO(exception.getMessage(), "404");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionDTO);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ExceptionDTO> httpException(HttpClientErrorException exception) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        String message = "Transação não autorizada";
        String error = String.valueOf(exception.getStatusCode().value());
        ExceptionDTO exceptionDTO = new ExceptionDTO(message, error);
        return ResponseEntity.status(status).body(exceptionDTO);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ExceptionDTO> handleHttpServerError(HttpServerErrorException exception) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        String message = "Erro inesperado ao autorizar transação";
        String error = String.valueOf(exception.getStatusCode().value());
        ExceptionDTO exceptionDTO = new ExceptionDTO(message, error);
        return ResponseEntity.status(status).body(exceptionDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDTO> handleGeneralException(Exception exception) {
        ExceptionDTO exceptionDTO = new ExceptionDTO(exception.getMessage(), "500");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionDTO);
    }
}
