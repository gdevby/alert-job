import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import { join } from 'node:path';

export default defineConfig(() => {
  return {
    plugins: [react(), visualizer({ filename: 'bundle-visualizer.html' })],
    server: { port: 3001, strictPort: true, allowedHosts: true },
    preview: {
      port: 3001,
    },
    resolve: {
      mainFields: [],
      alias: {
        axios: join(__dirname, 'node_modules', 'axios', 'dist', 'esm', 'axios.js'),
      },
    },
    build: {
      /* for vite preview need comment outDir property */
      outDir: 'dist/front',
      modulePreload: false,
      rolldownOptions: {
        output: {
          entryFileNames: `[name].js`,
          assetFileNames: ({ names }) => {
            if (names[0] === 'index.css') {
              return `[name][extname]`;
            }
            return 'assets/[name]-[hash][extname]';
          },
        },
      },
    },
  };
});
