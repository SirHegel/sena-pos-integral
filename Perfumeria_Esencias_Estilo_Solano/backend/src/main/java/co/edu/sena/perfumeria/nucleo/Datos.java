package co.edu.sena.perfumeria.nucleo;
import java.util.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public final class Datos {
    private Datos() {
    }
    public static String id() {
        return UUID.randomUUID().toString();
    }
    public static String texto(Map<String,Object> m,String k) {
        return V.texto(m.get(k),k,240);
    }
    public static String opcional(Map<String,Object> m,String k) {
        Object v=m.get(k);
        return v==null||v.toString().isBlank()?null:V.texto(v,k,240);
    }
    public static String ref(Map<String,Object> m,String k) {
        return V.uuid(m.get(k),k);
    }
    public static String refOpcional(Map<String,Object> m,String k) {
        return m.get(k)==null||m.get(k).toString().isBlank()?null:ref(m,k);
    }
    public static BigDecimal n(Object x) {
        return x==null?BigDecimal.ZERO:new BigDecimal(x.toString());
    }
    public static BigDecimal cant(Map<String,Object> m,String k) {
        return V.decimal(m.get(k),k,6,BigDecimal.ZERO,true);
    }
    public static BigDecimal dinero(Map<String,Object> m,String k,boolean cero) {
        return V.decimal(m.get(k),k,2,BigDecimal.ZERO,!cero);
    }
    public static BigDecimal dinero(Object x) {
        return n(x).setScale(2,RoundingMode.HALF_UP);
    }
    public static int unidades(Map<String,Object> m,String k) {
        return V.entero(m.get(k),k,1,10000);
    }
    public static boolean bool(Map<String,Object> m,String k) {
        return Boolean.TRUE.equals(m.get(k));
    }
    public static String op(Map<String,Object> m,String k,String... values) {
        return V.opcion(m.get(k),k,Set.of(values));
    }
    @SuppressWarnings("unchecked") public static List<Map<String,Object>> filas(Map<String,Object> m,String k) {
        if(!(m.get(k)instanceof List<?> l)||l.isEmpty()||l.size()>100)throw V.error(k,"lista de1a100renglones");
        for(Object x:l)if(!(x instanceof Map<?,?>))throw V.error(k,"renglón inválido");
        return (List<Map<String,Object>>)l;
    }
    public static String hash(Object x) {
        try {
            var out=new java.io.ByteArrayOutputStream();
            try(var data=new java.io.DataOutputStream(out)) {
                canon(data,x);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(out.toByteArray()));
        }
        catch(java.io.IOException|java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    private static void cadena(java.io.DataOutputStream out,String s)throws java.io.IOException {
        byte[] data=s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
    }
    private static void canon(java.io.DataOutputStream out,Object x)throws java.io.IOException {
        if(x==null) {
            out.writeByte(0);
            return;
        }
        if(x instanceof Map<?,?> m) {
            out.writeByte(1);
            var sorted=new TreeMap<String,Object>();
            for(var e:m.entrySet()) {
                if(!(e.getKey() instanceof String k))throw new IllegalArgumentException("Clave textual requerida");
                sorted.put(k,e.getValue());
            }
            out.writeInt(sorted.size());
            for(var e:sorted.entrySet()) {
                cadena(out,e.getKey());
                canon(out,e.getValue());
            }
            return;
        }
        if(x instanceof List<?> l) {
            out.writeByte(2);
            out.writeInt(l.size());
            for(Object item:l)canon(out,item);
            return;
        }
        if(x instanceof String s) {
            out.writeByte(3);
            cadena(out,s);
            return;
        }
        if(x instanceof Boolean v) {
            out.writeByte(4);
            out.writeBoolean(v);
            return;
        }
        if(x instanceof Number v) {
            out.writeByte(5);
            cadena(out,new BigDecimal(v.toString()).stripTrailingZeros().toPlainString());
            return;
        }
        throw new IllegalArgumentException("Tipo no admitido para huella: "+x.getClass().getName());
    }
    public static void conflicto(boolean condicion,String mensaje) {
        if(condicion)throw new ApiError(409,"REGLA_NEGOCIO",mensaje);
    }
}
