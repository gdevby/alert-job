### Запуск приложения для разработки

Open terminal and clone the projects

```bash
git clone https://github.com/gdevby/alert-job.git
git clone https://github.com/gdevby/alert-job-config-repo.git
```

Enter project folder and after that enter keycloak folder
```bash
cd alert-job
cd keycloak
./build.sh
```

Go to parent directory and after that enter front folder, and run next commands
```bash
cd ..
cd front
sudo apt install npm
npm i
npm run build
```

Go to parent directory, create images and run containers 
```bash
cd ..
docker compose pull logstash elasticsearch
docker compose up -d keycloak logstash
sudo chmod 777 public
```

There are two ways wo setup config:
1) In config module, in resource folder in `application.properties` file uncomment `#spring.cloud.config.server.native.search-locations=file:///....../alert-job-config` specify the local path to the config project.
Should look like this:
`spring.cloud.config.server.native.search-locations=/home/username/IdeaProjects/alert-job-config-repo`. 
Add `native` profile to the `spring.profiles.active=dev`. This is needed to use local configuration
2) You may add Environment Variables in Running configuration in your IDE.

Then open terminal and alter hosts file
```bash
sudo nano /etc/hosts

add next line
127.0.0.1       config eureka keycloak gateway notification parser core auth.alertjob.by alertjob.by logstash

```

Downloading nginx
```bash
sudo apt install nginx
```

Configure nginx config
```bash
sudo nano /etc/nginx/sites-available/default
```

Then adding next configuration (do no forget to change in location /front/ field root on your local path)
```bash
server{
    listen 80;
    server_name aj.by alertjob.by;
    
    location / {
        proxy_pass http://127.0.0.1:8015;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /keycloak {
        rewrite ^/keycloak/(.*) /$1 break;
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /front/ {
        rewrite ^/front/(.*) /$1 break;
        root /home/username/IdeaProjects/alert-job/front/dist;
        #try_files  /$1 =404;
    }
    
    location /page {
      try_files $uri /index.html;
    }
}
```

Restart nginx and check if it's working
```bash
sudo systemctl restart nginx.service
sudo systemctl status nginx.service

status should be active (running)

 nginx.service - A high performance web server and a reverse proxy server
     Loaded: loaded (/lib/systemd/system/nginx.service; enabled; vendor preset:>
     Active: active (running) since Fri 2024-10-25 08:04:09 +03; 2h 3min ago
```

Then adding user www-data to yours group
```bash
sudo usermod -aG $USER www-data 
```

In next order run next services config eureka gateway parser core notification

After that project available on [alertjob.by](http://alertjob.by/)