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
    // Accept any Host header. This is a dev server running in an ephemeral
    // workshop container with no sensitive data — DNS rebinding protection
    // is not a concern here. Hardcoding a host list does not scale: every
    // venue (XaresAICoder, duckdns, gitpod.io, github.dev, custom subdomains)
    // produces a different host, so an allowlist would need editing per
    // workshop. `true` is the documented escape hatch for this exact case.
    allowedHosts: true,
    proxy: {
      "/inquire": "http://localhost:8009",
      "/user": "http://localhost:8009",
      "/health": "http://localhost:8009",
    },
  },
});
