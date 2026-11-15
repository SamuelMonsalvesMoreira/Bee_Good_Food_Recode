// Build usada pela Vercel (frontend/vercel.json).
// Padrao: demonstracao com a API simulada no navegador (DEC-037).
// Com BEE_API_REAL=true no projeto da Vercel: interface ligada a API real, via /api.
import { execSync } from 'node:child_process';

const real = process.env.BEE_API_REAL === 'true';
const comando = real ? 'npm run build' : 'npm run build:demo';
console.log(`Build da Vercel: ${real ? 'API real' : 'demonstracao'} (${comando})`);
execSync(comando, { stdio: 'inherit' });
