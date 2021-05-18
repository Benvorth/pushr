window.onload = () => {
    'use strict';

    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('./sw-cache.js');
    }

    // Permission for Notification & Push
    Notification.requestPermission().then((result) => {
        if (result === 'granted') {
            randomNotification();
        }
    });
}

function randomNotification() {
    const randomItem = Math.floor(Math.random() * 523);
    const notifTitle = "Hi There";

    new Notification(
        "Random notification: " + randomItem,
        {
            body: 'Created by Computer',
            icon: 'img/pushr-128.png'
        }
    );
    setTimeout(randomNotification, 30000);
}