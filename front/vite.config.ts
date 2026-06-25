import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import { join } from 'node:path';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  const REVISION = env.VITE_REVISION;

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
          entryFileNames: `[name]-v${REVISION}.js`,
          assetFileNames: ({ names }) => {
            if (names[0] === 'index.css') {
              return `[name]-v${REVISION}[extname]`;
            }
            return 'assets/[name]-[hash][extname]';
          },
        },
      },
    },
  };
});
