import { spawn } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('\x1b[36m%s\x1b[0m', '🚀 Starting Nyaya Legal AI - Combined Development Environment...');

// Start Express Backend on port 5000
const backend = spawn('node', ['server.js'], {
  stdio: 'inherit',
  shell: true,
  cwd: __dirname
});

// Start Vite Frontend on port 5173
const frontend = spawn('npx', ['vite'], {
  stdio: 'inherit',
  shell: true,
  cwd: __dirname
});

// Handle termination signals
const cleanup = () => {
  console.log('\x1b[33m%s\x1b[0m', '\nShutting down servers...');
  backend.kill('SIGTERM');
  frontend.kill('SIGTERM');
  process.exit(0);
};

process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);

backend.on('close', (code) => {
  console.log(`Backend process exited with code ${code}`);
  frontend.kill('SIGTERM');
  process.exit(code || 0);
});

frontend.on('close', (code) => {
  console.log(`Frontend process exited with code ${code}`);
  backend.kill('SIGTERM');
  process.exit(code || 0);
});
