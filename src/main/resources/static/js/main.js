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
    debugger;
    const randomItem = Math.floor(Math.random() * 523);
    new Notification(
        "Random notification: " + randomItem,
        {
            body: 'Created by Computer',
            icon: 'img/pushr-24.png'
        }
    );
    setTimeout(randomNotification, 30000);
}