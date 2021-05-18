# pushr
Rreceive a push-message on your mobile phone by calling a REST-API. Use this eg together with some smart IoT-buttons.

Requirements

* Java 1.8+

Infrastructure

* VM for Backend
* and Frontend
* Running Jenkins for CI/CD
* 

Backend

* Host for frontend (`/src/resources/static/*`)
* Push Notification Server
(https://golb.hplar.ch/2019/08/webpush-java.html)

Frontend

* Managed Cache, incl. cleanup of old versions
* Notifications
* Push Messages
* 



License

* Backend: pushService: MIT (thanks @ralscha, https://github.com/ralscha/blog2019/tree/master/webpush, modified to run with Java 1.8+ instead of 11+)