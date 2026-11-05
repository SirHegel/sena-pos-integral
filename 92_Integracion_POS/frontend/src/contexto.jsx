import React,{createContext,useContext,useEffect,useState,useRef}from'react';
export const Contexto=createContext(null);
export const usePos=()=>useContext(Contexto);
export function useConsulta(rutas){
 const {api,actor,revision}=usePos(),key=JSON.stringify(rutas),identity=actor?actor.tenantId+'/'+actor.usuarioId+'/'+actor.roles.join(','):'';
 const [state,setState]=useState({data:[],cargando:true,error:null});const anterior=useRef('');
 useEffect(()=>{if(!identity)return;const controller=new AbortController();let activo=true;const consulta=identity+'|'+key,mismo=anterior.current===consulta;anterior.current=consulta;setState(s=>{const preservar=mismo&&s.data.length>0;return{data:preservar?s.data:[],cargando:!preservar,error:null};});
 Promise.all(JSON.parse(key).map(r=>r?api.get(r,controller.signal):Promise.resolve(null))).then(data=>{if(activo)setState({data,cargando:false,error:null});}).catch(error=>{if(activo&&error.name!=='AbortError')setState({data:[],cargando:false,error});});
 return()=>{activo=false;controller.abort();};},[api,key,identity,revision]);
 return state;
}
