import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools';

// https://vite.dev/config/
export default defineConfig({
  server: {
    port: 8020,
  },
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    lib: {
      entry: 'src/main.ts',
      name: 'CitronSpamGadget',
      formats: ['iife'],
    },
    rollupOptions: {
      external: ['vue', '@wikimedia/codex'],
      output: {
        format: 'iife',
        globals: {
          vue: 'Vue',
          '@wikimedia/codex': 'Codex',
        },
      },
    },
  },
});
