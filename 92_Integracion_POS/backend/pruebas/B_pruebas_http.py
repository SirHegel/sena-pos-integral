#!/usr/bin/env python3
"""Pruebas HTTP reales del bloque operativo, exclusivamente sobre la base académica aislada.
Las credenciales se leen del archivo privado indicado; nunca se registran en los resultados.
"""
import argparse, concurrent.futures, json, uuid, threading
from decimal import Decimal
from pathlib import Path
import requests

T1='90000000-0000-4000-8000-000000000001'
T2='90000000-0000-4000-8000-000000000002'
D=lambda x:Decimal(str(x))

class Suite:
 def __init__(self,env,out):
  self.env=json.loads(Path(env).read_text());self.base='http://127.0.0.1:'+self.env['POS_HTTP_PORT']+'/api/v1';self.out=Path(out);self.rows=[];self.run=uuid.uuid4().hex[:10]
 def record(self,name,ok,detail=None):
  self.rows.append({'caso':name,'aprobado':bool(ok),'detalle':detail})
  if not ok:raise AssertionError(name+': '+str(detail))
 def login(self,role='administrador',tenant=T1):
  s=requests.Session();r=s.get(self.base+'/auth/csrf',timeout=20);r.raise_for_status();s.headers['X-CSRF-Token']=r.json()['csrfToken']
  r=s.post(self.base+'/auth/ingreso',json={'tenantId':tenant,'correo':role+'@demo.example','password':self.env['POS_DEMO_PASSWORD']},timeout=20);r.raise_for_status();s.headers['X-CSRF-Token']=r.json()['csrfToken'];return s
 def call(self,s,method,path,body=None,status=200,key=None):
  h={'Idempotency-Key':key} if key else {};r=s.request(method,self.base+path,json=body,headers=h,timeout=30)
  try:data=r.json()
  except ValueError:data={'respuesta_no_json':r.text[:200]}
  self.record(method+' '+path+' HTTP'+str(status),r.status_code==status,{'http':r.status_code,'respuesta':data})
  return data
 def stock(self,s,b,p):
  return next((D(x['stock']) for x in self.call(s,'GET','/existencias?bodegaId='+b) if x['producto_id']==p),D(0))
 def product(self,s,code,price='10000.00',tax='0.1900',kind='VENDIBLE',mode='DIRECTO'):
  body={'codigo':self.run+code,'nombre':'Producto sintético '+code,'precioVigente':price,'tasaIva':tax,'tipo':kind,'unidad':'Unidad' if kind!='INSUMO' else 'g','modoDescuento':mode,'activo':True}
  p=self.call(s,'POST','/productos',body,201);return p['producto_id'],body
 def entry(self,s,b,p,qty,doc=None):
  return self.call(s,'POST','/movimientos',{'bodegaId':b,'productoId':p,'cantidadDelta':str(qty),'tipo':'ENTRADA','documentoOrigen':self.run+(doc or uuid.uuid4().hex)},201)
 def sale(self,s,b,c,lines,mesa=None,status=201):
  body={'bodegaId':b,'sesionCajaId':c,'lineas':[{'productoId':p,'cantidad':q} for p,q in lines]}
  if mesa:body['mesaId']=mesa
  return self.call(s,'POST','/ventas',body,status)
 def pay(self,s,v,medium='EFECTIVO',ref=None,key=None,status=200,amount=None):
  return self.call(s,'POST','/ventas/'+v['venta_id']+'/confirmar',{'medio':medium,'referencia':ref or self.run+uuid.uuid4().hex,'importe':str(amount if amount is not None else v['total'])},status,key or self.run+uuid.uuid4().hex)
 def concurrent(self,fn1,fn2):
  barrier=threading.Barrier(2)
  def go(fn):barrier.wait();return fn()
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
   f1=ex.submit(go,fn1);f2=ex.submit(go,fn2);return f1.result(),f2.result()
 def rawpay(self,s,v,key,ref):
  r=s.post(self.base+'/ventas/'+v['venta_id']+'/confirmar',json={'medio':'EFECTIVO','referencia':ref,'importe':str(v['total'])},headers={'Idempotency-Key':key},timeout=40);return r.status_code,r.json()
 def execute(self):
  r=requests.get(self.base+'/ventas',timeout=20);self.record('Sin sesión no consulta ventas',r.status_code==401)
  a=self.login();other=self.login(tenant=T2);cashier=self.login('cajero');inventory=self.login('inventario',tenant=T2);a2=self.login()
  b=self.call(a,'POST','/bodegas',{'nombre':'Bodega sintética '+self.run},201)['bodega_id']
  bo=self.call(other,'POST','/bodegas',{'nombre':'Bodega aislada '+self.run},201)['bodega_id']
  box=self.call(a,'POST','/cajas',{'nombre':'Caja sintética '+self.run},201)['caja_id']
  cs=self.call(a,'POST','/sesiones-caja',{'cajaId':box,'fondoInicial':'100.00'},201)['sesion_id']
  self.call(a,'POST','/sesiones-caja',{'cajaId':box,'fondoInicial':'100.00'},409)
  self.call(cashier,'GET','/sesiones-caja/'+cs,status=403)
  self.call(inventory,'GET','/ventas',status=403)
  self.call(cashier,'POST','/movimientos',{},403)
  p,body=self.product(a,'DIRECTO');self.entry(a,b,p,'5.000','entrada1');start=self.stock(a,b,p)
  self.entry(a,b,p,'5.000','entrada1');self.record('Entrada idempotente no duplica saldo',self.stock(a,b,p)==start)
  self.call(a,'POST','/movimientos',{'bodegaId':b,'productoId':p,'cantidadDelta':'6.000','tipo':'ENTRADA','documentoOrigen':self.run+'entrada1'},409)
  self.call(a,'POST','/movimientos',{'bodegaId':b,'productoId':p,'cantidadDelta':'-20.000','tipo':'AJUSTE','documentoOrigen':self.run+'negativo'},409)
  self.record('Ajuste fallido conserva saldo',self.stock(a,b,p)==start)
  self.sale(a,bo,cs,[(p,1)],status=404)
  self.sale(a,b,cs,[(p,'0.5')],status=400)
  self.sale(a,b,cs,[(p,6)],status=409)
  self.record('Adición sin stock se rechaza sin descontar',self.stock(a,b,p)==start)
  self.call(a,'POST','/bodegas',{'nombre':'No admisible','tenantId':T2},400)
  mesa=self.call(a,'POST','/mesas',{'codigo':'M-'+self.run},201)['mesa_id']
  draft=self.sale(a,b,cs,[(p,1)],mesa)
  self.sale(a,b,cs,[(p,1)],mesa,status=409)
  self.call(a,'PATCH','/mesas/'+mesa+'/estado',{'estado':'INACTIVA'},409)
  self.call(a,'POST','/ventas/'+draft['venta_id']+'/anular')
  self.record('Anular libera mesa',next(m for m in self.call(a,'GET','/mesas') if m['mesa_id']==mesa)['estado']=='DISPONIBLE')
  v=self.sale(a,b,cs,[(p,2)],mesa);ref=self.run+'pago1';key=self.run+'pago1'
  paid=self.pay(a,v,ref=ref,key=key);self.record('Precio e IVA por línea correctos',D(paid['base'])==D(20000) and D(paid['iva'])==D(3800) and D(paid['total'])==D(23800));after=self.stock(a,b,p)
  replay=self.pay(a,v,ref=ref,key=key);self.record('Repetición devuelve misma factura y saldo',replay['numero_factura']==paid['numero_factura'] and self.stock(a,b,p)==after)
  self.pay(a,v,ref=ref,key=key,status=409,amount='1.00');self.pay(a,v,status=409)
  self.call(a,'PUT','/ventas/'+v['venta_id']+'/detalle',{'lineas':[{'productoId':p,'cantidad':3}]},409)
  self.call(other,'GET','/ventas/'+v['venta_id']+'/comprobante',status=404)
  self.call(cashier,'GET','/ventas/'+v['venta_id']+'/comprobante',status=403)
  self.call(a,'PUT','/productos/'+p,dict(body,precioVigente='11000.00'))
  hist=self.call(a,'GET','/ventas/'+v['venta_id']+'/comprobante');self.record('Precio histórico permanece tras catálogo',D(hist['total'])==D(23800))
  stale=self.sale(a,b,cs,[(p,1)]);self.call(a,'PUT','/productos/'+p,dict(body,precioVigente='12000.00'));before=self.stock(a,b,p)
  self.pay(a,stale,status=409);self.record('Cambio de precio no altera existencias',self.stock(a,b,p)==before)
  fresh=self.call(a,'PUT','/ventas/'+stale['venta_id']+'/detalle',{'lineas':[{'productoId':p,'cantidad':1}]})
  rollback_key=self.run+'rollback';self.pay(a,fresh,ref=ref,key=rollback_key,status=409);self.record('Fallo tardío de referencia revierte stock',self.stock(a,b,p)==before)
  still=self.call(a,'GET','/ventas/'+stale['venta_id']+'/comprobante');self.record('Fallo tardío revierte pago y estado',still['estado']=='BORRADOR' and not still['pagos'])
  self.pay(a,fresh,medium='EXTERNO',key=rollback_key);self.record('Rollback retiró también la clave de idempotencia',True);summary=self.call(a,'GET','/sesiones-caja/'+cs);self.record('Efectivo y externo separados',D(summary['efectivo'])==D(23800) and D(summary['externo'])==D(14280) and D(summary['esperado_calculado'])==D(23900))
  # Dos platos comparten un insumo: demanda conjunta12 supera stock10 aunque cada plato aislado cabe.
  ingredient,_=self.product(a,'INSUMO','1.00','0.0000','INSUMO');self.entry(a,b,ingredient,'12.000')
  dishes=[]
  for i in [1,2]:
   dish,_=self.product(a,'PLATO'+str(i),'1000.00','0.0000','PLATO','RECETA');dishes.append(dish)
   recipe=self.call(a,'POST','/recetas',{'productoPlatoId':dish,'version':1,'rendimiento':'1.000','componentes':[{'productoInsumoId':ingredient,'cantidad':'6.000'}]},201)
   self.call(a,'POST','/recetas/'+recipe['receta_id']+'/activar')
  recipe_sale=self.sale(a,b,cs,[(dishes[0],1),(dishes[1],1)]);self.call(a,'POST','/movimientos',{'bodegaId':b,'productoId':ingredient,'cantidadDelta':'-2.000','tipo':'AJUSTE','documentoOrigen':self.run+'consumo_intermedio'},201);self.sale(a,b,cs,[(dishes[0],1),(dishes[1],1)],status=409);self.pay(a,recipe_sale,status=409)
  self.record('Demanda agregada falla sin consumir dos veces saldo',self.stock(a,b,ingredient)==D(10))
  self.entry(a,b,ingredient,'2.000');self.pay(a,recipe_sale);self.record('Dos recetas consumen12 sin descontar platos',self.stock(a,b,ingredient)==0 and self.stock(a,b,dishes[0])==0)
  # Fracción con rendimiento3: consumo conservador por componente a0.001.
  fractional,_=self.product(a,'FRACCION','100.00','0.0000','PLATO','RECETA');self.entry(a,b,ingredient,'1.000')
  rr=self.call(a,'POST','/recetas',{'productoPlatoId':fractional,'version':1,'rendimiento':'3.000','componentes':[{'productoInsumoId':ingredient,'cantidad':'1.000'}]},201);self.call(a,'POST','/recetas/'+rr['receta_id']+'/activar')
  fv=self.sale(a,b,cs,[(fractional,1)]);self.pay(a,fv);self.record('Rendimiento conserva unidad y redondeo0.001',self.stock(a,b,ingredient)==D('.666'))
  ledger=self.call(a,'GET','/movimientos?bodegaId='+b);self.record('Kardex receta conserva origen y consumo',any(m['venta_id']==fv['venta_id'] and D(m['cantidad_delta'])==D('-.334') and m['tipo_origen']=='RECETA' for m in ledger))
  self.call(a,'POST','/sesiones-caja/'+cs+'/cerrar',{'totalContado':str(self.call(a,'GET','/sesiones-caja/'+cs)['esperado_calculado'])})
  self.pay(a,v,ref=ref,key=key);self.sale(a,b,cs,[(p,1)],status=409)
  self.call(a,'POST','/sesiones-caja/'+cs+'/cerrar',{'totalContado':'0.00'},409)
  # Misma venta y misma clave en dos sesiones HTTP: ambas reciben la misma confirmación.
  box2=self.call(a,'POST','/cajas',{'nombre':'Caja concurrencia '+self.run},201)['caja_id'];cs2=self.call(a,'POST','/sesiones-caja',{'cajaId':box2,'fondoInicial':'0.00'},201)['sesion_id']
  self.entry(a,b,p,'20.000');cv=self.sale(a,b,cs2,[(p,1)]);before=self.stock(a,b,p);ck=self.run+'simultanea';cr=self.run+'simultanea'
  r1,r2=self.concurrent(lambda:self.rawpay(a,cv,ck,cr),lambda:self.rawpay(a2,cv,ck,cr));self.record('Cobros concurrentes misma clave ambos200 misma factura',r1[0]==200 and r2[0]==200 and r1[1]['numero_factura']==r2[1]['numero_factura'],{'http':[r1[0],r2[0]]});self.record('Cobro concurrente descuenta una sola vez',self.stock(a,b,p)==before-1)
  # Dos ventas compiten por la última unidad: nunca hay sobreventa.
  scarce,_=self.product(a,'ULTIMA','5.00','0.0000');self.entry(a,b,scarce,'1.000');sv1=self.sale(a,b,cs2,[(scarce,1)]);bx3=self.call(a,'POST','/cajas',{'nombre':'Caja independiente '+self.run},201)['caja_id'];cs3=self.call(a,'POST','/sesiones-caja',{'cajaId':bx3,'fondoInicial':'0.00'},201)['sesion_id'];sv2=self.sale(a,b,cs3,[(scarce,1)])
  r1,r2=self.concurrent(lambda:self.rawpay(a,sv1,self.run+'u1',self.run+'u1'),lambda:self.rawpay(a2,sv2,self.run+'u2',self.run+'u2'));self.record('Última unidad en cajas distintas sólo una venta confirmada',sorted([r1[0],r2[0]])==[200,409],{'http':[r1[0],r2[0]]});self.record('Concurrencia no produce saldo negativo',self.stock(a,b,scarce)==0)
  # Cierre y confirmación simultáneos: se acepta sólo un orden serial coherente.
  bx=self.call(a,'POST','/cajas',{'nombre':'Caja cierre '+self.run},201)['caja_id'];cx=self.call(a,'POST','/sesiones-caja',{'cajaId':bx,'fondoInicial':'10.00'},201)['sesion_id'];vs=self.sale(a,b,cx,[(p,1)])
  def close():
   r=a2.post(self.base+'/sesiones-caja/'+cx+'/cerrar',json={'totalContado':'10.00'},timeout=40);return r.status_code,r.json()
  rp,rc=self.concurrent(lambda:self.rawpay(a,vs,self.run+'cierre',self.run+'cierre'),close)
  expected=D('10')+(D(vs['total']) if rp[0]==200 else 0)
  self.record('Cierre concurrente serializa confirmación',rp[0] in [200,409] and rc[0]==200 and D(rc[1]['total_esperado'])==expected,{'http_pago':rp[0],'http_cierre':rc[0],'esperado':str(rc[1].get('total_esperado'))})
  # Saldos físicos y kardex se reconcilian para los productos probados, comenzados en cero.
  ledger=self.call(a,'GET','/movimientos?bodegaId='+b)
  for pid in [p,ingredient,scarce]:
   net=sum((D(m['cantidad_delta']) for m in ledger if m['producto_id']==pid),D(0));self.record('Saldo coincide con kardex '+pid,self.stock(a,b,pid)==net)
  self.out.parent.mkdir(parents=True,exist_ok=True);self.out.write_text(json.dumps({'estado':'Pruebas HTTP reales completadas','datos':'Sintéticos; ninguna operación financiera real','motor':'MySQL8.4.13','run':self.run,'total':len(self.rows),'aprobadas':sum(x['aprobado'] for x in self.rows),'casos':self.rows},ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--env',required=True);p.add_argument('--out',required=True);args=p.parse_args();s=Suite(args.env,args.out)
 try:s.execute()
 except Exception as e:
  s.out.parent.mkdir(parents=True,exist_ok=True);s.out.write_text(json.dumps({'estado':'Pruebas interrumpidas','error':type(e).__name__+': '+str(e),'run':s.run,'total':len(s.rows),'casos':s.rows},ensure_ascii=False,indent=2)+'\n');raise
 print(json.dumps({'total':len(s.rows),'aprobadas':sum(x['aprobado'] for x in s.rows)}))
