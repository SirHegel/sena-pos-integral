#!/usr/bin/env python3
"""HTTP real de administración, sólo contra el laboratorio local con datos sintéticos.
Las credenciales proceden de contexto privado; las salidas nunca las incluyen.
"""
import argparse, concurrent.futures, csv, datetime, io, json, os, re, subprocess, threading, uuid
from decimal import Decimal
from pathlib import Path
from zoneinfo import ZoneInfo
import requests

T1='90000000-0000-4000-8000-000000000001'
T2='90000000-0000-4000-8000-000000000002'
D=lambda n:Decimal(str(n))

class Suite:
 def __init__(self,contexto,salida,mysql):
  self.env=json.loads(Path(contexto).read_text());self.base='http://127.0.0.1:'+self.env['POS_HTTP_PORT']+'/api/v1'
  if str(self.env.get('POS_DEMO_ENABLED')).lower()!='true':raise RuntimeError('Se requiere laboratorio con semilla sintética explícita')
  self.db=re.fullmatch(r'jdbc:mysql://127\.0\.0\.1:(33119)/(pos_integral_ga6)\?.+',self.env['POS_DB_URL'])
  if not self.db:raise RuntimeError('La suite sólo admite el esquema aislado en127.0.0.1:33119')
  self.out=Path(salida);self.mysql=mysql;self.run='C'+uuid.uuid4().hex[:10];self.rows=[];self.fixtures={};self.periodo=f'{3000+int(uuid.uuid4().hex[:4],16)%5000:04d}-{1+int(uuid.uuid4().hex[:2],16)%12:02d}'
 def record(self,nombre,ok,detalle=None):
  self.rows.append({'caso':nombre,'aprobado':bool(ok),'detalle':detalle})
  if not ok:raise AssertionError(nombre+': '+str(detalle))
 def login(self,rol='administrador',tenant=T1):
  s=requests.Session();r=s.get(self.base+'/auth/csrf',timeout=20);r.raise_for_status();s.headers['X-CSRF-Token']=r.json()['csrfToken']
  r=s.post(self.base+'/auth/ingreso',json={'tenantId':tenant,'correo':rol+'@demo.example','password':self.env['POS_DEMO_PASSWORD']},timeout=20)
  self.record('Ingreso sintético '+rol+' negocio'+tenant[-1],r.status_code==200,{'http':r.status_code})
  r.raise_for_status();s.headers['X-CSRF-Token']=r.json()['csrfToken'];return s
 def call(self,s,metodo,ruta,cuerpo=None,status=200,headers=None):
  r=s.request(metodo,self.base+ruta,json=cuerpo,headers=headers or {},timeout=35)
  try:data=json.loads(r.text,parse_float=Decimal)
  except ValueError:data={'no_json':r.text[:200]}
  self.record(metodo+' '+ruta+' HTTP'+str(status),r.status_code==status,{'http':r.status_code,'respuesta':data})
  return data
 def empleado(self,s,nombre=None,estado='ACTIVO'):
  cuerpo={'documento':self.run+uuid.uuid4().hex[:8],'nombre':nombre or 'Empleado sintético '+self.run,'estado':estado}
  return self.call(s,'POST','/empleados',cuerpo,201),cuerpo
 def novedad(self,s,emp,importe,tipo='DEVENGO',periodo=None,autorizar=True):
  cuerpo={'empleadoId':emp,'periodo':periodo or self.periodo,'concepto':'Concepto académico '+self.run,'tipo':tipo,'importe':str(importe)}
  n=self.call(s,'POST','/novedades',cuerpo,201)
  if autorizar:self.call(s,'POST','/novedades/'+n['novedad_id']+'/autorizar')
  return n,cuerpo
 def liquidacion(self,s,version,periodo=None):
  return self.call(s,'POST','/liquidaciones',{'periodo':periodo or self.periodo,'version':version},201)
 def aprobar(self,s,liq,ids,status=200):return self.call(s,'POST','/liquidaciones/'+liq+'/aprobar',{'novedadesIds':ids},status)
 def cuenta(self,s,naturaleza,nombre=None):
  return self.call(s,'POST','/cuentas',{'codigo':self.run+uuid.uuid4().hex[:6],'nombre':nombre or 'Cuenta sintética '+self.run,'naturaleza':naturaleza},201)['cuenta_id']
 def evento(self,s,liq):return next(x for x in self.call(s,'GET','/eventos') if x['liquidacion_id']==liq)
 def asignar_fecha_borrador(self,liq,fecha):
  # Sólo un UUID creado por esta ejecución, todavía BORRADOR, en el laboratorio conocido.
  uuid.UUID(liq);assert re.fullmatch(r'2026-11-0[123] \d\d:\d\d:\d\d(?:\.\d{6})?',fecha)
  sql="UPDATE liquidacion_nomina SET creada_en='"+fecha+"' WHERE tenant_id='"+T1+"' AND liquidacion_id='"+liq+"' AND estado='BORRADOR'; SELECT ROW_COUNT();"
  env=dict(os.environ,MYSQL_PWD=self.env['POS_DB_PASSWORD'])
  r=subprocess.run([self.mysql,'--no-defaults','--protocol=TCP','--host=127.0.0.1','--port=33119','--user='+self.env['POS_DB_USER'],'--database=pos_integral_ga6','--batch','--skip-column-names','--execute',sql],env=env,capture_output=True,text=True,timeout=20)
  self.record('Fixture de límite temporal sólo en borrador propio',r.returncode==0 and r.stdout.strip()=='1',{'actualizadas':r.stdout.strip(),'ejecutado':r.returncode==0})
 def reporte(self,s,area,desde=None,hasta=None):
  hoy=datetime.datetime.now(ZoneInfo('America/Bogota')).date().isoformat();desde=desde or hoy;hasta=hasta or hoy
  return self.call(s,'GET',f'/reportes/{area}?desde={desde}&hasta={hasta}')
 def csv_coincide(self,s,reporte):
  ruta=f"/reportes/{reporte['area']}/csv?desde={reporte['desde']}&hasta={reporte['hasta']}"
  r=s.get(self.base+ruta,timeout=30);self.record('CSV responde200 '+reporte['area'],r.status_code==200)
  filas=list(csv.reader(io.StringIO(r.content.decode('utf-8-sig'))));pos=next(i for i,x in enumerate(filas) if x==reporte['columnas']);meta=dict(x for x in filas[1:pos] if len(x)==2)
  self.record('Origen y período en CSV '+reporte['area'],meta['tenantId']==reporte['origen']['tenantId'] and meta['usuarioId']==reporte['origen']['usuarioId'] and meta['desde']==reporte['desde'] and meta['hasta']==reporte['hasta'] and meta['zonaHoraria']=='America/Bogota')
  datos=filas[pos+1:-1];total=filas[-1]
  self.record('Mismo conjunto JSON y CSV '+reporte['area'],len(datos)==len(reporte['filas']) and len(datos)==reporte['totales']['filas'])
  for k,v in reporte['totales'].items():
   if k in reporte['columnas']:
    i=reporte['columnas'].index(k);self.record('Total CSV/JSON '+reporte['area']+'.'+k,D(total[i])==D(v))
  return [dict(zip(reporte['columnas'],x)) for x in datos]
 def execute(self):
  self.record('Sin sesión no consulta empleados',requests.get(self.base+'/empleados',timeout=15).status_code==401)
  a=self.login();n=self.login('nomina');c=self.login('contabilidad');j=self.login('cajero');i=self.login('inventario');otro=self.login(tenant=T2)
  self.call(j,'GET','/empleados',status=403);self.call(c,'GET','/empleados',status=403);self.call(n,'GET','/cuentas',status=403);self.call(i,'GET','/asientos',status=403)
  sin_csrf=a.post(self.base+'/empleados',json={'documento':'X','nombre':'X','estado':'ACTIVO'},headers={'X-CSRF-Token':None},timeout=20);self.record('Escritura sin CSRF se rechaza',sin_csrf.status_code==403)
  emp,datos=self.empleado(n,'=2+3');emp=emp['empleado_id'];self.fixtures['empleado']=emp
  self.call(otro,'GET','/empleados/'+emp,status=404)
  self.call(n,'POST','/empleados',dict(datos,tenantId=T2),400)
  self.call(n,'PUT','/empleados/'+emp,dict(datos,estado='INACTIVO'))
  self.call(n,'POST','/novedades',{'empleadoId':emp,'periodo':self.periodo,'concepto':'Prueba','tipo':'DEVENGO','importe':'10.00'},409)
  self.call(n,'PUT','/empleados/'+emp,datos)
  base={'empleadoId':emp,'periodo':self.periodo,'concepto':'Prueba','tipo':'DEVENGO','importe':'10.00'}
  for importe in ('0.00','-1.00','1.001','1000000000000.00'):
   self.call(n,'POST','/novedades',dict(base,importe=importe),400)
  self.call(n,'POST','/novedades',dict(base,periodo='2098-13'),400)
  ajeno,_=self.empleado(otro);self.call(n,'POST','/novedades',dict(base,empleadoId=ajeno['empleado_id']),404)
  v1,_=self.novedad(n,emp,'1000.10',autorizar=False);v2,_=self.novedad(n,emp,'55.55');v3,_=self.novedad(n,emp,'55.65','DEDUCCION')
  version=int(uuid.uuid4().hex[:4],16)+100000;liq=self.liquidacion(n,version);lid=liq['liquidacion_id'];ids=[v1['novedad_id'],v2['novedad_id'],v3['novedad_id']]
  self.aprobar(n,lid,ids,409)
  self.record('Rechazo de selección conserva borrador vacío',self.call(n,'GET','/liquidaciones/'+lid)['detalles']==[])
  self.call(n,'POST','/novedades/'+v1['novedad_id']+'/autorizar')
  self.call(n,'PUT','/novedades/'+v1['novedad_id'],base,409)
  sim=self.call(n,'GET','/liquidaciones/'+lid+'/simulacion');self.record('Suma didáctica exacta en simulación',D(sim['totales']['devengos'])==D('1055.65') and D(sim['totales']['deducciones'])==D('55.65') and D(sim['totales']['neto'])==D('1000.00'))
  self.aprobar(n,lid,[ids[0],ids[0]],400);ap=self.aprobar(n,lid,ids)
  self.record('Aprobación exacta y consumo explícito',ap['estado']=='APROBADA' and set(ap['novedadesIds'])==set(ids) and D(ap['detalles'][0]['neto'])==D('1000.00'))
  again=self.aprobar(n,lid,list(reversed(ids)));self.record('Aprobación repetida no cambia resultado',again['detalles']==ap['detalles'])
  self.aprobar(n,lid,ids[:1],409);l2=self.liquidacion(n,version+1);self.aprobar(n,l2['liquidacion_id'],ids,409)
  self.record('Novedad consumida no deja detalles en otra versión',self.call(n,'GET','/liquidaciones/'+l2['liquidacion_id'])['detalles']==[])
  self.call(n,'POST','/novedades/'+ids[0]+'/rechazar',status=409)
  self.call(n,'PUT','/empleados/'+emp,dict(datos,nombre='Otra identidad'),409)
  ev=self.evento(c,lid);self.record('Evento tipado de nómina pendiente',ev['tipo']=='NOMINA_APROBADA' and ev['venta_id'] is None and ev['estado']=='PENDIENTE')
  de=self.cuenta(c,'DEBITO');cr=self.cuenta(c,'CREDITO');aux=self.cuenta(c,'CREDITO');foreign=self.cuenta(otro,'CREDITO');self.fixtures.update(liquidacion=lid,evento=ev['evento_id'])
  mapping={'cuentaDebitoId':de,'cuentaCreditoId':cr,'cuentaAuxiliarId':aux};url='/eventos/'+ev['evento_id']+'/procesar'
  self.call(c,'POST',url,dict(mapping,cuentaCreditoId=foreign),404)
  self.record('Fallo de cuenta ajena no procesa evento',self.call(c,'GET','/eventos/'+ev['evento_id'])['estado']=='PENDIENTE')
  self.call(c,'POST',url,dict(mapping,cuentaCreditoId=de),400);self.call(c,'POST',url,{'cuentaDebitoId':de,'cuentaCreditoId':cr},400)
  self.call(c,'POST',url,dict(mapping,debito='1.00'),400)
  asiento=self.call(c,'POST',url,mapping);sid=asiento['asiento_id'];self.fixtures['asiento']=sid
  self.record('Asiento completo y equilibrio exacto',len(asiento['lineas'])==3 and D(asiento['totales']['debito'])==D('1055.65') and D(asiento['totales']['credito'])==D('1055.65') and D(asiento['totales']['diferencia'])==0)
  repetido=self.call(c,'POST','/asientos',dict(mapping,eventoId=ev['evento_id']),201);self.record('Dos rutas conservan asiento único',repetido['asiento_id']==sid)
  self.call(c,'POST',url,dict(mapping,cuentaCreditoId=aux,cuentaAuxiliarId=cr),409)
  self.call(otro,'GET','/asientos/'+sid,status=404);self.call(otro,'POST',url,mapping,404)
  self.call(c,'PUT','/cuentas/'+de,{'codigo':self.run+'edicion','nombre':'Nombre nuevo','naturaleza':'DEBITO'},409)
  self.record('Contabilización cambia sólo estado autorizado',self.call(n,'GET','/liquidaciones/'+lid)['estado']=='CONTABILIZADA')
  self.record('Evento procesado completo',self.call(c,'GET','/eventos/'+ev['evento_id'])['procesada_en'] is not None)
  # Dos aprobaciones compiten por la misma novedad: una gana y la otra no deja parciales.
  nv,_=self.novedad(n,emp,'20.00');q1=self.liquidacion(n,version+2)['liquidacion_id'];q2=self.liquidacion(n,version+3)['liquidacion_id'];n2=self.login('nomina');barrera=threading.Barrier(2)
  def competir(s,q):
   barrera.wait();r=s.post(self.base+'/liquidaciones/'+q+'/aprobar',json={'novedadesIds':[nv['novedad_id']]},timeout=40);return q,r.status_code
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
   f=ex.submit(competir,n,q1);g=ex.submit(competir,n2,q2);res=[f.result(),g.result()]
  self.record('Concurrencia no consume novedad dos veces',sorted(x[1] for x in res)==[200,409],res)
  perdedor=next(q for q,s in res if s==409);self.record('Aprobación perdedora revierte todos los detalles',self.call(n,'GET','/liquidaciones/'+perdedor)['detalles']==[])
  ganador=next(q for q,s in res if s==200);ev2=self.evento(c,ganador);c2=self.login('contabilidad');barrera=threading.Barrier(2);dos={'cuentaDebitoId':de,'cuentaCreditoId':cr}
  def procesar_concurrente(s):
   barrera.wait();r=s.post(self.base+'/eventos/'+ev2['evento_id']+'/procesar',json=dos,timeout=40);return r.status_code,r.json()
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
   f=ex.submit(procesar_concurrente,c);g=ex.submit(procesar_concurrente,c2);res2=[f.result(),g.result()]
  self.record('Evento concurrente registra un solo asiento de dos líneas',all(st==200 for st,_ in res2) and res2[0][1]['asiento_id']==res2[1][1]['asiento_id'] and all(len(r['lineas'])==2 and D(r['totales']['diferencia'])==0 for _,r in res2))
  # Matriz de permisos de reporte: las decisiones son del servidor por área.
  permitidas={'administrador':{'ventas','nomina','contabilidad','inventario','caja'},'nomina':{'nomina'},'contabilidad':{'ventas','contabilidad'},'inventario':{'inventario'},'cajero':{'ventas','caja'}}
  sesiones={'administrador':a,'nomina':n,'contabilidad':c,'inventario':i,'cajero':j};hoy=datetime.datetime.now(ZoneInfo('America/Bogota')).date().isoformat()
  for rol,s in sesiones.items():
   for area in ('ventas','nomina','contabilidad','inventario','caja'):
    self.call(s,'GET',f'/reportes/{area}?desde={hoy}&hasta={hoy}',status=200 if area in permitidas[rol] else 403)
  self.call(n,'GET','/reportes/nomina?desde=2026-12-01&hasta=2026-11-01',status=400)
  self.call(n,'GET','/reportes/nomina?desde=2026-02-30&hasta=2026-03-01',status=400)
  self.call(n,'GET','/reportes/nomina?desde=2020-01-01&hasta=2026-11-03',status=400)
  rep=self.reporte(n,'nomina');self.record('Origen del servidor',rep['origen']['tenantId']==T1 and rep['origen']['roles']==['NOMINA'] and rep['zonaHoraria']=='America/Bogota');csvrows=self.csv_coincide(n,rep)
  self.record('Fórmula ASCII neutralizada sólo en CSV',any(x['empleado_id']==emp and x['nombre']=="'=2+3" for x in csvrows) and any(x['empleado_id']==emp and x['nombre']=='=2+3' for x in rep['filas']))
  otrorep=self.reporte(otro,'nomina');self.record('Reporte no cruza negocios',not any(x['empleado_id']==emp for x in otrorep['filas']))
  self.csv_coincide(c,self.reporte(c,'contabilidad'));self.csv_coincide(i,self.reporte(i,'inventario'))
  # Límite exacto de días: preparación SQL sólo cambia la fecha de borradores propios.
  marcas=['2026-11-02 04:59:59.999999','2026-11-02 05:00:00.000000','2026-11-03 04:59:59.999999','2026-11-03 05:00:00.000000'];limites=[]
  for k,fecha in enumerate(marcas):
   e,_=self.empleado(n,'Empleado límite '+str(k)+' '+self.run);nov,_=self.novedad(n,e['empleado_id'],'1.00');q=self.liquidacion(n,version+10+k)['liquidacion_id'];self.asignar_fecha_borrador(q,fecha);self.aprobar(n,q,[nov['novedad_id']]);limites.append(q)
  dia=self.reporte(n,'nomina','2026-11-02','2026-11-02');presentes={x['liquidacion_id'] for x in dia['filas']};self.record('Día Bogotá incluye inicio/excluye siguiente y conserva microsegundos',limites[1] in presentes and limites[2] in presentes and limites[0] not in presentes and limites[3] not in presentes)
  self.csv_coincide(n,dia)
  # Origen venta pagada real del bloque B para contabilización de tres líneas.
  bod=self.call(a,'POST','/bodegas',{'nombre':'Bodega C '+self.run},201)['bodega_id'];box=self.call(a,'POST','/cajas',{'nombre':'Caja C '+self.run},201)['caja_id'];ses=self.call(a,'POST','/sesiones-caja',{'cajaId':box,'fondoInicial':'0.00'},201)['sesion_id']
  prod=self.call(a,'POST','/productos',{'codigo':self.run,'nombre':'Producto C '+self.run,'precioVigente':'100.00','tasaIva':'0.1900','tipo':'VENDIBLE','unidad':'Unidad','modoDescuento':'DIRECTO','activo':True},201)['producto_id']
  self.call(a,'POST','/movimientos',{'bodegaId':bod,'productoId':prod,'cantidadDelta':'2.000','tipo':'ENTRADA','documentoOrigen':self.run},201)
  venta=self.call(a,'POST','/ventas',{'bodegaId':bod,'sesionCajaId':ses,'lineas':[{'productoId':prod,'cantidad':1}]},201)
  pagada=self.call(a,'POST','/ventas/'+venta['venta_id']+'/confirmar',{'medio':'EFECTIVO','referencia':self.run,'importe':'119.00'},headers={'Idempotency-Key':self.run})
  ve=next(x for x in self.call(c,'GET','/eventos') if x['venta_id']==venta['venta_id']);cont=self.call(c,'POST','/eventos/'+ve['evento_id']+'/procesar',mapping)
  self.record('Asiento de venta coincide con comprobante B',cont['tipo_origen']=='VENTA' and len(cont['lineas'])==3 and D(cont['totales']['debito'])==D(pagada['total'])==D('119.00'))
  self.call(a,'POST','/sesiones-caja/'+ses+'/cerrar',{'totalContado':'119.00'})
  vr=self.reporte(c,'ventas');self.csv_coincide(c,vr);jr=self.reporte(j,'ventas');self.record('Cajero no recibe venta de administrador en reporte',not any(x['venta_id']==venta['venta_id'] for x in jr['filas']))
  ca=self.reporte(a,'caja');self.csv_coincide(a,ca);fila=next(x for x in ca['filas'] if x['sesion_id']==ses);self.record('Reporte conserva esperado/contado de cierre B',D(fila['total_esperado'])==D('119.00') and D(fila['total_contado'])==D('119.00') and D(fila['diferencia'])==0)
  self.fixtures.update(venta=venta['venta_id'],asiento_venta=cont['asiento_id'],limites_dia_bogota=limites)
  # Unicode ancho completo y comillas/separadores siguen siendo texto, nunca una nueva celda.
  nombres=['＝2+3','＋SUM(1,2)','＠SUM(1,2)','Texto "literal", separado'];nids=[];eids=[]
  for nombre in nombres:
   e,_=self.empleado(n,nombre);nov,_=self.novedad(n,e['empleado_id'],'1.00');nids.append(nov['novedad_id']);eids.append(e['empleado_id'])
  uq=self.liquidacion(n,version+40)['liquidacion_id'];self.aprobar(n,uq,nids);ur=self.reporte(n,'nomina');ucsv=self.csv_coincide(n,ur)
  for eid,nombre in zip(eids,nombres):
   salida=next(x['nombre'] for x in ucsv if x['empleado_id']==eid);esperado="'"+nombre if nombre[0] in '＝＋＠' else nombre
   self.record('CSV conserva texto y neutraliza prefijo '+nombre,salida==esperado,{'salida':salida,'esperado':esperado})
 def save(self,error=None):
  self.out.parent.mkdir(parents=True,exist_ok=True);self.out.write_text(json.dumps({'ejecucion':self.run,'entorno':'API local8090/MySQL aislado33119, datos sintéticos','casos':self.rows,'aprobados':sum(x['aprobado'] for x in self.rows),'total':len(self.rows),'error':error,'fixtures':self.fixtures},ensure_ascii=False,indent=2,default=str))

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--contexto',required=True);p.add_argument('--salida',required=True);p.add_argument('--mysql',required=True);args=p.parse_args();s=Suite(args.contexto,args.salida,args.mysql)
 try:s.execute()
 except Exception as e:s.save(type(e).__name__+': '+str(e));raise
 s.save();print('C:',sum(x['aprobado'] for x in s.rows),'/',len(s.rows),'aprobadas')
