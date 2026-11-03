package co.edu.sena.pos.web;

import co.edu.sena.pos.catalogo.ConnectionFactory;
import co.edu.sena.pos.catalogo.ProductoDao;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebApplication {
    public static void main(String[] args) { SpringApplication.run(WebApplication.class, args); }

    @Bean
    ServletRegistrationBean<CatalogoServlet> catalogoServlet() {
        return new ServletRegistrationBean<>(new CatalogoServlet(new ProductoDao(new ConnectionFactory())), "/catalogo");
    }
}
