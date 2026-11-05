import {defineConfig} from 'vite';
const proxy={'/api':{target:'http://127.0.0.1:8093',changeOrigin:false}};
export default defineConfig({base:'./',server:{host:'127.0.0.1',port:8092,strictPort:true,proxy},preview:{host:'127.0.0.1',port:8092,strictPort:true,proxy}});
