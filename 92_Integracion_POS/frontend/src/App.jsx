import React,{useEffect,useMemo,useRef,useState}from'react';
import{crearCliente}from'./api.js';import{Contexto}from'./contexto.jsx';import{RUTAS,puede}from'./utilidades.js';
import{Acceso,Inicio,Usuarios,Roles}from'./vistas/identidad.jsx';
import{Productos,Existencias,Movimientos}from'./vistas/inventario.jsx';
import{NuevaVenta,Pago,Comprobante,Mesas,Caja,Recetas}from'./vistas/operacion.jsx';
import{Empleados,Novedades,Liquidacion,Cuentas,Asientos,Reportes}from'./vistas/administracion.jsx';
const VISTAS={inicio:Inicio,usuarios:Usuarios,roles:Roles,productos:Productos,existencias:Existencias,movimientos:Movimientos,'nueva-venta':NuevaVenta,pago:Pago,comprobante:Comprobante,mesas:Mesas,caja:Caja,recetas:Recetas,empleados:Empleados,novedades:Novedades,liquidacion:Liquidacion,cuentas:Cuentas,asientos:Asientos,reportes:Reportes};
function leer(){const[r,q='']=location.hash.slice(1).split('?');return{ruta:r||'inicio',venta:new URLSearchParams(q).get('venta')||''};}
export default function App(){
 const[actor,setActor]=useState(undefined),[revision,setRevision]=useState(0),[ruta,setRuta]=useState(leer().ruta),[ventaId,setVentaId]=useState(leer().venta),[feedback,setFeedback]=useState(null),[menu,setMenu]=useState(false);
 const main=useRef(null),mensaje=useRef(null);
 const api=useMemo(()=>crearCliente({onSesionPerdida:()=>{setActor(null);setVentaId('');setRevision(r=>r+1);}}),[]);
 useEffect(()=>{let ok=true;api.csrf().then(()=>api.get('/auth/sesion')).then(a=>{if(ok)setActor(a);}).catch(e=>{if(ok){setActor(null);if(e.status!==401)setFeedback({error:true,mensaje:e.message});}});return()=>{ok=false;};},[api]);
 useEffect(()=>{const change=()=>{const x=leer();setRuta(x.ruta);setVentaId(x.venta);setMenu(false);setFeedback(null);};window.addEventListener('hashchange',change);return()=>window.removeEventListener('hashchange',change);},[]);
 useEffect(()=>{main.current?.focus();if(actor)api.get('/auth/sesion').then(setActor).catch(()=>{});},[ruta,api]);
 useEffect(()=>{if(feedback)mensaje.current?.focus();},[feedback]);
 const abrirVenta=(id,vista='nueva-venta')=>{setVentaId(id);location.hash=vista+'?venta='+encodeURIComponent(id);};
 const ejecutar=async(fn,texto='Operación guardada.')=>{setFeedback(null);try{const r=await fn();setFeedback({error:false,mensaje:texto});setRevision(x=>x+1);return r;}catch(e){setFeedback({error:true,mensaje:e.message+(e.codigo?' · '+e.codigo:'')});if(e.status===403)api.get('/auth/sesion').then(setActor).catch(()=>{});setRevision(x=>x+1);throw e;}};
 const ctx={api,actor,revision,ejecutar,ventaId,abrirVenta,refrescar:()=>setRevision(x=>x+1),setActor};
 const selected=RUTAS.find(r=>r[0]===ruta)||RUTAS[0],View=VISTAS[selected[0]],allowed=puede(actor,selected[3]);
 const notice=feedback&&<div ref={mensaje}tabIndex="-1"className={'message'+(feedback.error?' error':'')}role={feedback.error?'alert':'status'}>{feedback.mensaje}</div>;
 if(actor===undefined)return<main className="login"><p role="status">Recuperando sesión…</p></main>;
 return<Contexto.Provider value={ctx}><a className="skip"href="#contenido"onClick={e=>{e.preventDefault();main.current?.focus();}}>Saltar al contenido</a>{!actor?<div className="login"><div className="brand">POS <span>Integral</span></div><main id="contenido"tabIndex="-1"ref={main}>{notice}<Acceso/></main><p className="footer">Entorno académico · Datos de prueba</p></div>:<><header><div className="brand">POS <span>Integral</span><small className="muted">Negocio {actor.tenantId.slice(-4)} · {actor.roles.join(', ')}</small></div><div className="actions"><button className="secondary menu"aria-expanded={menu}aria-controls="navegacion"onClick={()=>setMenu(!menu)}>Menú</button><button className="secondary"onClick={()=>ejecutar(async()=>{await api.salir();api.limpiar();setActor(null);setVentaId('');location.hash='acceso';},'Sesión cerrada.').catch(()=>{})}>Cerrar sesión</button></div></header><div className="shell"><aside id="navegacion"className={'sidebar'+(menu?' open':'')}><nav aria-label="Navegación principal">{[...new Set(RUTAS.map(r=>r[2]))].map(area=><React.Fragment key={area}><strong>{area}</strong>{RUTAS.filter(r=>r[2]===area&&puede(actor,r[3])).map(r=><a key={r[0]}href={'#'+r[0]+(ventaId&&['nueva-venta','pago','comprobante'].includes(r[0])?'?venta='+ventaId:'')}aria-current={selected[0]===r[0]?'page':undefined}>{r[1]}</a>)}</React.Fragment>)}</nav></aside><main id="contenido"ref={main}tabIndex="-1"><p className="breadcrumb">Inicio / {selected[2]}</p><h1>{selected[1]}</h1>{notice}{allowed?<View key={selected[0]+'|'+actor.tenantId+'|'+actor.usuarioId}/>:<p role="alert"className="message error">Su rol no tiene acceso a esta sección.</p>}<p className="footer">Entorno académico · Operaciones con datos de prueba</p></main></div></>}</Contexto.Provider>;
}
