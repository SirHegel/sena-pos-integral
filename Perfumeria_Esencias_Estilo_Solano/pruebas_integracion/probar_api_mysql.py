from pathlib import Path
import os,getpass
import json,urllib.request,urllib.error,http.cookiejar,uuid,datetime,traceback,zipfile,io,concurrent.futures
from decimal import Decimal as D
BASE=Path(__file__).resolve().parents[1]
Q=Path(os.environ.get('PS_QA_DIR',str(BASE/'resultados_pruebas'))).resolve();Q.mkdir(parents=True,exist_ok=True)
if os.environ.get('PS_TEST_CONFIRM_DEMO')!='SI':raise SystemExit('Solo DEMO: establezca PS_TEST_CONFIRM_DEMO=SI para autorizar altas y movimientos sintéticos.')
API=os.environ.get('PS_TEST_API','http://127.0.0.1:8095/api/v2').rstrip('/')
if not API.startswith('http://127.0.0.1:'):raise SystemExit('El ensayo se limita a una API de laboratorio en loopback.')
cred={'admin_user':os.environ.get('PS_TEST_ADMIN_USER','admin'),'admin_password':os.environ.get('PS_TEST_ADMIN_PASSWORD')or getpass.getpass('Clave Admin de laboratorio: '),'db_user':os.environ.get('PS_TEST_DB_USER','ps_app'),'db_password':os.environ['PS_TEST_DB_PASSWORD']if 'PS_TEST_DB_PASSWORD'in os.environ else getpass.getpass('Clave MySQL de laboratorio: '),'db':os.environ['PS_TEST_DB_NAME']}
MYSQL=os.environ.get('PS_TEST_MYSQL_BIN','mysql');MYSQL_PORT=os.environ.get('PS_TEST_MYSQL_PORT','3306')

if (Q/'api_mysql_resultados.json').exists():
 import shutil
 archive=Q/'historial_baterias';archive.mkdir(exist_ok=True)
 stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
 shutil.copy2(Q/'api_mysql_resultados.json',archive/(stamp+'_resultados.json'))
 if (Q/'fixtures_DEMO.json').exists():shutil.copy2(Q/'fixtures_DEMO.json',archive/(stamp+'_fixtures.json'))
prefix='PSQA_'+uuid.uuid4().hex[:8];checks=[];fixtures={'naturaleza':'DATOS SINTÉTICOS DE LABORATORIO; no personas, inventario, costos ni transacciones reales del negocio','prefijo':prefix};phase='inicio'
def check(name,condition,observado=None,rf=None):
 checks.append({'nombre':name,'cumple':bool(condition),'observado':observado,'rf':rf or [],'fecha_utc':datetime.datetime.now(datetime.timezone.utc).isoformat()})
 if not condition:raise AssertionError(name+': '+str(observado))
class Client:
 def __init__(self):
  self.cookie=http.cookiejar.CookieJar();self.op=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPCookieProcessor(self.cookie));self.csrf=None
 def req(self,method,path,data=None,status=200,label=None,rf=None,csrf=True,raw=False):
  body=None if data is None else json.dumps(data,default=str).encode();h={'Content-Type':'application/json'}
  if csrf and self.csrf:h['X-CSRF-Token']=self.csrf
  r=urllib.request.Request(API+path,data=body,method=method,headers=h)
  try:
   with self.op.open(r,timeout=12) as res:code=res.status;b=res.read()
  except urllib.error.HTTPError as e:code=e.code;b=e.read()
  check(label or method+' '+path,code==status,{'esperado_http':status,'http':code,'error':b.decode()[:350] if code>=400 else None},rf)
  if raw:return b
  obj=json.loads(b,parse_float=D) if b else {}
  if path.startswith('/auth/') and isinstance(obj,dict) and 'csrf'in obj:self.csrf=obj['csrf']
  return obj
 def get(self,p,**kw):return self.req('GET',p,**kw)
 def post(self,p,b,**kw):return self.req('POST',p,b,**kw)
 def login(self,u,p):self.get('/auth/sesion');return self.post('/auth/login',{'nombre_acceso':u,'clave':p})
admin=Client()
def art(name,unit='ML',clase='LIQUIDO',nom=None):
 funcion='FRASCO' if name.startswith('FRASCO') else 'ATOMIZADOR' if name.startswith('ATOMIZADOR') else 'ETIQUETA' if name.startswith('ETIQUETA') else 'PERFUME' if name in ['SELLADO','ABIERTO','ULTIMA','SIN_COSTO'] else name if name in ['ESENCIA','FIJADOR','ALCOHOL','FEROMONAS','VALVULA','TAPA'] else 'OTRO'
 b={'codigo':prefix+'_'+name,'nombre':'DEMO '+name,'unidad_base':unit,'clase':clase,'minimo_base':'0','funcion_material':funcion}
 if funcion in ['FRASCO','ATOMIZADOR']:nom=name.removeprefix(funcion)
 if nom:b['contenido_nominal_ml']=nom
 return admin.post('/articulos',b,rf=['PS-RF02'])['id']
