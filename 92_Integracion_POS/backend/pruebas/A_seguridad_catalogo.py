"""Pruebas HTTP reales del bloque A en un laboratorio local con datos sintéticos.

Uso: POS_TEST_CONFIG=/ruta/entorno_local.json python3 pruebas/A_seguridad_catalogo.py
La configuración nunca se imprime ni forma parte de los resultados.
"""
from pathlib import Path
import hashlib
import http.cookiejar
import json
import os
import time
import urllib.error
import urllib.request
import uuid

CONFIG = json.loads(Path(os.environ['POS_TEST_CONFIG']).read_text())
URL = CONFIG.get('POS_TEST_URL', 'http://127.0.0.1:8090') + '/api/v1'
T1 = '90000000-0000-4000-8000-000000000001'
T2 = '90000000-0000-4000-8000-000000000002'
PASSWORD = CONFIG['POS_DEMO_PASSWORD']
RESULTADOS = []

class Cliente:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.csrf = None
    def solicitar(self, method, path, body=None, csrf=True):
        headers = {'Accept': 'application/json'}
        if body is not None: headers['Content-Type'] = 'application/json'
        if csrf and self.csrf: headers['X-CSRF-Token'] = self.csrf
        request = urllib.request.Request(URL + path, data=None if body is None else json.dumps(body).encode(), headers=headers, method=method)
        try: response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error: response = error
        data = response.read().decode()
        try: data = json.loads(data)
        except json.JSONDecodeError: pass
        if isinstance(data, dict) and data.get('csrfToken'): self.csrf = data['csrfToken']
        return response.status, data, dict(response.headers)
    def iniciar(self, tenant=T1, correo='administrador@demo.example', password=PASSWORD):
        self.solicitar('GET', '/auth/csrf')
        return self.solicitar('POST','/auth/ingreso',{'tenantId':tenant,'correo':correo,'password':password})
    def sesion_id(self): return next((cookie.value for cookie in self.jar if cookie.name=='POSSESSION'), None)

def comprobar(nombre, obtenido, esperado):
    correcto = obtenido == esperado
    RESULTADOS.append({'caso':nombre,'esperado':esperado,'observado':obtenido,'cumple':correcto})
    if not correcto: raise AssertionError(f'{nombre}: observado {obtenido!r}; esperado {esperado!r}')

def codigo(cliente, method, path, body=None, csrf=True):
    return cliente.solicitar(method,path,body,csrf)[0]

