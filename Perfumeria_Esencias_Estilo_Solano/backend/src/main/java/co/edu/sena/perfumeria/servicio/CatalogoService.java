package co.edu.sena.perfumeria.servicio;
import co.edu.sena.perfumeria.nucleo.*;
import co.edu.sena.perfumeria.dominio.*;
import static co.edu.sena.perfumeria.nucleo.Datos.*;
import java.util.*;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class CatalogoService {
    private final Db db;
    private final InventarioService inv;
    public CatalogoService(Db db,InventarioService i) {
        this.db=db;
        inv=i;
    }
    public List<Map<String,Object>> listar(Actor a,String tipo) {
        return switch(tipo) {
            case "clientes"->db.lista("SELECT * FROM cliente ORDER BY nombre LIMIT 500");
            case "proveedores"-> {
                a.exigirAdmin();
                yield db.lista("SELECT * FROM proveedor ORDER BY nombre LIMIT 500");
            }
            case "fragancias"->db.lista("SELECT * FROM fragancia ORDER BY codigo_interno");
            default->throw new ApiError(404,"RUTA_INVALIDA","Recurso no disponible");
        }
        ;
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> crear(Actor a,String tipo,Map<String,Object> m) {
        String id=id();
        if(!tipo.equals("clientes"))a.exigirAdmin();
        switch(tipo) {
            case "clientes"->db.cambiar("INSERT INTO cliente VALUES (?,?,?,true)",id,texto(m,"nombre"),opcional(m,"contacto_cobranza"));
            case "proveedores"->db.cambiar("INSERT INTO proveedor VALUES (?,?,?)",id,texto(m,"nombre"),opcional(m,"contacto"));
            case "fragancias"->db.cambiar("INSERT INTO fragancia VALUES (?,?,?,true)",id,texto(m,"codigo_interno"),texto(m,"nombre_comercial"));
            default->throw new ApiError(404,"RUTA_INVALIDA","Recurso no disponible");
        }
        return Map.of("id",id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> articulo(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String id=id();
        String unit=op(m,"unidad_base","ML","UNIDAD","G"),clase=op(m,"clase","LIQUIDO","ENVASE","ACCESORIO","PERFUME_SELLADO","PERFUME_ABIERTO","MUESTRA"),funcion=op(m,"funcion_material","ESENCIA","ALCOHOL","FIJADOR","FEROMONAS","FRASCO","VALVULA","TAPA","ETIQUETA","ATOMIZADOR","PERFUME","OTRO");
        conflicto(Set.of("ENVASE","ACCESORIO","PERFUME_SELLADO").contains(clase)&&!unit.equals("UNIDAD"),"Envases, accesorios y sellados requieren UNIDAD");
        conflicto(clase.equals("PERFUME_ABIERTO")&&!unit.equals("ML"),"Perfume abierto requiere ML");
        conflicto(Set.of("ESENCIA","ALCOHOL","FIJADOR","FEROMONAS").contains(funcion)&&!unit.equals("ML"),"Insumos líquidos requieren ML");
        conflicto(Set.of("FRASCO","VALVULA","TAPA","ETIQUETA","ATOMIZADOR").contains(funcion)&&!unit.equals("UNIDAD"),"Materiales físicos requieren UNIDAD");
        conflicto(funcion.equals("PERFUME")&&!Set.of("PERFUME_SELLADO","PERFUME_ABIERTO").contains(clase),"Función PERFUME requiere clase de perfume");
        BigDecimal nominal=m.get("contenido_nominal_ml")==null?null:cant(m,"contenido_nominal_ml");
        conflicto(Set.of("FRASCO","ATOMIZADOR").contains(funcion)&&nominal==null,"Frasco y atomizador requieren capacidad nominal ML");
        BigDecimal min=m.get("minimo_base")==null?BigDecimal.ZERO:V.decimal(m.get("minimo_base"),"minimo_base",6,BigDecimal.ZERO,false);
        db.cambiar("INSERT INTO articulo(articulo_id,codigo,nombre,unidad_base,clase,contenido_nominal_ml,minimo_base,activo,funcion_material) VALUES (?,?,?,?,?,?,?,true,?)",id,texto(m,"codigo"),texto(m,"nombre"),unit,clase,nominal,min,funcion);
        return Map.of("id",id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> minimo(Actor a,String id,Map<String,Object> m) {
        a.exigirAdmin();
        inv.articulo(id);
        BigDecimal min=V.decimal(m.get("minimo_base"),"minimo_base",6,BigDecimal.ZERO,false);
        db.cambiar("UPDATE articulo SET minimo_base=? WHERE articulo_id=?",min,id);
        return Map.of("id",id);
    }
    public Formula formula(String id) {
        var f=db.requerido("SELECT * FROM formula_version WHERE formula_id=? AND estado='APROBADA'",id);
        var comps=db.lista("SELECT * FROM formula_componente WHERE formula_id=? ORDER BY linea_no",id).stream().map(c->new Componente(inv.articulo(c.get("articulo_id").toString()),n(c.get("cantidad_ml_por_preparacion")))).toList();
        try {
            return new Formula(id,n(f.get("volumen_preparado_ml")),comps);
        }
        catch(IllegalArgumentException e) {
            throw V.error("formula",e.getMessage());
        }
    }
    public Especificacion especificacion(String id) {
        var e=db.requerido("SELECT e.*,o.modalidad FROM especificacion_suministro e JOIN oferta o ON o.oferta_id=e.oferta_id WHERE e.especificacion_id=? AND e.estado='APROBADA' AND o.activo=true",id);
        var comps=db.lista("SELECT * FROM especificacion_componente WHERE especificacion_id=? ORDER BY linea_no",id).stream().map(c->new Componente(inv.articulo(c.get("articulo_id").toString()),n(c.get("cantidad_base")))).toList();
        try {
            return new Especificacion(id,e.get("modalidad").toString(),e.get("formula_id")==null?null:formula(e.get("formula_id").toString()),comps);
        }
        catch(IllegalArgumentException x) {
            throw V.error("suministro",x.getMessage());
        }
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> crearFormula(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String frag=ref(m,"fragancia_id"),variante=op(m,"variante","NORMAL","EXTRA_FIJADOR","EXTRA_FEROMONAS");
        db.requerido("SELECT fragancia_id FROM fragancia WHERE fragancia_id=? AND activo=true FOR UPDATE",frag);
        BigDecimal vol=cant(m,"volumen_preparado_ml");
        var comps=filas(m,"componentes");
        List<Componente> cc=comps.stream().map(c->new Componente(inv.articulo(ref(c,"articulo_id")),cant(c,"cantidad_ml_por_preparacion"))).toList();
        conflicto(cc.stream().anyMatch(c->!Set.of("ESENCIA","ALCOHOL","FIJADOR","FEROMONAS").contains(c.articulo().funcionMaterial())),"La fórmula sólo admite insumos líquidos tipados");
        try {
            new Formula("validacion",vol,cc);
        }
        catch(IllegalArgumentException e) {
            throw V.error("componentes",e.getMessage());
        }
        String id=id();
        int v=n(db.requerido("SELECT COALESCE(MAX(version),0)+1 v FROM formula_version WHERE fragancia_id=? AND variante=? AND volumen_preparado_ml=?",frag,variante,vol).get("v")).intValueExact();
        db.cambiar("INSERT INTO formula_version VALUES (?,?,?,?,?,'APROBADA',?,UTC_TIMESTAMP(6))",id,frag,variante,v,vol,a.usuarioId());
        int i=0;
        for(var c:cc)db.cambiar("INSERT INTO formula_componente VALUES (?,?,?,?)",id,++i,c.articulo().id(),c.cantidad());
        return Map.of("id",id);
    }
    public List<Map<String,Object>> formulas(Actor a) {
        var rows=db.lista("SELECT f.*,g.nombre_comercial FROM formula_version f JOIN fragancia g ON g.fragancia_id=f.fragancia_id ORDER BY f.volumen_preparado_ml,f.variante,f.version");
        for(var r:rows)r.put("componentes",db.lista("SELECT c.*,a.codigo,a.nombre FROM formula_componente c JOIN articulo a ON a.articulo_id=c.articulo_id WHERE c.formula_id=? ORDER BY linea_no",r.get("formula_id")));
        return rows;
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> oferta(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String id=id();
        String modalidad=op(m,"modalidad","TERMINADO","PREPARACION","RECARGA","DECANT","ADICIONAL");
        db.cambiar("INSERT INTO oferta VALUES (?,?,?,?,?,?,true)",id,texto(m,"codigo_venta"),texto(m,"nombre_venta"),modalidad,refOpcional(m,"fragancia_id"),m.get("volumen_ml")==null?null:cant(m,"volumen_ml"));
        suministro(a,id,m);
        if(m.get("precio_cop")!=null)precio(a,id,Map.of("importe_unitario_cop",m.get("precio_cop")));
        return Map.of("id",id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> suministro(Actor a,String oferta,Map<String,Object> m) {
        a.exigirAdmin();
        var o=db.requerido("SELECT * FROM oferta WHERE oferta_id=? FOR UPDATE",oferta);
        String formula=refOpcional(m,"formula_id"),modal=o.get("modalidad").toString();
        List<Map<String,Object>> comps=m.get("componentes")instanceof List<?> l&&!l.isEmpty()?filas(m,"componentes"):List.of();
        List<Componente> cc=comps.stream().map(c->new Componente(inv.articulo(ref(c,"articulo_id")),cant(c,"cantidad_base"))).toList();
        conflicto(formula==null&&cc.isEmpty(),"La oferta necesita componentes físicos");
        if(Set.of("PREPARACION","RECARGA").contains(modal)) {
            conflicto(formula==null,"Preparación y recarga requieren fórmula");
            var f=db.requerido("SELECT * FROM formula_version WHERE formula_id=? AND estado='APROBADA'",formula);
            conflicto(!Objects.equals(f.get("fragancia_id"),o.get("fragancia_id"))||n(f.get("volumen_preparado_ml")).compareTo(n(o.get("volumen_ml")))!=0,"Fórmula y oferta deben compartir fragancia y volumen");
        }
        else conflicto(formula!=null,"Terminado y decant usan componentes directos, no mezcla");
        validarMateriales(modal,n(o.get("volumen_ml")),cc);
        Formula f=formula==null?null:formula(formula);
        int v=n(db.requerido("SELECT COALESCE(MAX(version),0)+1 v FROM especificacion_suministro WHERE oferta_id=?",oferta).get("v")).intValueExact();
        String eid=id();
        try {
            new Especificacion(eid,modal,f,cc);
        }
        catch(IllegalArgumentException e) {
            throw V.error("componentes",e.getMessage());
        }
        db.cambiar("INSERT INTO especificacion_suministro VALUES (?,?,?,?,'APROBADA')",eid,oferta,v,formula);
        int i=0;
        for(var c:cc)db.cambiar("INSERT INTO especificacion_componente VALUES (?,?,?,?)",eid,++i,c.articulo().id(),c.cantidad());
        return Map.of("id",eid);
    }
    private void validarMateriales(String modalidad,BigDecimal volumen,List<Componente> cc) {
        conflicto(cc.stream().map(c->c.articulo().id()).distinct().count()!=cc.size(),"Un componente por artículo");
        if(Set.of("PREPARACION","RECARGA").contains(modalidad))conflicto(cc.stream().anyMatch(c->c.articulo().unidad()!=Unidad.UNIDAD),"No se admiten líquidos adicionales fuera de la fórmula");
        if(modalidad.equals("PREPARACION")) {
            conflicto(cc.size()!=4,"Preparación requiere frasco, válvula, tapa y etiqueta");
            for(String f:List.of("FRASCO","VALVULA","TAPA","ETIQUETA"))material(cc,f,volumen);
        }
        else if(modalidad.equals("RECARGA")) {
            conflicto(cc.size()>1||cc.stream().anyMatch(c->!c.articulo().funcionMaterial().equals("ETIQUETA")||c.cantidad().compareTo(BigDecimal.ONE)!=0),"Recarga sólo permite una etiqueta opcional");
        }
        else if(modalidad.equals("DECANT")) {
            conflicto(!Set.of(new BigDecimal("5"),new BigDecimal("10")).stream().anyMatch(x->x.compareTo(volumen)==0),"Decant admite 5 o 10 ML");
            conflicto(cc.size()!=3,"Decant requiere perfume abierto, atomizador y etiqueta");
            var liquidos=cc.stream().filter(c->c.articulo().unidad()==Unidad.ML).toList();
            conflicto(liquidos.size()!=1||!liquidos.getFirst().articulo().clase().equals("PERFUME_ABIERTO")||!liquidos.getFirst().articulo().funcionMaterial().equals("PERFUME")||liquidos.getFirst().cantidad().compareTo(volumen)!=0,"Decant requiere volumen exacto de un original abierto");
            material(cc,"ATOMIZADOR",volumen);
            material(cc,"ETIQUETA",volumen);
        }
    }
    private void material(List<Componente> cc,String funcion,BigDecimal volumen) {
        var matches=cc.stream().filter(c->c.articulo().funcionMaterial().equals(funcion)).toList();
        conflicto(matches.size()!=1||matches.getFirst().cantidad().compareTo(BigDecimal.ONE)!=0||matches.getFirst().articulo().unidad()!=Unidad.UNIDAD,"Se requiere exactamente una unidad de "+funcion);
        if(Set.of("FRASCO","ATOMIZADOR").contains(funcion)) {
            BigDecimal nominal=matches.getFirst().articulo().contenidoNominalMl();
            conflicto(nominal==null||nominal.compareTo(volumen)!=0,"Capacidad de "+funcion+" debe coincidir con la presentación");
        }
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> precio(Actor a,String oferta,Map<String,Object> m) {
        a.exigirAdmin();
        db.requerido("SELECT oferta_id FROM oferta WHERE oferta_id=? FOR UPDATE",oferta);
        BigDecimal p=dinero(m,"importe_unitario_cop",false);
        db.cambiar("UPDATE precio_oferta SET vigente_hasta=UTC_TIMESTAMP(6) WHERE oferta_id=? AND vigente_hasta IS NULL",oferta);
        String id=id();
        db.cambiar("INSERT INTO precio_oferta VALUES (?,?,UTC_TIMESTAMP(6),NULL,?)",id,oferta,p);
        return Map.of("id",id);
    }
    public List<Map<String,Object>> ofertas(Actor a) {
        var rows=db.lista("SELECT o.*,e.especificacion_id,e.formula_id,e.version,p.importe_unitario_cop precio_cop FROM oferta o JOIN especificacion_suministro e ON e.oferta_id=o.oferta_id AND e.version=(SELECT MAX(ee.version) FROM especificacion_suministro ee WHERE ee.oferta_id=o.oferta_id AND ee.estado='APROBADA') LEFT JOIN precio_oferta p ON p.oferta_id=o.oferta_id AND p.vigente_hasta IS NULL WHERE o.activo=true ORDER BY o.codigo_venta");
        for(var r:rows) {
            var e=especificacion(r.get("especificacion_id").toString());
            r.put("componentes",db.lista("SELECT c.*,a.nombre,a.unidad_base FROM especificacion_componente c JOIN articulo a ON a.articulo_id=c.articulo_id WHERE c.especificacion_id=?",e.id()));
            BigDecimal available=null;
            for(var c:e.consumo(1).entrySet()) {
                BigDecimal max=inv.saldo(c.getKey().id()).saldo().divide(c.getValue(),0,RoundingMode.FLOOR);
                if(available==null||max.compareTo(available)<0)available=max;
            }
            r.put("disponible_unidades",available==null?0:available);
        }
        return rows;
    }
}
