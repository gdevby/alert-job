import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig({
  plugins: [react(), visualizer({ filename: 'bundle-visualizer.html' })],
  server: { port: 3000, strictPort: true, allowedHosts: true },
  resolve: {
    mainFields: [],
  },
  build: {
    modulePreload: false,
    rolldownOptions: {
      output: {
        entryFileNames: '[name].js',
        assetFileNames: ({ names }) => {
          if (names[0] === 'index.css') {
            return '[name][extname]';
          }
          return 'assets/[name]-[hash][extname]';
        },
      },
    },
  },
});
