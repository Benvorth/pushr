

install node

* ...
* npm install -g pnpm


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

install mariaDB

* sudo apt update
* sudo apt install mariadb-server
* mariadb will start automatically: 
  sudo systemctl status mariadb
* sudo mysql_secure_installation
  (Set root password, Remove anonymous users, Disallow root login remotely, Remove test database and access to it, Reload privilege tables)
* re-set root password:
  >mysql -u root -p
  >> update mysql.user set password=password('<rootPW>') where user='root';
  >> flush privileges;
  >> exit  
  
* CREATE DATABASE pushr;
* CREATE USER 'pushr'@'localhost' IDENTIFIED BY '<Password>';
* GRANT ALL PRIVILEGES ON pushr.* TO 'pushr'@localhost;


* sudo service mysql restart

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
* Pipeline-home: /var/lib/jenkins/workspace/<pipeline-name>/

* Configure "Automatic Mode" for GitHub hook trigger for GITScm polling (https://plugins.jenkins.io/github/):
Dashboard -> Configure Jenkins -> Configure System -> Scroll down to "GitHub", Add gitHub Server, User personal Access Token from GitHub (self-service there), create credentials as «Secret Text», -> check "Manage hooks"
* In your pipeline configure "Build Triggers" as "GitHub hook trigger for GITScm polling"
* As "Pipeline" select "Pipeline Scripte from SCM". Make sure to check out "*/main" (not "*/master"), Script path is "Jenkinsfile".


install nginx




https for jenkins:
https://www.digitalocean.com/community/tutorials/how-to-configure-nginx-with-ssl-as-a-reverse-proxy-for-jenkins



config for several domains pointing to one server hosting different tomcat instances on different ports:

/etc/nginx/site-enabled/default

```
# redirect from http to https
server {
    listen 80;
    server_name monte.chat;
    return 301 https://$host$request_uri;
}

server {
    listen 80;
    server_name pushr.info;
    return 301 https://$host$request_uri;
}

# HTTPS Servers

# https://coolDomainOne.com -> http://localhost:3000
server {
    listen 443;
    server_name coolDomainOne.com;

    # You can increase the limit if your need to.
    client_max_body_size 200M;

    error_log /var/log/nginx/coolDomainOne.com.access.log;

    ssl on;
    ssl_certificate /etc/nginx/coolDomainOne.com_certificate.crt; # dont forget to append intermediate certs here, too
    ssl_certificate_key /etc/nginx/coolDomainOne.com_certificate.key;
    ssl_protocols TLSv1 TLSv1.1 TLSv1.2; # don’t use SSLv3 ref: POODLE

    location / {
       
        proxy_set_header        Host $host;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header        X-Forwarded-Proto $scheme;

        # Fix the “It appears that your reverse proxy set up is broken" error.
        proxy_pass          http://localhost:3000;
        proxy_read_timeout  90;
        
        proxy_redirect http://localhost:3000 https://coolDomainOne.com;
    }
}

# https://anotherCoolDomain.org -> https://localhost:8081
server {
    listen 443;
    server_name anotherCoolDomain.org;

    # You can increase the limit if your need to.
    client_max_body_size 200M;

    error_log /var/log/nginx/anotherCoolDomain.org.access.log;

    ssl on;
    ssl_certificate /etc/nginx/anotherCoolDomain.org_certificate.crt; # dont forget to append intermediate certs here, too
    ssl_certificate_key /etc/nginx/anotherCoolDomain.org_certificate.key;
    ssl_protocols TLSv1 TLSv1.1 TLSv1.2; # don’t use SSLv3 ref: POODLE

    location / {        
        proxy_set_header        Host $host;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header        X-Forwarded-Proto $scheme;

        # Fix the “It appears that your reverse proxy set up is broken" error.
        proxy_pass          http://localhost:8081;
        proxy_read_timeout  90;
        
        proxy_redirect http://localhost:8081 https://anotherCoolDomain.org;
    }
}

```
