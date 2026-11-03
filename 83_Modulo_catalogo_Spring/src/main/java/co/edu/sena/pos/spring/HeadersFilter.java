package co.edu.sena.pos.spring;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HeadersFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("Cache-Control","no-store");
        res.setHeader("Content-Security-Policy","default-src 'self'; style-src 'self'; form-action 'self'; frame-ancestors 'none'");
        chain.doFilter(req,res);
    }
}
