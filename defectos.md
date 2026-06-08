# Registro de Defectos

## Defecto #1 — Respuesta HTTP incorrecta para género inválido

| Campo | Detalle |
|---|---|
| **ID** | DEF-001 |
| **Caso probado** | Registro con género inválido (`"INVALIDO"`) |
| **Resultado esperado** | HTTP 400 Bad Request |
| **Resultado obtenido** | HTTP 500 Internal Server Error |
| **Tipo de prueba** | Sistema (HTTP) |
| **Test que lo detectó** | `shouldReturnErrorWhenJsonIsInvalid()` en `RegistryControllerIT.java` |
| **Causa probable** | El controlador no valida el enum `Gender` antes de procesarlo. Cuando Jackson intenta deserializar un valor inválido, lanza una excepción no controlada que Spring convierte en HTTP 500 en lugar de HTTP 400. |
| **Estado** | Abierto |

### Evidencia

**Request enviado:**
```json
{
  "name": "Pedro",
  "id": 500,
  "age": 25,
  "gender": "INVALIDO",
  "alive": true
}
```

**Response obtenido:**
```json
{
  "timestamp": "2026-06-08T22:00:28.856+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/register"
}
```

### Solución propuesta
Agregar `@Valid` en el `PersonDTO` y un `@ControllerAdvice` que capture
`MethodArgumentNotValidException` y retorne HTTP 400.