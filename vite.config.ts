import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/inquire": "http://localhost:8009",
      "/user": "http://localhost:8009",
      "/health": "http://localhost:8009",
    },
  },
});
