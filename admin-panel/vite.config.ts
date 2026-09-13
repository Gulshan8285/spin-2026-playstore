import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: '/spin-2026-playstore/',
  plugins: [react()],
  server: {
    port: 3000
  }
});
