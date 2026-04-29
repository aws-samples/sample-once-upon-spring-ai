import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Bind to all interfaces so reverse proxies (e.g. XaresAICoder,
    // Gitpod, Coder, Codespaces) can forward port 5173 from outside
    // the container. Default 127.0.0.1 is unreachable to external proxies.
    host: "0.0.0.0",
    // Explicit port so proxies can template <session>-5173.<host>.
    port: 5173,
    // Vite blocks requests whose Host header doesn't match an allowed
    // host when exposed on 0.0.0.0 (Vite 5+). ".localhost" allows any
    // subdomain pattern used by cloud-IDE reverse proxies.
    allowedHosts: [".localhost"],
    proxy: {
      "/inquire": "http://localhost:8009",
      "/user": "http://localhost:8009",
      "/health": "http://localhost:8009",
    },
  },
});
