// Guarda o app para abrir sem internet. As chamadas de IA sempre vão para a rede.
const CACHE = 'treino-v22';
const CACHE_EX = 'treino-exercicios'; // imagens e lista de exercícios: não é apagado nas atualizações do app
const BASE = ['./', 'index.html', 'manifest.webmanifest', 'icons/icon-192.png', 'icons/icon-512.png'];
self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(BASE)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE && k !== CACHE_EX).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  const proprio = url.origin === location.origin;
  const biblioteca = /(^|\.)(cdnjs\.cloudflare\.com|fonts\.googleapis\.com|fonts\.gstatic\.com)$/.test(url.hostname);
  const exercicios = (url.hostname === 'cdn.jsdelivr.net' && url.pathname.startsWith('/gh/yuhonas/free-exercise-db'))
    || (url.hostname === 'raw.githubusercontent.com' && url.pathname.startsWith('/yuhonas/free-exercise-db'));
  if (!proprio && !biblioteca && !exercicios) return;
  // app: rede primeiro (pega atualizações), cache se estiver sem internet
  if (proprio) {
    e.respondWith(fetch(req).then(r => { const c = r.clone(); caches.open(CACHE).then(k => k.put(req, c)); return r; })
      .catch(() => caches.match(req).then(r => r || caches.match('index.html'))));
    return;
  }
  // fontes, leitor de PDF e exercícios: cache primeiro
  const nome = exercicios ? CACHE_EX : CACHE;
  e.respondWith(caches.match(req).then(r => r || fetch(req).then(res => { if (res && (res.ok || res.type === 'opaque')) { const c = res.clone(); caches.open(nome).then(k => k.put(req, c)); } return res; })));
});
