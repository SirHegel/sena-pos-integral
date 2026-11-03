package co.edu.sena.pos.spring;
import co.edu.sena.pos.catalogo.Producto;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class CatalogoService {
    private final ProductoRepository repository;
    public CatalogoService(ProductoRepository repository) { this.repository=repository; }

    public List<Producto> list(UUID tenant, String query) {
        String q=query.toLowerCase(Locale.ROOT);
        return repository.findByTenantIdOrderByCodigoAscProductoIdAsc(tenant.toString()).stream()
            .map(ProductoEntity::domain).filter(p->p.nombre().toLowerCase(Locale.ROOT).contains(q)||p.codigo().toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    public Producto find(UUID tenant,UUID id) {
        return repository.findById(new ProductoKey(tenant.toString(),id.toString())).map(ProductoEntity::domain)
            .orElseThrow(()->new CatalogoError(404,"Producto no encontrado en el negocio"));
    }
    @Transactional
    public void create(Producto p) {
        validateBusiness(p);
        repository.saveAndFlush(ProductoEntity.from(p));
    }
    @Transactional
    public void update(Producto p) {
        Producto existing=find(p.tenantId(),p.productoId());
        validateBusiness(p);
        // Mantener el estado existente: editar el catálogo no reactiva un registro inadvertidamente.
        Producto copy=new Producto(p.tenantId(),p.productoId(),p.codigo(),p.nombre(),p.precio(),p.iva(),p.tipo(),p.unidad(),p.modoDescuento(),existing.activo());
        repository.saveAndFlush(ProductoEntity.from(copy));
    }
    @Transactional
    public void delete(UUID tenant,UUID id) {
        find(tenant,id);
        repository.deleteById(new ProductoKey(tenant.toString(),id.toString()));
        repository.flush();
    }
    private void validateBusiness(Producto p) {
        // El prototipoGA5 exige precio positivo; es más restrictivo que el CHECK físico no negativo.
        if(p.precio().signum()<=0)throw new CatalogoError(400,"El precio debe ser mayor que cero");
    }
}
