package co.edu.sena.pos.spring;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.UUID;
import co.edu.sena.pos.catalogo.Producto;

@Entity
@Table(name="producto")
@IdClass(ProductoKey.class)
public class ProductoEntity {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(name="tenant_id",length=36,columnDefinition="char(36)")
    String tenantId;
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(name="producto_id",length=36,columnDefinition="char(36)")
    String productoId;
    @Column(nullable=false,length=64) String codigo;
    @Column(nullable=false,length=128) String nombre;
    @Column(name="precio_vigente",nullable=false,precision=14,scale=2) BigDecimal precio;
    @Column(name="tasa_iva",nullable=false,precision=5,scale=4) BigDecimal iva;
    @Column(nullable=false,length=32) String tipo;
    @Column(nullable=false,length=32) String unidad;
    @Column(name="modo_descuento",nullable=false,length=32) String modoDescuento;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(nullable=false,columnDefinition="tinyint") boolean activo;
    protected ProductoEntity() {}

    public static ProductoEntity from(Producto p) {
        ProductoEntity e = new ProductoEntity();
        e.tenantId=p.tenantId().toString(); e.productoId=p.productoId().toString();
        e.codigo=p.codigo(); e.nombre=p.nombre(); e.precio=p.precio(); e.iva=p.iva();
        e.tipo=p.tipo(); e.unidad=p.unidad(); e.modoDescuento=p.modoDescuento(); e.activo=p.activo();
        return e;
    }
    public Producto domain() {
        return new Producto(UUID.fromString(tenantId),UUID.fromString(productoId),codigo,nombre,precio,iva,tipo,unidad,modoDescuento,activo);
    }
}
