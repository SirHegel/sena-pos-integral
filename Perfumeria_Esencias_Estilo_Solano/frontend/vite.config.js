import { defineConfig } from "vite";
const proxy = {
  "/api": { target: "http://127.0.0.1:8095", changeOrigin: false },
};
export default defineConfig({
  base: "./",
  server: { host: "127.0.0.1", port: 8094, strictPort: true, proxy },
  preview: { host: "127.0.0.1", port: 8094, strictPort: true, proxy },
});
