package co.edu.sena.pos.auth;

/** El usuario del caso se representa con el correo normalizado dentro de un negocio. */
public record Credentials(String tenantId, String usuario, String password) {}
