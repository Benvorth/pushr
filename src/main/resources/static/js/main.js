window.onload = () => {
    'use strict';

    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('./sw-cache.js');
    }

    const notificationsButton = document.getElementById("notifications");
    notificationsButton.addEventListener('click', () => {
        // Permission for Notification & Push
        Notification.requestPermission().then((result) => {
            if (result !== 'denied') {
                randomNotification();
            }
        });
    });


}

function randomNotification() {
    const randomItem = Math.floor(Math.random() * 523);

    const options = {
        body: 'Created by Computer',
        icon: 'img/pushr-24.png'
    };

    new Notification(
        "Random notification: " + randomItem,
        options
    );
    setTimeout(randomNotification, 30000);
}