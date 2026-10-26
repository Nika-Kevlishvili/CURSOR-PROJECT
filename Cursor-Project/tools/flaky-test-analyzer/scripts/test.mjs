import { spawn } from 'node:child_process';

const existing = process.env.NODE_OPTIONS ?? '';
const nodeOptions = `${existing} --experimental-sqlite`.trim();
const child = spawn(process.execPath, ['node_modules/vitest/vitest.mjs', 'run'], {
  stdio: 'inherit',
  env: { ...process.env, NODE_OPTIONS: nodeOptions },
  shell: false,
});

child.on('exit', (code) => {
  process.exit(code ?? 1);
});
