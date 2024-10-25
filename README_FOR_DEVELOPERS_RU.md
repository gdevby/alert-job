### Запуск приложения для разработки

Откройте терминал и склонируйте проекты

```bash
git clone https://github.com/gdevby/alert-job.git
git clone https://github.com/gdevby/alert-job-config-repo.git
```

Перейдите в папку проекта, затем в папку keycloak и запустите скрипт

```bash
cd alert-job
cd keycloak
./build.sh
```

Перейдите в корневую директорию, а затем в папку front и запустите следующие команды

```bash
cd ..
cd front
sudo apt install npm
npm i
npm run build
```

Перейдите в корневую директорию и запустите следующие команды

```bash
cd ..
docker compose pull logstash elasticsearch
docker compose up -d keycloak logstash
sudo chmod 777 public
```
Есть два способа настроить конфиг
1) В модуле config в папке resources в файле application.properties раскоментировать `#spring.cloud.config.server.native.search-locations=file:///....../alert-job-config` и указать в ней локальный путь до проекта с конфигурацией.
Должно выглядеть примерно так:
`spring.cloud.config.server.native.search-locations=/home/username/IdeaProjects/alert-job-config-repo`. Добавить к `spring.profiles.active=dev` профиль `native`. Это нужно для того, чтобы не пушить конфиг на гитхаб, а использовать локальный. 
2) Либо можно указать эти параметры в переменных окружения в IDE

Далее открываем терминал и вводим

```bash
sudo nano /etc/hosts

и добавляем
127.0.0.1       config eureka keycloak gateway notification parser core auth.alertjob.by alertjob.by logstash

```

Устанавливаем nginx

```bash
sudo apt install nginx
```

Изменяем настройки nginx

```bash
sudo nano /etc/nginx/sites-available/default
```

Добавляем следующее в настройки (не забудьте изменить в location /front/ поле root на путь к вашему проекту

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
        root /home/dima/IdeaProjects/alert-job/front/dist;
        #try_files  /$1 =404;
    }
    
    location /page {
      try_files $uri /index.html;
    }
}
```

Перезапускаем nginx и проверяем его работу

```bash
sudo systemctl restart nginx.service
sudo systemctl status nginx.service

статус должен быть active (running)

 nginx.service - A high performance web server and a reverse proxy server
     Loaded: loaded (/lib/systemd/system/nginx.service; enabled; vendor preset:>
     Active: active (running) since Fri 2024-10-25 08:04:09 +03; 2h 3min ago
```

Далее добавляем пользователя www-data в вашу группу

```bash
sudo usermod -aG $USER www-data 
```

Далее по порядку запускаем config eureka gateway parser core notification

Заходим в браузер и пишем [alertjob.by](http://alertjob.by/)