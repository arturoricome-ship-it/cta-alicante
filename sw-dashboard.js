const CACHE = 'dashboard-nominas-v1';
self.addEventListener('install', function(e){
  self.skipWaiting();
});
self.addEventListener('activate', function(e){
  e.waitUntil(clients.claim());
});
self.addEventListener('fetch', function(e){
  // Solo cachear el propio HTML del dashboard
  if(e.request.url.includes('dashboard-nominas.html')){
    e.respondWith(
      fetch(e.request).catch(function(){
        return caches.match(e.request);
      })
    );
  }
});
