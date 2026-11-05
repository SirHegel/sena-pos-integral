package co.edu.sena.pos.integral;

import co.edu.sena.pos.integral.nucleo.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;

/** Dobles en memoria: ninguna conexión JDBC, servidor o dato de una persona. */
public abstract class PruebaBase {
    public static final String T=id(1), U=id(2), OTRO=id(3), P=id(4), B=id(5), S=id(6), VENTA=id(7);
    public static final Actor ADMIN=new Actor(T,U,Set.of("ADMINISTRADOR"));
    public static final Actor CAJERO=new Actor(T,U,Set.of("CAJERO"));
    public static String id(int n) { return "92000000-0000-4000-8000-"+String.format("%012d",n); }
    public static BigDecimal bd(String s) { return new BigDecimal(s); }
    public static Map<String,Object> fila(Object... campos) {
        var m=new LinkedHashMap<String,Object>();
        for(int i=0;i<campos.length;i+=2)m.put((String)campos[i],campos[i+1]);
        return m;
    }
    public static List<Map<String,Object>> filas(Map<String,Object>... rows) { return List.of(rows); }
    public static ApiError error(int status,Runnable accion) {
        ApiError e=assertThrows(ApiError.class,accion::run);assertEquals(status,e.estado());return e;
    }
    public record Llamada(String sql,List<Object> args) {
        public Object arg(int n) { return args.get(n); }
        public boolean contiene(String s) { return sql.contains(s); }
    }
    public static final class DbDoble extends Db {
        public final List<Llamada> lecturas=new ArrayList<>(), escrituras=new ArrayList<>(), confirmadas=new ArrayList<>(), pendientes=new ArrayList<>();
        public Function<Llamada,List<Map<String,Object>>> responder=c->List.of();
        public Consumer<Llamada> antesDeEscribir=c->{};
        boolean transaccion;
        public DbDoble() { super(null); }
        private Llamada llamada(String sql,Object[] args) { return new Llamada(sql,Collections.unmodifiableList(Arrays.asList(args.clone()))); }
        @Override public List<Map<String,Object>> lista(String sql,Object... args) {
            var c=llamada(sql,args);lecturas.add(c);return responder.apply(c);
        }
        @Override public int cambiar(String sql,Object... args) {
            var c=llamada(sql,args);antesDeEscribir.accept(c);escrituras.add(c);
            (transaccion?pendientes:confirmadas).add(c);return 1;
        }
        public long escritas(String fragmento) { return escrituras.stream().filter(c->c.contiene(fragmento)).count(); }
        public void sinEscrituras() { assertTrue(escrituras.isEmpty(),()->"Escrituras inesperadas: "+escrituras); }
        public void sinAcceso() { assertTrue(lecturas.isEmpty());sinEscrituras(); }
    }
    public static final class CodificadorDoble implements PasswordEncoder {
        public int comparaciones;
        @Override public String encode(CharSequence s) { return "codificado:"+s; }
        @Override public boolean matches(CharSequence s,String encoded) { comparaciones++;return encoded.equals(encode(s)); }
    }
    /** El interceptor transaccional es real; el recurso que confirma/revierte es sólo el doble. */
    public static final class TxDoble extends AbstractPlatformTransactionManager {
        private final DbDoble db;
        public int inicios, commits, rollbacks;
        public TxDoble(DbDoble db) { this.db=db; }
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx,TransactionDefinition def) { inicios++;db.transaccion=true;db.pendientes.clear(); }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++;db.confirmadas.addAll(db.pendientes);db.pendientes.clear(); }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++;db.pendientes.clear(); }
        @Override protected void doCleanupAfterCompletion(Object tx) { db.transaccion=false; }
        public <X> X proxy(X objeto) {
            var f=new ProxyFactory(objeto);f.setProxyTargetClass(true);
            f.addAdvice(new TransactionInterceptor(this,new AnnotationTransactionAttributeSource()));
            return (X) f.getProxy();
        }
    }
}
