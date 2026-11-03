package co.edu.sena.pos.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");
    private static final Pattern UUID_TEXT = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private final AuthRepository repository;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(AuthRepository repository, PasswordEncoder passwords, Clock clock) {
        this.repository = repository;
        this.passwords = passwords;
        this.clock = clock;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Result register(Credentials request) {
        Validated input = validate(request);
        if (!repository.tenantActive(input.tenant())) {
            throw new ApiError(400, "NEGOCIO_NO_DISPONIBLE", "Negocio no disponible");
        }
        String id = UUID.randomUUID().toString();
        try {
            repository.create(input.tenant(), id, input.email(), passwords.encode(input.password()));
        } catch (DuplicateKeyException ex) {
            throw new ApiError(409, "USUARIO_EXISTENTE", "El usuario ya está registrado en este negocio");
        }
        return new Result("Registro satisfactorio", id);
    }

    // Los rechazos de autenticación deben conservar el contador de fallos al confirmar la transacción.
    @Transactional(noRollbackFor = ApiError.class)
    public Result login(Credentials request) {
        Validated input = validate(request);
        AuthRepository.User user = repository.lockUser(input.tenant(), input.email()).orElse(null);
        if (user == null || !user.active() || !repository.tenantActive(input.tenant())) {
            passwords.matches(input.password(), dummyHash);
            throw denied();
        }
        Instant now = clock.instant();
        if (user.lockedUntil() != null && now.isBefore(user.lockedUntil())) {
            passwords.matches(input.password(), dummyHash);
            throw new ApiError(429, "BLOQUEO_TEMPORAL", "Acceso bloqueado temporalmente por intentos fallidos");
        }
        // Un bloqueo vencido abre una nueva ventana de intentos, sin perpetuar el contador anterior.
        int previous = user.lockedUntil() == null ? user.attempts() : 0;
        if (!passwords.matches(input.password(), user.hash())) {
            int attempts = previous + 1;
            Instant until = attempts >= 3 ? now.plusSeconds(30) : null;
            repository.updateAttempts(input.tenant(), user.id(), attempts, until);
            if (until != null) {
                throw new ApiError(429, "BLOQUEO_TEMPORAL", "Tres intentos fallidos. Acceso bloqueado por 30 segundos");
            }
            throw denied();
        }
        repository.updateAttempts(input.tenant(), user.id(), 0, null);
        return new Result("Autenticación satisfactoria", user.id());
    }

    static Validated validate(Credentials request) {
        if (request == null || request.tenantId() == null || !UUID_TEXT.matcher(request.tenantId()).matches()) {
            throw new ApiError(400, "DATOS_INVALIDOS", "tenantId debe ser un UUID válido");
        }
        String tenant = UUID.fromString(request.tenantId()).toString();
        String email = request.usuario() == null ? "" : request.usuario().strip().toLowerCase(Locale.ROOT);
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : "";
        if (email.length() > 254 || local.length() > 64 || local.startsWith(".")
                || local.endsWith(".") || local.contains("..") || !EMAIL.matcher(email).matches()) {
            throw new ApiError(400, "DATOS_INVALIDOS", "usuario debe ser un correo válido de hasta 254 caracteres");
        }
        String password = request.password();
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.codePoints().anyMatch(Character::isISOControl)) {
            throw new ApiError(400, "DATOS_INVALIDOS", "password debe tener entre 8 y 64 caracteres, máximo 72 bytes UTF-8, sin controles");
        }
        return new Validated(tenant, email, password);
    }

    private ApiError denied() {
        return new ApiError(401, "AUTENTICACION_FALLIDA", "Error en la autenticación");
    }

    record Validated(String tenant, String email, String password) {}
    public record Result(String mensaje, String usuarioId) {}
}