def stocks():return {x['articulo_id']:D(str(x['saldo_base'])) for x in admin.get('/articulos')}
def offer(name,modal,vol,formula,components,price,frag):
 b={'codigo_venta':prefix+'_'+name,'nombre_venta':'DEMO '+name,'modalidad':modal,'fragancia_id':frag,'componentes':[{'articulo_id':a,'cantidad_base':q}for a,q in components]}
 if vol:b['volumen_ml']=vol
 if formula:b['formula_id']=formula
 if price is not None:b['precio_cop']=price
 id=admin.post('/ofertas',b,rf=['PS-RF02','PS-RF03'])['id']
 row=next(x for x in admin.get('/ofertas')if x['oferta_id']==id)
 return row

def sale(client,offer,units=1,customer=None):
 b={'lineas':[{'especificacion_id':offer['especificacion_id'],'unidades':units}]}
 if customer:b['cliente_id']=customer
 id=client.post('/ventas',b,rf=['PS-RF06'])['id'];return id

def issue(client,id,credit=False):
 b={'clave_operacion':prefix+'EM'+id,'credito':credit}
 if not credit:b['cobro']={'turno_id':turn,'medio':'EFECTIVO','importe_cop':str(client.get('/ventas/'+id)['total_cop']),'confirmado':True}
 if credit:b.update({'fecha_vencimiento':(datetime.date.today()+datetime.timedelta(days=15)).isoformat(),'condicion_pactada':'DEMO15díascalendario desde emisión'})
 return client.post('/ventas/'+id+'/emitir',b,rf=['PS-RF06','PS-RF10'] if credit else['PS-RF06']),b
