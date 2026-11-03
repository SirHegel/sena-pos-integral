package co.edu.sena.pos.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Result register(@RequestBody Credentials request) {
        return service.register(request);
    }

    @PostMapping("/ingreso")
    public AuthService.Result login(@RequestBody Credentials request) {
        return service.login(request);
    }
}
