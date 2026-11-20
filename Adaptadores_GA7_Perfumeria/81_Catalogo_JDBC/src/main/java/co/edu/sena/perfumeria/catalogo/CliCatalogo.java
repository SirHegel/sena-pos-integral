package co.edu.sena.perfumeria.catalogo;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;
/** Operador de catálogo. La identidad se autentica; no existe argumento que conceda un rol. */
public class CliCatalogo {
    public static void main(String[] args) {
        try {
            ejecutar(args);
        }
        catch(SecurityException e) {
            System.err.println(e.getMessage());
            System.exit(3);
        }
        catch(IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
        catch(SQLException e) {
            System.err.println(switch(e.getErrorCode()) {
                case 1062->"Código duplicado";case 1451->"Artículo referenciado: no se puede eliminar";default->"No fue posible completar la operación JDBC";
            }
            );
            System.exit(4);
        }
    }
    private static void ejecutar(String[] a)throws SQLException {
        if(a.length==0)throw new IllegalArgumentException("Use listar | crear código nombre unidad clase función nominal-o-- mínimo | editar id nombre mínimo | eliminar id");
        var f=new ConnectionFactory();
        var actor=new AuthDao(f).autenticar(System.getenv("AUTH_USER"),System.getenv("AUTH_PASSWORD"));
        var dao=new ArticuloDao(f);
        switch(a[0]) {
            case "listar"-> {
                if(a.length!=1)bad();
                System.out.println("ID\tCÓDIGO\tNOMBRE\tUNIDAD\tMÍNIMO");
                for(var x:dao.listar(actor))System.out.println(x.getId()+"\t"+x.getCodigo()+"\t"+x.getNombre()+"\t"+x.getUnidad()+"\t"+x.getMinimo().toPlainString());
            }
            case "crear"-> {
                if(a.length!=8)bad();
                var x=new Articulo(UUID.randomUUID().toString(),a[1],a[2],a[3],a[4],a[5],a[6].equals("-")?null:new BigDecimal(a[6]),new BigDecimal(a[7]),true);
                dao.crear(actor,x);
                System.out.println(x.getId());
            }
            case "editar"-> {
                if(a.length!=4)bad();
                if(!dao.actualizar(actor,a[1],a[2],new BigDecimal(a[3])))throw new IllegalArgumentException("Artículo no encontrado");
                System.out.println("Artículo actualizado");
            }
            case "eliminar"-> {
                if(a.length!=2)bad();
                if(!dao.eliminar(actor,a[1]))throw new IllegalArgumentException("Artículo no encontrado");
                System.out.println("Artículo eliminado");
            }
            default->bad();
        }
    }
    private static void bad() {
        throw new IllegalArgumentException("Argumentos no válidos; consulte README");
    }
}
