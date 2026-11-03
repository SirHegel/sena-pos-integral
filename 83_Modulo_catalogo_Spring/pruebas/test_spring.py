from pathlib import Path
import urllib.request,urllib.error,urllib.parse,http.cookiejar,re,json,uuid,datetime

OUT=Path(__file__).resolve().parent/'salidas';OUT.mkdir(exist_ok=True)
BASE='http://127.0.0.1:8083'
A='11111111-1111-4111-8111-111111111111';B='22222222-2222-4222-8222-222222222222'
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*args):return None
cookies=http.cookiejar.CookieJar()
client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies),NoRedirect())
results=[]
def call(path,fields=None):
 data=None if fields is None else urllib.parse.urlencode(fields).encode()
 req=urllib.request.Request(BASE+path,data=data)
 try:
  with client.open(req,timeout=15) as f:status=f.status;body=f.read().decode();headers=dict(f.headers)
 except urllib.error.HTTPError as f:status=f.code;body=f.read().decode();headers=dict(f.headers)
 return status,body,headers
def check(name,condition,detail=''):
 results.append({'caso':name,'correcto':bool(condition),'detalle':detail});print(name,'OK' if condition else 'FALLO',flush=True)
def location(headers):return headers.get('Location',headers.get('location',''))
def token(body):return re.search(r'name="csrf" value="([^"]+)"',body).group(1)
def find_id(body):return re.search(r'modo=editar&amp;id=([a-f0-9-]{36})',body).group(1)

status,body,headers=call('/catalogo')
check('GET compila y muestra JSP del catálogo',status==200 and '<h1>Catálogo</h1>' in body,str(status))
check('Cabeceras antiinterpretación y CSP',headers.get('X-Content-Type-Options')=='nosniff' and "frame-ancestors 'none'" in headers.get('Content-Security-Policy',''))
check('Cookie sesión HttpOnly y SameSite',any(c.name=='JSESSIONID' and 'HttpOnly' in c._rest and c._rest.get('SameSite','').lower()=='lax' for c in cookies))
status,body,_=call('/catalogo?modo=nuevo&tenant='+A);csrf=token(body)
check('GET muestra formulario JSP',status==200 and 'method="post"' in body and 'name="precio"' in body)
code='QA83-'+uuid.uuid4().hex[:8]
fields={'tenant':A,'csrf':csrf,'accion':'crear','codigo':code,'nombre':'<script>alert(1)</script>','precio':'1250.50','iva':'0.19','tipo':'VENDIBLE','unidad':'unidad','modoDescuento':'DIRECTO'}
status,body,headers=call('/catalogo',fields)
check('POST crea y responde303',status==303,str(status))
status,body,_=call(location(headers));id_a=find_id(body)
check('GET recupera resultado persistido',status==200 and code in body and '1250.50' in body)
check('JSTL escapa textoHTML del producto','&lt;script&gt;alert(1)&lt;/script&gt;' in body and '<script>alert(1)</script>' not in body)
status,_,_=call('/catalogo',{**fields,'csrf':'no-valido','codigo':code+'X'})
check('POST rechaza tokenCSRF inválido',status==403,str(status))
status,_,_=call('/catalogo',{k:v for k,v in fields.items() if k!='csrf'})
check('POST rechaza tokenCSRF ausente',status==403,str(status))
status,_,_=call('/catalogo',fields)
check('POST rechaza código duplicado',status==409,str(status))
status,_,_=call('/catalogo',{**fields,'codigo':code+'CERO','precio':'0'})
check('Aplicación rechaza precio cero',status==400,str(status))
status,_,_=call('/catalogo',{**fields,'codigo':code+'NEG','precio':'-1'})
check('POST rechaza precio negativo',status==400,str(status))
status,_,_=call('/catalogo',{**fields,'codigo':code+'DEC','precio':'1.234'})
check('POST rechaza precisión monetaria',status==400,str(status))
status,_,_=call('/catalogo',{**fields,'codigo':code+'IVA','iva':'1.1'})
check('POST rechazaIVA fuera de rango',status==400,str(status))
status,_,_=call('/catalogo',{**fields,'codigo':code+'REC','tipo':'INSUMO','modoDescuento':'RECETA'})
check('POST rechaza receta para insumo',status==400,str(status))
status,_,_=call('/catalogo',{'tenant':A,'csrf':csrf,'accion':'crear'})
check('POST rechaza campos ausentes',status==400,str(status))
for label,change in [
 ('Nombre mayor de128caracteres',{'nombre':'x'*129}),
 ('Código mayor de64caracteres',{'codigo':'x'*65}),
 ('Unidad mayor de32caracteres',{'unidad':'x'*33}),
 ('Nombre sólo espacios',{'nombre':'   '}),
 ('Caracteres de control en nombre',{'nombre':'Nombre\nno válido'})]:
 status,_,_=call('/catalogo',{**fields,'codigo':code+'LIM',**change})
 check(label,status==400,str(status))
