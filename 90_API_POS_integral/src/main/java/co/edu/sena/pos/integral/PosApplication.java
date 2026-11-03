package co.edu.sena.pos.integral;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class PosApplication {
    public static void main(String[] args) { SpringApplication.run(PosApplication.class, args); }
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
