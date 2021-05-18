/* This service worker manages the device-local cache */

let cacheName = 'PUSHr-v2';
let filesToCache = [
    '/', /* so browser caches index.html even if user didn't directly type in that */
    '/index.html',
    '/css/style.css',
    '/img/pushr-24.png',
    '/img/pushr-36.png',
    '/img/pushr-48.png',
    '/img/pushr-72.png',
    '/img/pushr-96.png',
    '/img/pushr-128.png',
    '/img/pushr-144.png',
    '/img/pushr-152.png',
    '/img/pushr-192.png',
    '/img/pushr-256.png',
    '/img/pushr-512.png',
    '/js/main.js'
];

/* Start the service worker and cache all of the app's content */
self.addEventListener('install', (e) => {
    console.log('[Service Worker] Install cache...')
    e.waitUntil(
        caches.open(cacheName).then((cache) => {
            return cache.addAll(filesToCache);
        })
    );
});

/* Serve cached content when offline */
self.addEventListener('fetch', (e) => {
    e.respondWith(
        caches.match(e.request).then((response) => {
            return response || fetch(e.request);
        })
    );
});

/* Delete old cache versions */
self.addEventListener('activate', (e) => {
    e.waitUntil(caches.keys().then((keyList) => {
        Promise.all(keyList.map((key) => {
            if (key === cacheName) { return;}
            caches.delete(key);
        }))
    }));
});