status,_,_=call('/catalogo?tenant='+A+'&q='+'x'*129)
check('Búsqueda mayor de128caracteres',status==400,str(status))
status,body,_=call('/catalogo?tenant='+A+'&q='+urllib.parse.quote(code))
check('GET procesa búsqueda por parámetro',status==200 and code in body)
status,body,_=call('/catalogo?tenant='+A+'&q='+urllib.parse.quote('<img src=x onerror=alert(1)>'))
check('GET escapa búsqueda en atributo','&lt;img src=x onerror=alert(1)&gt;' in body and '<img src=x onerror=alert(1)>' not in body)
status,_,_=call('/catalogo?tenant=otro')
check('GET rechaza negocio no permitido',status==400,str(status))
status,_,_=call('/catalogo?tenant='+B+'&modo=editar&id='+id_a)
check('GET no edita clave de otro negocio',status==404,str(status))
status,_,_=call('/catalogo',{**fields,'tenant':B,'accion':'actualizar','id':id_a,'nombre':'Ajeno'})
check('POST no actualiza clave de otro negocio',status==404,str(status))
status,_,_=call('/catalogo',{'tenant':B,'csrf':csrf,'accion':'eliminar','id':id_a})
check('POST no elimina clave de otro negocio',status==404,str(status))
status,_,headers=call('/catalogo',{**fields,'tenant':B,'nombre':'Otro negocio'})
check('Código repetible entre negocios en web',status==303,str(status))
_,body,_=call(location(headers));id_b=find_id(body)
status,_,headers=call('/catalogo',{**fields,'accion':'actualizar','id':id_a,'nombre':'Producto actualizado','precio':'1500.75'})
check('POST actualiza existente',status==303,str(status))
status,body,_=call(location(headers))
check('GET verifica actualización persistida','Producto actualizado' in body and '1500.75' in body)
status,body,_=call('/catalogo?tenant='+A+'&accion=eliminar&id='+id_a)
check('GET no elimina datos',status==200 and code in body)
status,_,_=call('/WEB-INF/jsp/catalogo.jsp')
check('JSP no accesible directamente',status==404,str(status))
status,_,_=call('/catalogo?modo=no-existe')
check('Modo GET desconocido rechazado',status==400,str(status))
status,_,_=call('/catalogo',{'tenant':A,'csrf':csrf,'accion':'no-existe'})
check('Acción POST desconocida rechazada',status==400,str(status))
for tenant,product in [(A,id_a),(B,id_b)]:
 status,_,headers=call('/catalogo',{'tenant':tenant,'csrf':csrf,'accion':'eliminar','id':product})
 check('POST elimina producto sintético de '+tenant[0],status==303,str(status))
 _,body,_=call(location(headers));check('Eliminación persistida de '+tenant[0],code not in body)

report={'fecha_UTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'base_URL':BASE,'instrumento':'HTTP real con cookie de sesión; SpringMVC/JPA/Hibernate y Jasper/Tomcat reales','datos':'Productos sintéticos retirados por POST al finalizar','resultados':results,'total':len(results),'correctos':sum(x['correcto'] for x in results),'nota':'No es un video ni se atribuye a Postman. La selección de dos negocios es demostración, no autorización.'}
(OUT/'resultados_spring_jpa.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print('TOTAL',report['correctos'],'/',report['total']);assert report['correctos']==report['total']
