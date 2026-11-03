package co.edu.sena.pos.spring;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<ProductoEntity,ProductoKey> {
    List<ProductoEntity> findByTenantIdOrderByCodigoAscProductoIdAsc(String tenantId);
}
