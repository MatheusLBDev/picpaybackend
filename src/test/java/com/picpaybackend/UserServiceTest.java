package com.picpaybackend;

import com.picpaybackend.domain.user.User;
import com.picpaybackend.domain.user.UserType;
import com.picpaybackend.dtos.UserDTO;
import com.picpaybackend.repositories.UserRepository;
import com.picpaybackend.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    // Criando um logger para registrar mensagens
    private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

    @Mock
    private UserRepository repository; // Mock do repositório de usuários

    @InjectMocks
    private UserService userService; // Serviço de usuário que será testado

    private UserDTO userDTO; // Objeto DTO do usuário
    private User user; // Objeto usuário que será utilizado nos testes

    private AutoCloseable closeable; // AutoCloseable para limpar mocks

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this); // Inicializa os mocks

        // Criando um UserDTO com dados fictícios para o teste
        userDTO = new UserDTO(
            "Matheus",
            "Lima",
            "1234567890",
            new BigDecimal(1000),
            "matheus@hotmail.com",
            "123",
            UserType.COMMON
        );

        // Criando um objeto User a partir do UserDTO
        user = new User(userDTO);
        UUID generatedId = UUID.fromString("0bfd4db2-787b-4c9a-ad61-5a12c23c8096");
        user.setId(generatedId);

        logger.info("Configuração de teste concluída para o usuário: {}", user.getId()); // Log de configuração
    }

    @Test
    void testFindUserById() {
        logger.info("Executando o teste testFindUserById...");

        // Configura o mock para simular o retorno do repositório
        when(repository.findUserById(user.getId())).thenReturn(Optional.of(user));

        // Chama o metodo do serviço para buscar o usuário
        User foundUser = userService.findUserById(user.getId());

        // Validações
        assertNotNull(foundUser);
        assertEquals(user.getFirstName(), foundUser.getFirstName());
        assertEquals(user.getEmail(), foundUser.getEmail());

        // Verifica se o repositório foi chamado corretamente
        verify(repository, times(1)).findUserById(user.getId());

        logger.info("Teste testFindUserById concluído com sucesso.");
    }

    @Test
    void testFindUserById_UserNotFound() {
        logger.info("Executando o teste testFindUserById_UserNotFound...");

        // Configura o mock para retornar uma resposta vazia
        when(repository.findUserById(user.getId())).thenReturn(Optional.empty());

        // Espera-se que uma exceção seja lançada
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            userService.findUserById(user.getId());
        });

        System.out.println(exception.getMessage());

        // Validações da exceção
        assertEquals("Usuário não encontrado com o ID: " + user.getId(), exception.getMessage());

        // Verifica se o repositório foi chamado corretamente
        verify(repository, times(1)).findUserById(user.getId());

        // Log de erro
        logger.error("Usuário não encontrado: {}", user.getId());
    }

    @Test
    void testCreateUser() {
        logger.info("Executando o teste testCreateUser...");

        // Configura o mock para simular a criação do usuário
        when(repository.save(any(User.class))).thenReturn(user);

        // Chama o metodo do serviço para criar o usuário
        User createdUser = userService.createUser(userDTO);

        // Validações
        assertNotNull(createdUser);
        assertEquals(user.getFirstName(), createdUser.getFirstName());
        assertEquals(user.getEmail(), createdUser.getEmail());

        // Verifica se o repositório foi chamado corretamente
        verify(repository, times(1)).save(any(User.class));

        logger.info("Teste testCreateUser concluído com sucesso.");
    }

    @Test
    void testGetAllUsers() {
        logger.info("Executando o teste testGetAllUsers...");

        // Configura o mock para simular a recuperação de todos os usuários
        when(repository.findAll()).thenReturn(List.of(user));

        // Chama o metodo do serviço para obter todos os usuários
        List<User> users = userService.getAllUsers();

        // Validações
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(user.getEmail(), users.get(0).getEmail());

        // Verifica se o repositório foi chamado corretamente
        verify(repository, times(1)).findAll();

        logger.info("Teste testGetAllUsers concluído com sucesso.");
    }

    @Test
    void testSaveUser() {
        logger.info("Executando o teste testSaveUser...");

        // Configura o mock para simular o salvamento do usuário
        when(repository.save(any(User.class))).thenReturn(user);

        // Garante que nenhuma exceção será lançada
        assertDoesNotThrow(() -> userService.saveUser(user));

        // Verifica se o repositório foi chamado corretamente
        verify(repository, times(1)).save(user);

        logger.info("Teste testSaveUser concluído com sucesso.");
    }

    @Test
    void testSaveUserThrowsException() {
        logger.info("Executando o teste testSaveUserThrowsException...");

        // Configura o mock para lançar uma exceção ao salvar o usuário
        doThrow(new RuntimeException("Erro ao salvar usuário")).when(repository).save(any(User.class));

        // Espera-se que uma exceção seja lançada
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.saveUser(user));

        // Validações da exceção
        assertEquals("Erro ao salvar usuário", exception.getMessage());

        // Verifica se o repositório foi chamado corretamente
        verify(repository, times(1)).save(user);

        // Log de erro
        logger.error("Erro ao salvar o usuário: {}", exception.getMessage());
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close(); // Fecha os mocks após cada teste
        logger.info("Limpeza após o teste concluída."); // Log de conclusão da limpeza
    }
}