def main():
    anonymous=Cliente()
    comprobar('Lectura protegida sin sesión',codigo(anonymous,'GET','/usuarios'),401)
    status,data,headers=anonymous.solicitar('GET','/auth/csrf')
    comprobar('Emisión CSRF',status,200)
    cookie=headers.get('Set-Cookie','')
    comprobar('Cookie HttpOnly y SameSite Strict',all(s in cookie for s in ['HttpOnly','SameSite=Strict']),True)
    comprobar('Login sin CSRF',codigo(anonymous,'POST','/auth/ingreso',{'tenantId':T1,'correo':'administrador@demo.example','password':PASSWORD},False),403)
    old_id,old_csrf=anonymous.sesion_id(),anonymous.csrf
    status,body,headers=anonymous.solicitar('POST','/auth/ingreso',{'tenantId':T1,'correo':'  ADMINISTRADOR@DEMO.EXAMPLE  ','password':PASSWORD})
    comprobar('Ingreso normaliza correo',status,200)
    comprobar('Identificador de sesión rotado',anonymous.sesion_id()!=old_id,True)
    comprobar('Token CSRF rotado',anonymous.csrf!=old_csrf,True)
    comprobar('Actor tenant servidor',body['actor']['tenantId'],T1)
    comprobar('Rol admin derivado de base',body['actor']['roles'],['ADMINISTRADOR'])
    a=anonymous
    comprobar('Listado de usuarios sin hash',any('hash_password' in r for r in a.solicitar('GET','/usuarios')[1]),False)
    product={'codigo':'QA-A-'+uuid.uuid4().hex[:12],'nombre':'Café <texto> & pruebas','precioVigente':'1250.50','tasaIva':'0.1900','tipo':'VENDIBLE','unidad':'UN','modoDescuento':'DIRECTO','activo':True}
    comprobar('Escritura sin CSRF',codigo(a,'POST','/productos',product,False),403)
    current=a.csrf;a.csrf=old_csrf
    comprobar('CSRF anterior al login rechazado',codigo(a,'POST','/productos',product),403);a.csrf=current
    status,p,_=a.solicitar('POST','/productos',product)
    comprobar('Crear producto con decimal exacto',status,201)
    pid=p['producto_id']
    comprobar('Nombre se conserva como texto',p['nombre'],product['nombre'])
    comprobar('Duplicado de código',codigo(a,'POST','/productos',product),409)
    comprobar('Precio cero inválido',codigo(a,'POST','/productos',{**product,'codigo':product['codigo']+'0','precioVigente':'0'}),400)
    comprobar('Demasiados decimales monetarios',codigo(a,'POST','/productos',{**product,'codigo':product['codigo']+'1','precioVigente':'1.001'}),400)
    comprobar('IVA mayor a uno',codigo(a,'POST','/productos',{**product,'codigo':product['codigo']+'2','tasaIva':'1.0001'}),400)
    comprobar('Campos de autorización ajenos rechazados',codigo(a,'POST','/productos',{**product,'codigo':product['codigo']+'3','tenantId':T2}),400)
    comprobar('Consulta SQL se trata como texto',a.solicitar('GET','/productos?q=%27%20OR%201%3D1--')[1],[])
    b=Cliente();comprobar('Ingreso segundo negocio',b.iniciar(T2)[0],200)
    comprobar('Producto ajeno inaccesible',codigo(b,'GET','/productos/'+pid),404)
    comprobar('Producto ajeno inmodificable',codigo(b,'PUT','/productos/'+pid,product),404)
    cashier=Cliente();comprobar('Ingreso cajero',cashier.iniciar(correo='cajero@demo.example')[0],200)
    comprobar('Cajero no administra usuarios',codigo(cashier,'GET','/usuarios'),403)
    comprobar('Cajero no escribe catálogo',codigo(cashier,'POST','/productos',product),403)
    admin_id=a.solicitar('GET','/auth/sesion')[1]['usuarioId']
    comprobar('Último admin no desactivable',codigo(a,'PUT','/usuarios/'+admin_id,{'activo':False,'roles':['CAJERO']}),409)
    comprobar('Roles vacíos rechazados',codigo(a,'POST','/usuarios',{'correo':'vaciorol@demo.example','password':PASSWORD,'roles':[]}),400)
    correo='qa-'+uuid.uuid4().hex[:12]+'@demo.example'
    status,user,_=a.solicitar('POST','/usuarios',{'correo':correo,'password':PASSWORD,'roles':['INVENTARIO']})
    comprobar('Alta usuario inventario',status,201)
    uid=user['usuarioId'];other=Cliente();comprobar('Ingreso usuario recién creado',other.iniciar(correo=correo)[0],200)
    comprobar('Permiso inventario crear catálogo',codigo(other,'POST','/productos',{**product,'codigo':product['codigo']+'4'}),201)
    comprobar('Cambio de rol por administrador',codigo(a,'PUT','/usuarios/'+uid,{'activo':True,'roles':['CAJERO']}),200)
    comprobar('Sesión previa relee nuevos roles',codigo(other,'POST','/productos',{**product,'codigo':product['codigo']+'5'}),403)
    comprobar('Desactivación usuario',codigo(a,'PUT','/usuarios/'+uid,{'activo':False,'roles':['CAJERO']}),200)
    comprobar('Sesión previa pierde acceso tras desactivación',codigo(other,'GET','/auth/sesion'),401)
    comprobante=Cliente();comprobante.iniciar(correo='inventario@demo.example')
    for n,expected in [(1,401),(2,401),(3,429)]:
        comprobar('Login fallido persistente '+str(n),comprobante.solicitar('POST','/auth/ingreso',{'tenantId':T1,'correo':'inventario@demo.example','password':'ClaveErroneaSintetica'} )[0],expected)
    comprobar('Clave correcta no evita bloqueo',comprobante.solicitar('POST','/auth/ingreso',{'tenantId':T1,'correo':'inventario@demo.example','password':PASSWORD})[0],429)
    time.sleep(31)
    comprobar('Vencimiento real de bloqueo30s',comprobante.solicitar('POST','/auth/ingreso',{'tenantId':T1,'correo':'inventario@demo.example','password':PASSWORD})[0],200)
    comprobar('Logout válido',codigo(a,'POST','/auth/salir',{}),200)
    comprobar('Cookie anterior no reabre sesión',codigo(a,'GET','/auth/sesion'),401)

if __name__=='__main__':
    started=time.time()
    try:main()
    finally:
        output=Path(os.environ.get('POS_TEST_OUTPUT','A_resultados_http.json'))
        output.write_text(json.dumps({'alcance':'Bloque A:seguridad servidor,identidad y catálogo; no QA integral B/C','duracion_segundos':round(time.time()-started,3),'casos':RESULTADOS,'total':len(RESULTADOS),'correctos':sum(r['cumple'] for r in RESULTADOS),'fallos':sum(not r['cumple'] for r in RESULTADOS),'script_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()},ensure_ascii=False,indent=2)+'\n')
    print(f'Bloque A: {len(RESULTADOS)} comprobaciones correctas; sin imprimir contraseñas, cookies ni tokens.')