try:
 phase='autenticacion'
 admin.login(cred['admin_user'],cred['admin_password']);admin.post('/proveedores',{'nombre':'DEMO sinCSRF'},status=403,csrf=False,rf=['PS-RF01'])
 vendor_name=prefix.lower()+'_vendedora';vendor_password='ClaveDEMO_'+uuid.uuid4().hex
 uid=admin.post('/usuarios',{'nombre_visible':'Vendedora DEMO','nombre_acceso':vendor_name,'clave':vendor_password,'perfil':'VENDEDORA'},rf=['PS-RF01'])['id'];vendor=Client();vendor.login(vendor_name,vendor_password)
 vendor.post('/articulos',{'codigo':'DENEGADO'},status=403,rf=['PS-RF01']);vendor.get('/movimientos',status=403,rf=['PS-RF01']);vendor.get('/cartera',status=403,rf=['PS-RF01']);vendor.get('/reportes?tipo=costos',status=403,rf=['PS-RF01'])
 admin.post('/usuarios',{'nombre_visible':'Límite DEMO','nombre_acceso':prefix+'_long','clave':'á'*37,'perfil':'VENDEDORA'},status=400,rf=['PS-RF01']);Client().get('/auth/sesion')
 admin.post('/auth/login',{'nombre_acceso':cred['admin_user'],'clave':'á'*37},status=400,rf=['PS-RF01'])
 phase='catalogo_recepcion'
 supplier=admin.post('/proveedores',{'nombre':prefix+' PROVEEDOR DEMO'},rf=['PS-RF04'])['id'];customer=vendor.post('/clientes',{'nombre':prefix+' CLIENTE DEMO','contacto_cobranza':'DEMO sin dato personal'},rf=['PS-RF10'])['id'];frag=admin.post('/fragancias',{'codigo_interno':prefix+'_AROMA','nombre_comercial':'Fragancia DEMO'},rf=['PS-RF02'])['id']
 arts={k:art(k)for k in['ESENCIA','FIJADOR','ALCOHOL','FEROMONAS']}
 for k in['FRASCO30','FRASCO50','FRASCO100']:arts[k]=art(k,'UNIDAD','ENVASE')
 for k in['VALVULA','TAPA','ETIQUETA','ATOMIZADOR5','ATOMIZADOR10','ETIQUETA_PEQUENA']:arts[k]=art(k,'UNIDAD','ACCESORIO')
 arts['SELLADO']=art('SELLADO','UNIDAD','PERFUME_SELLADO','100');arts['ABIERTO']=art('ABIERTO','ML','PERFUME_ABIERTO')
 # All costs below are chosen laboratory constants, never reported business costs.
 costs={'ESENCIA':'100000','FIJADOR':'20000','ALCOHOL':'10000','FEROMONAS':'50000','FRASCO30':'100000','FRASCO50':'120000','FRASCO100':'150000','VALVULA':'10000','TAPA':'5000','ETIQUETA':'1000','ATOMIZADOR5':'50000','ATOMIZADOR10':'60000','ETIQUETA_PEQUENA':'1000','SELLADO':'400000'}
 rec={'proveedor_id':supplier,'documento_proveedor':prefix+' FACTURA DEMO','clave_operacion':prefix+'_REC','lineas':[{'articulo_id':aid,'cantidad':'1'if k in ['ESENCIA','FIJADOR','ALCOHOL','FEROMONAS']else'2'if k=='SELLADO'else'100','unidad_codigo':'L'if k in ['ESENCIA','FIJADOR','ALCOHOL','FEROMONAS']else'UNIDAD','costo_total_cop':costs[k]}for k,aid in arts.items()if k!='ABIERTO']}
 rid=admin.post('/recepciones',rec,rf=['PS-RF04','PS-RF05'])['id'];before=stocks();check('1L convertido exactamente1000ML',before[arts['ESENCIA']]==1000,str(before[arts['ESENCIA']]),['PS-RF05']);rid2=admin.post('/recepciones',rec,rf=['PS-RF04'])['id'];check('Recepción repetida no duplica stock',rid2==rid and stocks()[arts['ESENCIA']]==1000,rf=['PS-RF04','PS-RF05'])
 bad={**rec,'documento_proveedor':prefix+'_BADG_FACTURA','clave_operacion':prefix+'_BADG','lineas':[{'articulo_id':arts['ESENCIA'],'cantidad':'1','unidad_codigo':'G','costo_total_cop':'10'}]};admin.post('/recepciones',bad,status=400,rf=['PS-RF05']);check('Gramos no crearonML',stocks()[arts['ESENCIA']]==1000,rf=['PS-RF05'])
 bad2={**rec,'lineas':[{'articulo_id':arts['ESENCIA'],'cantidad':'1','unidad_codigo':'L','costo_total_cop':'1'}]};admin.post('/recepciones',bad2,status=409,rf=['PS-RF04'])
 admin.post('/aperturas',{'articulo_sellado_id':arts['SELLADO'],'articulo_abierto_id':arts['ABIERTO'],'unidades':1,'volumen_util_ml':'100','motivo':'Apertura DE LABORATORIO','clave_operacion':prefix+'_AP'},rf=['PS-RF03','PS-RF05']);after=stocks();check('Apertura consume1unidad y produce100MLconorigen',after[arts['SELLADO']]==1 and after[arts['ABIERTO']]==100,rf=['PS-RF05'])
 admin.req('PUT','/articulos/'+arts['ESENCIA']+'/minimo',{'minimo_base':'1000'},rf=['PS-RF13']);rows=admin.get('/articulos');check('Alerta y mínimo no alteran saldo',next(x for x in rows if x['articulo_id']==arts['ESENCIA'])['alerta'] and stocks()[arts['ESENCIA']]==1000,rf=['PS-RF13'])
 vendor_arts=vendor.get('/articulos');check('Costo no visible aVendedora',all('costo_promedio_cop'not in x for x in vendor_arts),rf=['PS-RF01','PS-RF17'])
 phase='formulas_20variantes'
 fdata=json.loads((Path(__file__).with_name('formulas_escenarios.json')).read_text());offers=[]
 for rule in fdata['reglas_liquidas_y_empaque']:
  vals=rule['liquidos_ml'];components=[{'articulo_id':arts[k.upper()],'cantidad_ml_por_preparacion':q}for k,q in vals.items()if D(q)>0]
  variante='NORMAL'if rule['tipo']=='preparacion_normal'else'EXTRA_FEROMONAS'if rule['tipo'].endswith('con_feromonas')else'EXTRA_FIJADOR'
  fid=admin.post('/formulas',{'fragancia_id':frag,'variante':variante,'volumen_preparado_ml':rule['presentacion_ml'],'componentes':components},rf=['PS-RF03'])['id']
  base=D({'30':'25000','50':'38000','100':'65000'}[rule['presentacion_ml']])+(D('5000')if variante!='NORMAL'else D('0'))
  for modalidad in ['PREPARACION','RECARGA']:
   pack=[(arts['ETIQUETA'],'1')]
   if modalidad=='PREPARACION':pack+=[(arts['FRASCO'+rule['presentacion_ml']],'1'),(arts['VALVULA'],'1'),(arts['TAPA'],'1')]
   off=offer(rule['id']+'_'+modalidad,modalidad,rule['presentacion_ml'],fid,pack,str(base-(D('3000')if modalidad=='RECARGA'else D('0'))),frag)
   offers.append({'offer':off,'rule':rule,'pack':pack,'price':base-(D('3000')if modalidad=='RECARGA'else D('0')),'modalidad':modalidad})
 d5=offer('DECANT5','DECANT','5',None,[(arts['ABIERTO'],'5'),(arts['ATOMIZADOR5'],'1'),(arts['ETIQUETA_PEQUENA'],'1')],'10000',frag);d10=offer('DECANT10','DECANT','10',None,[(arts['ABIERTO'],'10'),(arts['ATOMIZADOR10'],'1'),(arts['ETIQUETA_PEQUENA'],'1')],'18000',frag)
 # Refill cannot be given a bottle even by manipulating API.
 badrefill={'codigo_venta':prefix+'_BADREFILL','nombre_venta':'DEMO inválida','modalidad':'RECARGA','fragancia_id':frag,'volumen_ml':'30','formula_id':offers[0]['offer']['formula_id'],'precio_cop':'22000','componentes':[{'articulo_id':arts['FRASCO30'],'cantidad_base':'1'}]};admin.post('/ofertas',badrefill,status=409,rf=['PS-RF03'])
 phase='ventas_consumos_cobros'
 # Reuse administrator's currently open turn if root has opened it; isolate financial fixture labels.
 turns=admin.get('/turnos');active=[x for x in turns if x['cerrada_en']is None]
 turn=active[0]['turno_id']if active else admin.post('/turnos',{'fondo_cop':'100'},rf=['PS-RF14'])['id'];fixtures['turno_id']=turn
 for i,x in enumerate(offers):
  before=stocks();vid=sale(admin,x['offer']);issued,emit=issue(admin,vid);after=stocks()
  expected={arts[k.upper()]:D(q)for k,q in x['rule']['liquidos_ml'].items()if D(q)>0}
  for aid,q in x['pack']:expected[aid]=expected.get(aid,D('0'))+D(q)
  check('Consumo exacto '+x['rule']['id']+' '+x['modalidad'],all(before[aid]-after[aid]==q for aid,q in expected.items()),{'expected':{a:str(q)for a,q in expected.items()}},['PS-RF03','PS-RF05','PS-RF06'])
  check('Precio reportado/derivado '+x['rule']['id']+' '+x['modalidad'],D(str(issued['total_cop']))==x['price'],str(issued['total_cop']),['PS-RF02'])
  if x['modalidad']=='RECARGA':check('Recarga no consume frasco/válvula/tapa',all(before[arts[k]]==after[arts[k]]for k in['FRASCO30','FRASCO50','FRASCO100','VALVULA','TAPA']),rf=['PS-RF03'])
  if i==0:
   admin.post('/ventas/'+vid+'/emitir',emit,rf=['PS-RF06']);check('Repetir emisión no consume otra vez',stocks()==after,rf=['PS-RF06'])
   admin.post('/ventas/'+vid+'/emitir',{**emit,'credito':True},status=409,rf=['PS-RF06'])
  check('Contado emitido y cobrado atómicamente',issued['estado_pago']=='PAGADA' and D(str(issued['saldo_cop']))==0,rf=['PS-RF06','PS-RF07'])
  if i==0:fixtures['venta_normal30_id']=vid
 for off,qty in[(d5,D('5')),(d10,D('10'))]:
  b=stocks();vid=sale(admin,off);issued,_=issue(admin,vid);check('Decant descuenta volumen exacto sinmermaimplícita',b[arts['ABIERTO']]-stocks()[arts['ABIERTO']]==qty,rf=['PS-RF03','PS-RF12'])
 # New normal30 tariff does not mutate prior receipt.
 normal=offers[0]['offer'];historic=admin.get('/ventas/'+fixtures['venta_normal30_id'])['total_cop'];admin.req('PUT','/ofertas/'+normal['oferta_id']+'/precio',{'importe_unitario_cop':'26000'},rf=['PS-RF02']);check('Precio histórico inmutable',admin.get('/ventas/'+fixtures['venta_normal30_id'])['total_cop']==historic,rf=['PS-RF02'])
 vendor.req('PUT','/ofertas/'+normal['oferta_id']+'/precio',{'importe_unitario_cop':'1'},status=403,rf=['PS-RF01','PS-RF09'])
 phase='fiado_parciales_descuento'
 vcredit=sale(vendor,normal,customer=customer);vendor.post('/ventas/'+vcredit+'/descuento',{'importe_cop':'1000','motivo':'DEMO'},status=403,rf=['PS-RF09']);admin.post('/ventas/'+vcredit+'/descuento',{'importe_cop':'1000','motivo':'Descuento de laboratorio autorizado'},rf=['PS-RF09']);b=stocks();issued,emit=issue(admin,vcredit,True);s=stocks();check('Fiado entregado consume antes de cobro',b[arts['ESENCIA']]-s[arts['ESENCIA']]==10 and D(str(issued['cobrado_cop']))==0,rf=['PS-RF06','PS-RF10']);check('Descuento conserva precio y total',D(str(issued['total_cop']))==25000,rf=['PS-RF09'])
 pay={'turno_id':turn,'medio':'NEQUI','importe_cop':'10000','referencia_externa':prefix+'_TRANSFERENCIA_DEMO','confirmado':False,'clave_operacion':prefix+'_ABONO1'};admin.post('/ventas/'+vcredit+'/cobros',pay,status=409,rf=['PS-RF07']);pay['confirmado']=True;pid=admin.post('/ventas/'+vcredit+'/cobros',pay,rf=['PS-RF07','PS-RF10'])['id'];pid2=admin.post('/ventas/'+vcredit+'/cobros',pay,rf=['PS-RF07'])['id'];part=admin.get('/ventas/'+vcredit);check('Abono parcial reduce saldo una vez y no stock',pid==pid2 and D(str(part['saldo_cop']))==15000 and part['estado_pago']=='PARCIAL'and stocks()==s,rf=['PS-RF07','PS-RF10']);admin.post('/ventas/'+vcredit+'/cobros',{**pay,'importe_cop':'11000'},status=409,rf=['PS-RF07'])
 admin.post('/ventas/'+vcredit+'/cobros',{'turno_id':turn,'medio':'EFECTIVO','importe_cop':'16000','confirmado':True,'clave_operacion':prefix+'_SOBREPAGO'},status=409,rf=['PS-RF07']);admin.post('/ventas/'+vcredit+'/cobros',{'turno_id':turn,'medio':'DAVIPLATA','importe_cop':'15000','referencia_externa':prefix+'_DAVIPLATA_DEMO','confirmado':True,'clave_operacion':prefix+'_ABONO2'},rf=['PS-RF07','PS-RF10']);final=admin.get('/ventas/'+vcredit);check('Fiado saldado sin nuevo consumo',final['estado_pago']=='PAGADA'and stocks()==s,rf=['PS-RF10']);fixtures['venta_credito_id']=vcredit
 phase='incidencias_gastos'
 incidence={'tipo':'MERMA','motivo':'Medición sintética de derrame10ml','clave_operacion':prefix+'_MERMA','lineas':[{'articulo_id':arts['ALCOHOL'],'cantidad_base':'10','direccion':'SALIDA'}]};b=stocks();iid=admin.post('/incidencias',incidence,rf=['PS-RF12'])['id'];admin.post('/incidencias',incidence,rf=['PS-RF12']);check('Merma real deensayo10ml una sola vez',b[arts['ALCOHOL']]-stocks()[arts['ALCOHOL']]==10,rf=['PS-RF12'])
 b=stocks();admin.post('/incidencias',{'tipo':'MUESTRA','venta_id':fixtures['venta_normal30_id'],'linea_no':1,'motivo':'Reclasificación de mezcla ya consumida DEMO','clave_operacion':prefix+'_MUESTRA','lineas':[]},rf=['PS-RF12']);check('Reclasificar mezcla no repite consumo',stocks()==b,rf=['PS-RF12'])
 b=stocks();admin.post('/incidencias',{'tipo':'REPOSICION','venta_id':fixtures['venta_normal30_id'],'linea_no':1,'motivo':'Válvula defectuosa DEMO','clave_operacion':prefix+'_REPOSICION','lineas':[{'articulo_id':arts['VALVULA'],'cantidad_base':'1','direccion':'SALIDA'}]},rf=['PS-RF11']);check('Reposición trazable consume válvula',b[arts['VALVULA']]-stocks()[arts['VALVULA']]==1,rf=['PS-RF11'])
 expense={'fecha':datetime.date.today().isoformat(),'concepto':'Gasto DEMO','importe_cop':'1000','documento_ref':prefix+'_SOPORTE_DEMO','turno_id':turn,'medio':'EFECTIVO','clave_operacion':prefix+'_GASTO'};gid=admin.post('/gastos',expense,rf=['PS-RF18'])['id'];gid2=admin.post('/gastos',expense,rf=['PS-RF18'])['id'];check('Gasto idempotente',gid==gid2,rf=['PS-RF18']);admin.post('/gastos',{**expense,'clave_operacion':prefix+'_GASTO_INVALIDO','importe_cop':'-1'},status=400,rf=['PS-RF18'])
 phase='regresiones_negativas_y_devoluciones'
 # The same body and operation key must never cross inventory operation routes.
 superset={**rec,'documento_proveedor':prefix+'_SUP_FACTURA','clave_operacion':prefix+'_SUP','articulo_sellado_id':arts['SELLADO'],'articulo_abierto_id':arts['ABIERTO'],'unidades':1,'volumen_util_ml':'100','motivo':'Superset sintético'}
 admin.post('/recepciones',superset,rf=['PS-RF04']);sb=stocks();admin.post('/aperturas',superset,status=409,rf=['PS-RF05']);check('Clave recepción no se reutiliza como apertura',stocks()==sb,rf=['PS-RF04','PS-RF05'])
 admin.post('/recepciones',{**rec,'clave_operacion':prefix+'_DUPFACT'},status=409,rf=['PS-RF04']);check('Factura proveedor duplicada no repite compra',stocks()==sb,rf=['PS-RF04'])
 # Delimiter and type collisions are no longer accepted as identical requests.
 collision={'tipo':'MERMA','motivo':'A;nota:B','clave_operacion':prefix+'_COLLISION','lineas':[]}
 admin.post('/incidencias',collision,rf=['PS-RF12']);admin.post('/incidencias',{**collision,'motivo':'A','nota':'B'},status=409,rf=['PS-RF12'])
 admin.post('/incidencias',{**collision,'clave_operacion':prefix+'_TYPE','observacion':True},rf=['PS-RF12']);admin.post('/incidencias',{**collision,'clave_operacion':prefix+'_TYPE','observacion':'true'},status=409,rf=['PS-RF12'])
 # Route identity belongs to a payment fingerprint, even when the body is identical.
 other=sale(admin,normal,customer=customer);issue(admin,other,True);ob=admin.get('/ventas/'+other);admin.post('/ventas/'+other+'/cobros',pay,status=409,rf=['PS-RF07','PS-RF10']);check('Clave cobro de otra venta no cruza origen',admin.get('/ventas/'+other)['cobrado_cop']==ob['cobrado_cop'],rf=['PS-RF07'])
 # External references cannot confirm two different collections.
 admin.post('/ventas/'+other+'/cobros',{**pay,'clave_operacion':prefix+'_DUPREF'},status=409,rf=['PS-RF07'])
 # No credit authorization can be bypassed by a cash sale without payment.
 pending=sale(vendor,normal);sb=stocks();vendor.post('/ventas/'+pending+'/emitir',{'clave_operacion':prefix+'_NOPAY','credito':False},status=400,rf=['PS-RF06','PS-RF07']);check('Contado sin cobro no entrega ni consume',vendor.get('/ventas/'+pending)['estado']=='BORRADOR' and stocks()==sb,rf=['PS-RF06','PS-RF07'])
 badpay={'clave_operacion':prefix+'_UNCONFIRMED','credito':False,'cobro':{'turno_id':turn,'medio':'EFECTIVO','importe_cop':'26000','confirmado':False}}
 admin.post('/ventas/'+pending+'/emitir',badpay,status=409,rf=['PS-RF06','PS-RF07']);check('Fallo cobro revierte estado y consumo completos',admin.get('/ventas/'+pending)['estado']=='BORRADOR' and stocks()==sb,rf=['PS-RF06','PS-RF07'])
 admin.post('/ventas/'+pending+'/emitir',{**badpay,'clave_operacion':prefix+'_SHORT','cobro':{**badpay['cobro'],'confirmado':True,'importe_cop':'1000'}},status=409,rf=['PS-RF07'])
 # A refund is a separate actual cash movement, linked to its incident and capped by collected funds.
 origin=fixtures['venta_normal30_id'];prev=admin.get('/turnos/'+turn);sb=stocks()
 refund={'tipo':'DEVOLUCION','venta_id':origin,'linea_no':1,'motivo':'Ensayo devolución parcial10COP sin devolución física','devolucion_cop':'10','turno_id':turn,'clave_operacion':prefix+'_REFUND','lineas':[]}
 ri=admin.post('/incidencias',refund,rf=['PS-RF11'])['id'];admin.post('/incidencias',refund,rf=['PS-RF11']);now=admin.get('/turnos/'+turn);rm=[x for x in now['movimientos']if x.get('incidencia_id')==ri];check('Reembolso ligado a incidencia una vez, sin stock implícito',len(rm)==1 and D(str(prev['esperado_efectivo_cop']))-D(str(now['esperado_efectivo_cop']))==10 and stocks()==sb,rf=['PS-RF11','PS-RF14'])
 admin.post('/incidencias',{**refund,'clave_operacion':prefix+'_EXCESS_REFUND','devolucion_cop':'25000'},status=409,rf=['PS-RF11'])
 # Return materials retain the historical supply cost, not a free invented zero.
 ret={'tipo':'DEVOLUCION','venta_id':origin,'linea_no':1,'motivo':'Envase sintético retornado','clave_operacion':prefix+'_RETURN','lineas':[{'articulo_id':arts['FRASCO30'],'cantidad_base':'1','direccion':'ENTRADA'}]}
 admin.post('/incidencias',ret,rf=['PS-RF11','PS-RF17']);check('Entrada derivada devuelve exactamente envase sin fabricar líquido',stocks()[arts['FRASCO30']]-sb[arts['FRASCO30']]==1,rf=['PS-RF11'])
 admin.post('/incidencias',{**ret,'clave_operacion':prefix+'_RETURN2'},status=409,rf=['PS-RF11'])
 admin.post('/incidencias',{'tipo':'MERMA','motivo':'Entrada sin valoración','clave_operacion':prefix+'_NOCOST','lineas':[{'articulo_id':arts['ALCOHOL'],'cantidad_base':'1','direccion':'ENTRADA'}]},status=409,rf=['PS-RF17'])
 for tipo in ['MERMA','MUESTRA']:admin.post('/incidencias',{'tipo':tipo,'motivo':'Entrada ilegítima DEMO','clave_operacion':prefix+'_BADENTRY_'+tipo,'lineas':[{'articulo_id':arts['ALCOHOL'],'cantidad_base':'1','direccion':'ENTRADA','costo_unitario_base_cop':'10'}]},status=409,rf=['PS-RF12'])
 # A physical count records an observation and does not automatically adjust stock.
 sb=stocks();count=admin.post('/conteos',{'observacion':'Conteo DEMO separado de piloto cliente','renglones':[{'articulo_id':arts['ALCOHOL'],'cantidad_medida':'1','unidad_medicion':'L'}]},rf=['PS-RF05'])['id'];check('Conteo conserva observación sin alterar stock',stocks()==sb and any(x['conteo_id']==count for x in admin.get('/conteos')),rf=['PS-RF05']);vendor.get('/conteos',status=403,rf=['PS-RF01'])
 admin.post('/conteos',{'observacion':'Conteo fraccional inválido','renglones':[{'articulo_id':arts['FRASCO30'],'cantidad_medida':'1.5','unidad_medicion':'UNIDAD'}]},status=400,rf=['PS-RF05'])
 # Timestamp is explicitly UTC; browser conversion must not silently shift business date.
 stamp=admin.get('/ventas/'+origin)['fecha_emision'];check('Fechas HTTP llevan zona UTC explícita',isinstance(stamp,str)and stamp.endswith('Z'),stamp,['PS-RF15'])
 # Invalid supply templates must roll back the entire offer transaction.
 template={'codigo_venta':prefix+'_INVALID','nombre_venta':'Suministro inválido DEMO','modalidad':'PREPARACION','fragancia_id':frag,'volumen_ml':'30','formula_id':offers[0]['offer']['formula_id'],'precio_cop':'25000','componentes':[]}
 admin.post('/ofertas',template,status=409,rf=['PS-RF03'])
 admin.post('/ofertas',{**template,'codigo_venta':prefix+'_SIZE','componentes':[{'articulo_id':arts[x],'cantidad_base':'1'}for x in ['FRASCO50','VALVULA','TAPA','ETIQUETA']]},status=409,rf=['PS-RF03'])
 admin.post('/ofertas',{**template,'codigo_venta':prefix+'_LIQEXTRA','modalidad':'RECARGA','componentes':[{'articulo_id':arts['ABIERTO'],'cantidad_base':'1'}]},status=409,rf=['PS-RF03'])
 dt={'codigo_venta':prefix+'_DEC_BAD','nombre_venta':'Decant inválido DEMO','modalidad':'DECANT','fragancia_id':frag,'volumen_ml':'5','precio_cop':'10000','componentes':[{'articulo_id':arts['ABIERTO'],'cantidad_base':'5'}]}
 admin.post('/ofertas',dt,status=409,rf=['PS-RF03']);admin.post('/ofertas',{**dt,'codigo_venta':prefix+'_DEC_SIZE','componentes':dt['componentes']+[{'articulo_id':arts['ATOMIZADOR10'],'cantidad_base':'1'},{'articulo_id':arts['ETIQUETA_PEQUENA'],'cantidad_base':'1'}]},status=409,rf=['PS-RF03'])
 admin.post('/articulos',{'codigo':prefix+'_ROLE','nombre':'Función inválida DEMO','unidad_base':'ML','clase':'LIQUIDO','funcion_material':'ETIQUETA'},status=409,rf=['PS-RF03'])
 phase='valoracion_nula_importada'
 # Controlled DB fixture represents an imported, explicitly unvalued stock record.
 # No public API creates stock with unknown cost; no invented zero may leave this fixture.
 import subprocess,os
 def fixture_sql(sql):
  e=os.environ.copy();e['MYSQL_PWD']=cred['db_password'];r=subprocess.run([MYSQL,'--no-defaults','-h127.0.0.1','-P'+MYSQL_PORT,'-u'+cred['db_user'],cred['db']],input=sql,text=True,capture_output=True,env=e)
  check('SQL fixture controlada ejecutada',r.returncode==0,{'exit_code':r.returncode,'error_sql':r.stderr[-300:] if r.returncode else None},['PS-RF17'])
  if r.returncode:raise RuntimeError('Fixture SQL no aplicada')
 unvalued=art('SIN_COSTO','UNIDAD','PERFUME_SELLADO','1');doc=str(uuid.uuid4());actorid=admin.get('/auth/sesion')['actor']['usuario_id']
 fixture_sql("INSERT INTO documento_inventario(documento_id,tipo,registrado_en,estado,registrado_por_usuario_id,proveedor_id,referencia_factura,motivo,clave_operacion,huella_solicitud) VALUES ('"+doc+"','RECEPCION',UTC_TIMESTAMP(6),'CONFIRMADO','"+actorid+"','"+supplier+"','"+prefix+"_SIN_COSTO','Importación DEMO sin costo','"+prefix+"_SIN_COSTO','"+'0'*64+"');INSERT INTO renglon_inventario VALUES ('"+doc+"',1,'"+unvalued+"',1,'UNIDAD',NULL);")
 unoff=offer('SIN_COSTO','TERMINADO','1',None,[(unvalued,'1')],'1000',frag);unsale=sale(admin,unoff);b=stocks();ur=next(x for x in admin.get('/articulos')if x['articulo_id']==unvalued);check('Costo no valorado se muestra NULL y pendiente',ur['costo_promedio_cop']is None and ur['valoracion_pendiente'],rf=['PS-RF17'])
 admin.post('/ventas/'+unsale+'/emitir',{'credito':False,'clave_operacion':prefix+'_UNVALUED','cobro':{'turno_id':turn,'medio':'EFECTIVO','importe_cop':'1000','confirmado':True}},status=409,rf=['PS-RF17']);check('Salida sin valoración no inventa costo cero',stocks()==b and admin.get('/ventas/'+unsale)['estado']=='BORRADOR',rf=['PS-RF17'])
 fixture_sql("UPDATE renglon_inventario SET costo_unitario_base_cop=100 WHERE documento_id='"+doc+"';")
 issued,_=issue(admin,unsale);check('Con valoración explícita DEMO se reconoce costo100',D(str(issued['costo_materiales_cop']))==100 and issued['estado_pago']=='PAGADA',rf=['PS-RF17'])
 phase='reportes_excel_caja'
 today=datetime.date.today().isoformat()
 for tipo,rf in [('ventas','PS-RF15'),('medios','PS-RF15'),('rotacion','PS-RF16'),('costos','PS-RF17'),('vendedoras','PS-RF20'),('gastos','PS-RF18'),('devoluciones','PS-RF11')]:
  result=admin.get('/reportes?tipo='+tipo+'&desde='+today+'&hasta='+today,rf=[rf]);check('Reporte '+tipo+' con documentos realesDEMO',len(result['filas'])>0,rf=[rf])
 sellers=admin.get('/reportes?tipo=vendedoras&desde='+today+'&hasta='+today)['filas'];check('Reporte conserva perfil Admin sin atribuirlo a Vendedora',any(x['perfil']=='ADMINISTRADOR' for x in sellers),rf=['PS-RF20'])
 costsrows=admin.get('/reportes?tipo=costos&desde='+today+'&hasta='+today)['filas'];c30=next(x for x in costsrows if x['venta_id']==fixtures['venta_normal30_id']);check('Costo30normalDEMO explícito y margenbruto',D(str(c30['costo_materiales_cop']))==D('2366'),str(c30),['PS-RF17']);check('Margen ajustado incluye devolución y costo retorno/reposición',D(str(c30['devoluciones_acumuladas_cop']))==10 and D(str(c30['costo_ajustes_cop']))==D('-900') and D(str(c30['margen_bruto_ajustado_cop']))==D('23524'),str(c30),['PS-RF11','PS-RF17'])
 books=admin.req('GET','/exportacion.xlsx?mes='+today[:7],raw=True,rf=['PS-RF19']);(Q/'exportacion_DEMO.xlsx').write_bytes(books);z=zipfile.ZipFile(io.BytesIO(books));check('XLSXreal5hojas noCSV',len([n for n in z.namelist()if n.startswith('xl/worksheets/sheet')])==5 and '[Content_Types].xml'in z.namelist(),rf=['PS-RF19']);
 try:
  import openpyxl
  w=openpyxl.load_workbook(io.BytesIO(books),read_only=True,data_only=True);check('Lectura posterior XLSX5hojas',w.sheetnames==['Ventas','Cobros','Gastos','Cartera','Devoluciones'],w.sheetnames,['PS-RF19']);head=next(w['Cartera'].iter_rows(values_only=True));check('Cartera exportada rotula corte vigente con instante', 'naturaleza_corte'in head and 'consultada_en_utc'in head,head,['PS-RF19']);w.close()
 except ImportError:pass
 shift=admin.get('/turnos/'+turn);cash=D(str(shift['esperado_efectivo_cop']));check('Caja separa medios deefectivo',D(str(shift['totales_por_medio']['NEQUI']))>=10000 and D(str(shift['totales_por_medio']['DAVIPLATA']))>=15000 and cash>0,rf=['PS-RF14'])
 # Retain a deliberately different actual DEMO count instead of forcing conformance.
 close={'conciliaciones':[{'medio':med,'importe_verificado_cop':str(cash+D('1')if med=='EFECTIVO'else shift['totales_por_medio'][med])}for med in ['EFECTIVO','NEQUI','DAVIPLATA','TARJETA']],'observacion':'Conteo DEMOcon diferencia deliberada1COP'}
 closed=admin.post('/turnos/'+turn+'/cerrar',close,rf=['PS-RF14']);check('Cierre conserva diferencia1COP',next(D(str(x['diferencia_cop']))for x in closed['conciliaciones']if x['medio']=='EFECTIVO')==1,rf=['PS-RF14'])
 phase='concurrencia'
 turn=admin.post('/turnos',{'fondo_cop':'0'},rf=['PS-RF14'])['id']
 scarce=art('ULTIMA','UNIDAD','PERFUME_SELLADO','1');admin.post('/recepciones',{'proveedor_id':supplier,'documento_proveedor':prefix+'_ULTIMA','clave_operacion':prefix+'_RECULT','lineas':[{'articulo_id':scarce,'cantidad':'1','unidad_codigo':'UNIDAD','costo_total_cop':'100'}]},rf=['PS-RF04']);last=offer('ULTIMA','TERMINADO','1',None,[(scarce,'1')],'1000',frag);v1=sale(admin,last);v2=sale(admin,last)
 c1=Client();c1.login(cred['admin_user'],cred['admin_password']);c2=Client();c2.login(cred['admin_user'],cred['admin_password'])
 def concurrent_emit(client,vid):
  data={'clave_operacion':prefix+'_RACE'+vid,'credito':False,'cobro':{'turno_id':turn,'medio':'EFECTIVO','importe_cop':'1000','confirmado':True}};req=urllib.request.Request(API+'/ventas/'+vid+'/emitir',data=json.dumps(data).encode(),headers={'Content-Type':'application/json','X-CSRF-Token':client.csrf},method='POST')
  try:
   with client.op.open(req,timeout=12)as r:return r.status,json.loads(r.read(),parse_float=D)
  except urllib.error.HTTPError as e:return e.code,json.loads(e.read())
 with concurrent.futures.ThreadPoolExecutor(2)as ex:out=list(ex.map(lambda v:concurrent_emit(*v),[(c1,v1),(c2,v2)]))
 check('ConcurrenciaMySQLúltimaunidad:unaemisión,unrechazo,saldo0',sorted(x[0]for x in out)==[200,409]and stocks()[scarce]==0,{'statuses':[x[0]for x in out],'saldo':str(stocks()[scarce])},['PS-RF05','PS-RF06'])
 fixtures.update({'articulos':arts,'ofertas':[x['offer']for x in offers],'decants':[d5,d10],'usuario_vendedora_id':uid,'usuario_vendedora_nombre':vendor_name,'proveedor_id':supplier,'cliente_id':customer,'fragancia_id':frag});phase='completo'
except Exception as e:
 checks.append({'nombre':'FALLO_DE_BATERIA','cumple':False,'fase':phase,'error':str(e),'traceback':traceback.format_exc()});raise
finally:
 (Q/'api_mysql_resultados.json').write_text(json.dumps({'fecha_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'naturaleza':'Pruebas técnicas propias ejecutadas contraAPIJava yMySQLaislados; todosdatosDEMO. No piloto ni aceptacióncliente.','fase_final':phase,'checks':checks,'total':len(checks),'conformes':sum(x.get('cumple')is True for x in checks),'fuentes_reales_negocio_usadas_como_inventario':False},ensure_ascii=False,indent=2,default=str))
 (Q/'fixtures_DEMO.json').write_text(json.dumps(fixtures,ensure_ascii=False,indent=2,default=str))
 print(json.dumps({'fase_final':phase,'checks':len(checks),'conformes':sum(x.get('cumple')is True for x in checks)},ensure_ascii=False))
