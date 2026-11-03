package co.edu.sena.pos.spring;
import java.io.Serializable;
import java.util.Objects;

/** La identidad conserva ambos atributos de la PK física GA6. */
public class ProductoKey implements Serializable {
    public String tenantId;
    public String productoId;
    public ProductoKey() {}
    public ProductoKey(String tenant, String product) { tenantId = tenant; productoId = product; }
    @Override public boolean equals(Object other) {
        return other instanceof ProductoKey k && Objects.equals(tenantId, k.tenantId) && Objects.equals(productoId, k.productoId);
    }
    @Override public int hashCode() { return Objects.hash(tenantId, productoId); }
}
