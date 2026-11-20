package co.edu.sena.perfumeria.web;
import co.edu.sena.perfumeria.catalogo.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
@SpringBootApplication public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class,args);
    }
    @Bean ServletRegistrationBean<CatalogoServlet> catalogo() {
        var f=new ConnectionFactory();
        return new ServletRegistrationBean<>(new CatalogoServlet(new ArticuloDao(f),new AuthDao(f)),"/catalogo");
    }
}
