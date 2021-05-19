
let subscribeButton;
let unsubscribeButton;

let subscribeStatusLog;
let factOutput;
let jokeOutput;

window.onload = () => {
    'use strict';

    // cache manager
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('./sw-cache.js');
    }

    // push
    subscribeStatusLog = document.getElementById('subscribeStatusLog');
    subscribeButton = document.getElementById('subscribeButton');
    unsubscribeButton = document.getElementById('unsubscribeButton');

    factOutput = document.getElementById('fact');
    jokeOutput = document.getElementById('joke');

    if ('serviceWorker' in navigator) {
        try {
            checkSubscription();
            init();
        } catch (e) {
            console.error('error init(): ' + e);
        }

        subscribeButton.addEventListener('click', () => {

            Notification.requestPermission().then((result) => {
                debugger;
                if (result === 'granted') {
                    subscribe().catch(e => {console.error('error subscribe(): ' + e);});
                } else {
                    console.warn('Permission for notifications was denied');
                }
            });

            /*
            subscribe().catch(e => {
                if (Notification.permission === 'denied') {
                    console.warn('Permission for notifications was denied');
                } else {
                    console.error('error subscribe(): ' + e);
                }
            });
            */

        });

        unsubscribeButton.addEventListener('click', () => {
            unsubscribe().catch(e => console.error('error unsubscribe(): ' + e));
        });
    }

    // notifications
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