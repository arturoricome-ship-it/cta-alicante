// Service Worker CTA Alicante — notificaciones en segundo plano
const DRIVE_API = 'https://www.googleapis.com/drive/v3';
const API_KEY = 'AIzaSyBK2GCTdfHt7PPhl-OBa3DTvCdZEQ5mprc';
const ROOT_ID = '1TS5vDO29JBTqd4JsGvO3a39YUIC7stbz';
const CHECK_INTERVAL = 30 * 60 * 1000; // 30 minutos

// Instalar SW
self.addEventListener('install', e => {
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(clients.claim());
  // Arrancar el ciclo de comprobación
  startChecking();
});

// Escuchar mensajes desde la app
self.addEventListener('message', e => {
  if (e.data && e.data.type === 'START_CHECK') {
    startChecking();
  }
});

let checkTimer = null;

function startChecking() {
  if (checkTimer) clearInterval(checkTimer);
  checkTimer = setInterval(checkAvisos, CHECK_INTERVAL);
  // También comprobar al arrancar
  checkAvisos();
}

async function checkAvisos() {
  try {
    // Buscar avisos.txt en Drive
    const url = `${DRIVE_API}/files?q='${ROOT_ID}'+in+parents+and+trashed=false+and+name='avisos.txt'&fields=files(id,modifiedTime)&key=${API_KEY}`;
    const res = await fetch(url);
    const data = await res.json();
    const files = data.files || [];
    if (!files.length) return;

    const modifiedTime = files[0].modifiedTime || '';

    // Leer última vez vista desde IndexedDB
    const lastSeen = await getLastSeen();

    if (lastSeen && lastSeen !== modifiedTime) {
      // Hay aviso nuevo — mandar notificación
      await self.registration.showNotification('CTA Alicante', {
        body: 'Hay un nuevo aviso de CTA Alicante',
        icon: 'data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 96 96\'%3E%3Crect width=\'96\' height=\'96\' rx=\'22\' fill=\'%231a2744\'/%3E%3Crect x=\'6\' y=\'6\' width=\'84\' height=\'84\' rx=\'18\' fill=\'%23c8a84b\'/%3E%3Ctext x=\'48\' y=\'62\' font-family=\'Arial Black\' font-size=\'26\' font-weight=\'900\' fill=\'%231a2744\' text-anchor=\'middle\'%3ECTA%3C/text%3E%3C/svg%3E',
        badge: 'data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 96 96\'%3E%3Crect width=\'96\' height=\'96\' rx=\'22\' fill=\'%23c8a84b\'/%3E%3Ctext x=\'48\' y=\'62\' font-family=\'Arial Black\' font-size=\'26\' font-weight=\'900\' fill=\'%231a2744\' text-anchor=\'middle\'%3ECTA%3C/text%3E%3C/svg%3E',
        tag: 'cta-aviso',
        renotify: true,
        vibrate: [200, 100, 200]
      });
    }

    // Guardar el modifiedTime actual
    await setLastSeen(modifiedTime);

  } catch(e) {
    console.log('[SW] Error comprobando avisos:', e.message);
  }
}

// Al pulsar la notificación, abrir la app
self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
      for (const client of list) {
        if (client.url.includes('cta-alicante.es') && 'focus' in client) {
          return client.focus();
        }
      }
      return clients.openWindow('https://cta-alicante.es');
    })
  );
});

// IndexedDB helpers
function openDB() {
  return new Promise((res, rej) => {
    const req = indexedDB.open('cta-sw', 1);
    req.onupgradeneeded = e => e.target.result.createObjectStore('kv');
    req.onsuccess = e => res(e.target.result);
    req.onerror = () => rej(req.error);
  });
}

async function getLastSeen() {
  const db = await openDB();
  return new Promise((res, rej) => {
    const tx = db.transaction('kv', 'readonly');
    const req = tx.objectStore('kv').get('avisos_seen');
    req.onsuccess = () => res(req.result || null);
    req.onerror = () => res(null);
  });
}

async function setLastSeen(val) {
  const db = await openDB();
  return new Promise((res, rej) => {
    const tx = db.transaction('kv', 'readwrite');
    tx.objectStore('kv').put(val, 'avisos_seen');
    tx.oncomplete = () => res();
    tx.onerror = () => rej(tx.error);
  });
}
