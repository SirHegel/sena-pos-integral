from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import urllib.request,urllib.error,json,time,uuid,os,subprocess,datetime

OUT=Path(__file__).resolve().parent/'salidas';OUT.mkdir(exist_ok=True)
URL=os.environ.get('API_BASE_URL','http://127.0.0.1:8088/api/v1/auth/')
A='11111111-1111-4111-8111-111111111111'
B='22222222-2222-4222-8222-222222222222'
INACTIVE='33333333-3333-4333-8333-333333333333'
email='caso'+uuid.uuid4().hex[:10]+'@example.test'
results=[]

def request(endpoint,payload,raw=False):
 data=payload.encode() if raw else json.dumps(payload).encode()
 req=urllib.request.Request(URL+endpoint,data=data,headers={'Content-Type':'application/json'})
 started=time.monotonic()
 try:
  with urllib.request.urlopen(req,timeout=15) as response:status=response.status;body=response.read()
 except urllib.error.HTTPError as e:status=e.code;body=e.read()
 try:decoded=json.loads(body)
 except Exception:decoded=body.decode(errors='replace')
 return {'status':status,'respuesta':decoded,'segundos':round(time.monotonic()-started,4)}

def case(name,endpoint,payload,expected,code=None,raw=False):
 r=request(endpoint,payload,raw)
 ok=r['status']==expected and (not code or r['respuesta'].get('codigo')==code)
 results.append({'caso':name,'endpoint':endpoint,'esperado':expected,'correcto':ok,**r})
 print(name,r['status'],'OK' if ok else 'FALLO',flush=True)
 return r

def credentials(tenant=A,user=email,password='ClaveDemo88!'):
 return {'tenantId':tenant,'usuario':user,'password':password}

case('Registro válido con correo normalizado','registro',credentials(user='  '+email.upper()+'  '),201)
case('Ingreso válido','ingreso',credentials(),200)
case('Duplicado dentro del negocio','registro',credentials(),409,'USUARIO_EXISTENTE')
case('Mismo correo en otro negocio','registro',credentials(B,password='ClaveNegocioB!'),201)
case('Contraseña de otro negocio rechazada','ingreso',credentials(B),401,'AUTENTICACION_FALLIDA')
case('Ingreso correcto del segundo negocio','ingreso',credentials(B,password='ClaveNegocioB!'),200)
case('Negocio inexistente','registro',credentials(str(uuid.uuid4())),400,'NEGOCIO_NO_DISPONIBLE')
case('Negocio inactivo','registro',credentials(INACTIVE),400,'NEGOCIO_NO_DISPONIBLE')
case('Usuario inexistente','ingreso',credentials(user='noexiste'+uuid.uuid4().hex+'@example.test'),401,'AUTENTICACION_FALLIDA')
case('UUID inválido','registro',credentials('123'),400,'DATOS_INVALIDOS')
case('Correo inválido','registro',credentials(user='correo-sin-arroba'),400,'DATOS_INVALIDOS')
case('Puntos repetidos en correo','registro',credentials(user='a..b@example.test'),400,'DATOS_INVALIDOS')
case('Correo con inyección no válido','ingreso',credentials(user="x' OR 1=1 --@example.test"),400,'DATOS_INVALIDOS')
case('Contraseña demasiado corta','registro',credentials(password='abc'),400,'DATOS_INVALIDOS')
case('Contraseña demasiado larga','registro',credentials(password='a'*65),400,'DATOS_INVALIDOS')
case('Contraseña sólo espacios','registro',credentials(password=' '*8),400,'DATOS_INVALIDOS')
case('Más de72bytes UTF8','registro',credentials(password='é'*37),400,'DATOS_INVALIDOS')
case('Control en contraseña','registro',credentials(password='Clave\nDemo88'),400,'DATOS_INVALIDOS')
case('Campo obligatorio ausente','registro',{'tenantId':A,'usuario':email},400,'DATOS_INVALIDOS')
case('JSON mal formado','registro','{"tenantId":',400,'JSON_INVALIDO',True)
case('Campos desconocidos','registro',{**credentials(),'rol':'ADMIN'},400,'JSON_INVALIDO')
case('Primer fallo','ingreso',credentials(password='Incorrecta88!'),401,'AUTENTICACION_FALLIDA')
case('Segundo fallo','ingreso',credentials(password='Incorrecta88!'),401,'AUTENTICACION_FALLIDA')
locked=case('Tercer fallo activa bloqueo','ingreso',credentials(password='Incorrecta88!'),429,'BLOQUEO_TEMPORAL')
lock_start=time.monotonic()
case('Clave correcta no evita bloqueo','ingreso',credentials(),429,'BLOQUEO_TEMPORAL')

# Carrera real de registro: la restricción UNIQUE debe dejar exactamente una cuenta.
race_user='carrera'+uuid.uuid4().hex[:10]+'@example.test'
with ThreadPoolExecutor(max_workers=2) as executor:
 race=list(executor.map(lambda _:request('registro',credentials(user=race_user)),range(2)))
race_ok=sorted(r['status'] for r in race)==[201,409]
results.append({'caso':'Dos registros concurrentes del mismo correo','correcto':race_ok,'esperado':[201,409],'resultados':race})
print('Concurrencia registro','OK' if race_ok else 'FALLO',flush=True)

# Se espera el vencimiento real del bloqueo: no se cambia la hora ni el dato guardado.
remaining=max(0,31-(time.monotonic()-lock_start))
print('Esperando vencimiento real del bloqueo',round(remaining,1),'segundos',flush=True)
time.sleep(remaining)
case('Ingreso después del vencimiento real','ingreso',credentials(),200)

report={'fecha_UTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'base':'sena_pos_ga7_auth','url':URL,'datos':'Cuentas y negocios sintéticos; ninguna cuenta del aula','registro_correo':email,'alcance':'Pruebas HTTP reales del caso88; no Postman ni backend completo','resultados':results,'total':len(results),'correctos':sum(r['correcto'] for r in results)}
(OUT/'resultados_http.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print('TOTAL',report['correctos'],'/',report['total'])
assert report['correctos']==report['total'], 'Hay casos fallidos; revisar resultados'
