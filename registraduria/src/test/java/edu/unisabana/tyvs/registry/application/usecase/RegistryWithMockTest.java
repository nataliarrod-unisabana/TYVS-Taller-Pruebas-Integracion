package edu.unisabana.tyvs.registry.application.usecase;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.domain.model.Gender;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Clase de prueba unitaria para {@link Registry} utilizando un mock de {@link RegistryRepositoryPort}.
 *
 * <p>Estas pruebas ilustran cómo aislar el caso de uso del repositorio real,
 * aplicando dobles de prueba (Mockito) para simular los escenarios.</p>
 *
 * <p><b>Formato AAA:</b></p>
 * <ul>
 *   <li><b>Arrange</b>: se preparan datos y comportamiento del mock.</li>
 *   <li><b>Act</b>: se ejecuta el método bajo prueba.</li>
 *   <li><b>Assert</b>: se verifican resultados y que no haya interacciones no deseadas.</li>
 * </ul>
 *
 * <p><b>Beneficio:</b> este tipo de prueba es una <i>unitaria pura</i>,
 * sin necesidad de levantar bases de datos ni infraestructura adicional.</p>
 */
public class RegistryWithMockTest {

    /** Mock del puerto de persistencia. */
    private RegistryRepositoryPort repo;

    /** Caso de uso bajo prueba, instanciado con el mock. */
    private Registry registry;

    /**
     * Configura el mock y el caso de uso antes de cada prueba.
     *
     * <p>Se crea un mock de {@link RegistryRepositoryPort} usando Mockito
     * y se inyecta en la instancia de {@link Registry}.</p>
     */
    @Before
    public void setUp() {
        repo = mock(RegistryRepositoryPort.class);
        registry = new Registry(repo);
    }

    /**
     * Caso de prueba: detectar registros duplicados.
     *
     * <p><b>Escenario (BDD):</b></p>
     * <ul>
     *   <li><b>Given</b>: una persona con ID=7 y el repositorio ya indica que ese ID existe.</li>
     *   <li><b>When</b>: se intenta registrar la persona.</li>
     *   <li><b>Then</b>: el resultado debe ser {@link RegisterResult#DUPLICATED}
     *       y no se debe invocar el método {@code save(...)} en el repositorio.</li>
     * </ul>
     *
     * @throws Exception propagada en caso de error durante la ejecución.
     */
    @Test
    public void shouldReturnDuplicatedWhenRepoSaysExists() throws Exception {
        // Arrange: configurar mock y datos
        when(repo.existsById(7)).thenReturn(true);
        Person p = new Person("Ana", 7, 25, Gender.FEMALE, true);

        // Act: ejecutar método bajo prueba
        RegisterResult result = registry.registerVoter(p);

        // Assert: verificar resultado y comportamiento esperado del mock
        assertEquals(RegisterResult.DUPLICATED, result);
        verify(repo, never()).save(anyInt(), anyString(), anyInt(), anyBoolean());
    }

    /**
     * Caso de prueba: registro válido invoca save().
     *
     * <p><b>Escenario (BDD):</b></p>
     * <ul>
     *   <li><b>Given</b>: una persona válida con ID=10 que no existe en el repositorio.</li>
     *   <li><b>When</b>: se intenta registrar la persona.</li>
     *   <li><b>Then</b>: el resultado debe ser {@link RegisterResult#VALID}
     *       y se debe invocar el método {@code save(...)} exactamente una vez.</li>
     * </ul>
     */
    @Test
    public void shouldCallSaveWhenPersonIsValid() throws Exception {
        // Arrange
        when(repo.existsById(10)).thenReturn(false);
        Person p = new Person("Luis", 10, 25, Gender.MALE, true);

        // Act
        RegisterResult result = registry.registerVoter(p);

        // Assert
        assertEquals(RegisterResult.VALID, result);
        verify(repo, times(1)).save(anyInt(), anyString(), anyInt(), anyBoolean());
    }

    /**
     * Caso de prueba: excepción SQL simulada.
     *
     * <p><b>Escenario (BDD):</b></p>
     * <ul>
     *   <li><b>Given</b>: una persona válida y el repositorio lanza una excepción al guardar.</li>
     *   <li><b>When</b>: se intenta registrar la persona.</li>
     *   <li><b>Then</b>: el caso de uso debe propagar la excepción como {@link IllegalStateException}.</li>
     * </ul>
     */
    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenRepositoryFails() throws Exception {
        // Arrange
        when(repo.existsById(20)).thenReturn(false);
        doThrow(new IllegalStateException("SQL error simulado"))
                .when(repo).save(anyInt(), anyString(), anyInt(), anyBoolean());
        Person p = new Person("Sofia", 20, 30, Gender.FEMALE, true);

        // Act
        registry.registerVoter(p);

        // Assert: manejado por @Test(expected = IllegalStateException.class)
    }

    /**
     * Caso de prueba (reto): FakeRepository con HashMap.
     *
     * <p>En lugar de un mock, se usa una implementación falsa real
     * que almacena datos en memoria con un HashMap.</p>
     */
    @Test
    public void shouldRegisterValidPersonWithFakeRepository() throws Exception {
        // Arrange: FakeRepository implementado con HashMap
        RegistryRepositoryPort fakeRepo = new RegistryRepositoryPort() {
            private final java.util.HashMap<Integer, String> db = new java.util.HashMap<>();

            @Override
            public boolean existsById(int id) {
                return db.containsKey(id);
            }

            @Override
            public void save(int id, String name, int age, boolean alive) {
                db.put(id, name);
            }

            @Override
            public void initSchema() {}

            @Override
            public void deleteAll() {}

            @Override
            public java.util.Optional<edu.unisabana.tyvs.registry.infrastructure.persistence.RegistryRecord> findById(int id) {
                return java.util.Optional.empty();
            }
        };

        Registry fakeRegistry = new Registry(fakeRepo);
        Person p = new Person("Carlos", 50, 28, Gender.MALE, true);

        // Act
        RegisterResult result = fakeRegistry.registerVoter(p);

        // Assert
        assertEquals(RegisterResult.VALID, result);
        assertTrue(fakeRepo.existsById(50));
    }
}
