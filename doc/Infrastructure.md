


install java 8:

* sudo apt install openjdk-8-jdk
* sudo update-alternatives --config java
* sudo update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/bin/java


install git:

* sudo apt-get install git-all

install maven:

* sudo apt update
* sudo apt install maven
* mvn -version

install Jenkins:

* wget --no-check-certificate -vO - https://pkg.jenkins.io/debian-stable/jenkins.io.key |sudo apt-key add -
* sudo sh -c 'echo deb https://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
* sudo apt-get update
* sudo apt-get install jenkins -y
* sudo service jenkins restart
* service jenkins status
* Get admin-password:
  sudo cat /var/lib/jenkins/secrets/initialAdminPassword
* Go to http://<ip>:8080

* Default jenkins home-dir: /var/lib/jenkins

