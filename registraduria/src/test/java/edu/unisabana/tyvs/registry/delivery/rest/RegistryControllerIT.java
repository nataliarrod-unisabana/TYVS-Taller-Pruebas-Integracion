// src/test/java/edu/unisabana/tyvs/registry/delivery/rest/RegistryControllerIT.java
package edu.unisabana.tyvs.registry.delivery.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;

// src/test/java/.../RegistryControllerIT.java
@RunWith(SpringRunner.class)
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true")
public class RegistryControllerIT {

    @TestConfiguration
    static class TestBeans {
        @Bean
        public RegistryRepositoryPort registryRepositoryPort() throws Exception {
            String jdbc = "jdbc:h2:mem:regdb;DB_CLOSE_DELAY=-1";
            var repo = new edu.unisabana.tyvs.registry.infrastructure.persistence.RegistryRepository(jdbc);
            repo.initSchema();
            return repo;
        }

        @Bean
        public edu.unisabana.tyvs.registry.application.usecase.Registry registry(RegistryRepositoryPort port) {
            return new edu.unisabana.tyvs.registry.application.usecase.Registry(port);
        }
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    public void shouldRegisterValidPerson() {
        String json = "{\"name\":\"Ana\",\"id\":100,\"age\":30,\"gender\":\"FEMALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assert resp.getStatusCode() == HttpStatus.OK;
        assert "VALID".equals(resp.getBody());
    }
    @Test
    public void shouldReturnDuplicatedWhenSameIdRegisteredTwice() {
        // Arrange
        String json1 = "{\"name\":\"Luis\",\"id\":200,\"age\":30,\"gender\":\"MALE\",\"alive\":true}";
        String json2 = "{\"name\":\"LuisDos\",\"id\":200,\"age\":35,\"gender\":\"MALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act: primer registro
        rest.postForEntity("/register", new HttpEntity<>(json1, headers), String.class);

        // Act: segundo registro con mismo ID
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json2, headers), String.class);

        // Assert
        assert resp.getStatusCode() == HttpStatus.OK;
        assert "DUPLICATED".equals(resp.getBody());
    }

    @Test
    public void shouldReturnUnderageWhenPersonIsTooYoung() {
        // Arrange
        String json = "{\"name\":\"Carlos\",\"id\":300,\"age\":15,\"gender\":\"MALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        // Assert
        assert resp.getStatusCode() == HttpStatus.OK;
        assert "UNDERAGE".equals(resp.getBody());
    }

    @Test
    public void shouldReturnDeadWhenPersonIsNotAlive() {
        // Arrange
        String json = "{\"name\":\"Maria\",\"id\":400,\"age\":40,\"gender\":\"FEMALE\",\"alive\":false}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        // Assert
        assert resp.getStatusCode() == HttpStatus.OK;
        assert "DEAD".equals(resp.getBody());
    }

    @Test
    public void shouldReturnErrorWhenJsonIsInvalid() {
        // Arrange: JSON con género inválido
        String json = "{\"name\":\"Pedro\",\"id\":500,\"age\":25,\"gender\":\"INVALIDO\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        // Assert: debe retornar error del servidor
        assert resp.getStatusCode().is5xxServerError();
    }
}
