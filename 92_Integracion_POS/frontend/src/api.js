export class ErrorApi extends Error{
  constructor(status,data){super(data?.mensaje||`La solicitud no pudo completarse (${status}).`);this.status=status;this.codigo=data?.codigo;this.campos=data?.campos||{};}
}
export function crearCliente({fetchImpl=globalThis.fetch,onSesionPerdida=()=>{}}={}){
  let token='';
  async function solicitar(ruta,{method='GET',body,signal,headers={},archivo=false}={}){
    const h={Accept:archivo?'text/csv':'application/json',...headers};
    if(body!==undefined)h['Content-Type']='application/json';
    if(!['GET','HEAD'].includes(method)&&token)h['X-CSRF-Token']=token;
    const response=await fetchImpl('/api/v1'+ruta,{method,credentials:'same-origin',headers:h,body:body===undefined?undefined:JSON.stringify(body),signal});
    if(!response.ok){let error={};try{error=await response.json();}catch{}if(response.status===401)onSesionPerdida();throw new ErrorApi(response.status,error);}
    if(archivo)return{blob:await response.blob(),nombre:response.headers.get('Content-Disposition')?.match(/filename="([^"/\\]+)"/)?.[1]||'reporte.csv'};
    if(response.status===204)return null;
    return response.json();
  }
  return{
    get:(ruta,signal)=>solicitar(ruta,{signal}),
    escribir:(ruta,method,body,headers)=>solicitar(ruta,{method,body,headers}),
    async csrf(){const r=await solicitar('/auth/csrf');token=r.csrfToken;return Boolean(token);},
    async ingresar(body){await this.csrf();const r=await solicitar('/auth/ingreso',{method:'POST',body});token=r.csrfToken;return r.actor;},
    async salir(){try{return await solicitar('/auth/salir',{method:'POST'});}finally{token='';}},
    descargar:ruta=>solicitar(ruta,{archivo:true}),
    limpiar(){token='';}
  };
}
export const urlConsulta=(ruta,params={})=>ruta+'?'+new URLSearchParams(Object.entries(params).filter(([,v])=>v!==''&&v!=null)).toString();
export function cuerpoPago({medio,referencia,importe}){return{medio,referencia:String(referencia).trim(),importe:String(importe).trim()};}
export function claveCobro(ventaId,body,previa,crear=()=>crypto.randomUUID()){
  const huella=JSON.stringify([ventaId,body.medio,body.referencia,body.importe]);
  if(previa&&previa.huella!==huella)throw new Error('La confirmación pendiente tiene otro contenido. Recupere primero el comprobante de la venta.');
  return previa||{huella,clave:crear()};
}
