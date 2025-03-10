## Запуск приложения для разработки

### 1. Клонирование проектов

Откройте коммандную строку (cmd) и склонируйте проекты

```bash
git clone https://github.com/gdevby/alert-job.git
git clone https://github.com/gdevby/alert-job-config-repo.git
```

### 2. Установка параметра Windows Execution Policy

Откройте PowerShell с правами администратора и выполните
```bash
Get-ExecutionPolicy
```
<b> Запомните значение, возвращенное этой командой. Следующей командой оно будет изменено и лучше вернуть его на прежнее значение после выполнения этого руководства. </b>

Выполните следующую команду
```bash
Set-ExecutionPolicy -ExecutionPolicy Remotesigned
```
Передайте параметр А (Да для всех). <b> Верните значение этой настройки на прежнее используя команду выше, заменив параметр Remotesigned на ваше прежнее значение после выполнения этого руководства. Напоминание об этом будет в самом конце руководства.</b>

### 3. Keycloak

Перейдите в папку keycloak внутри папки проекта и выполните скрипт
```bash
cd (path_to_project)\alert-job\keycloak
build.sh
```
### 4. Установка Node
Перейдите по ссылке https://github.com/coreybutler/nvm-windows/releases и скачайте установщик nvm.

Установите nvm без изменения параметров во время установки.

Откройте PowerShell с правами администратора и выполните
```bash
nvm ls
```
"No installations recognized" должно быть возвращено, если вы не устанавливали nvm ранее.

Тогда запустите команду для установки npm
```bash
nvm install lts
```
Проверьте версию npm и результат установки запуском еще раз следующей команды. Должна быть возвращена версия вашей установки
 ```bash
nvm ls
```
Перейдите в папку проекта и перейдите в папку front (замените PATH_TO_PROJECT вашим путем). Замените YOUR_VERION_NUMBER версией npm, которая была возвращена предыдущей командой. Запустите сборку npm.
```bash
cd PATH_TO_PROJECT\alert-job\front
nvm use YOUR_VERION_NUMBER
npm run build
```

### 5. Docker 
Перейдите в родительскую директорию, создайте образы и запустите контейнеры
```bash
cd ..
docker compose pull logstash elasticsearch
docker compose up -d keycloak logstash
```

### 6. Config repo

Существует два способа настроить конфиг:

1. Внутри сервиса alert-job-config найдите файл application.properties в папке с ресурсами и раскомментируйте следующее
```bash
#spring.cloud.config.server.native.search-locations=file:///....../alert-job-config-repo 
```
определите локальный путь к проекту конфигураций. Должно получиться примерно следующее
```bash
spring.cloud.config.server.native.search-locations=file:///e:/Programming/Java EE/alert-job-config-repo/
```
<b> обратите внимание на тип слешей (здесь "/"), использованные в путях во всех файлах конфигурации. </b>

Добавьте профиль native в том же файле в строку
```bash
spring.profiles.active=dev,native
```
Это нужно, чтобы использовать локальную конфигурацию

2. Вы можете добавить переменные окружения в Running configuration вашей IDE 

### 7. Hosts

Перейдите по пути и откройте файл для редактирования
```bash
c:\Windows\System32\drivers\etc\hosts
```

Добавьте следующие строки в конец файла
```bash
127.0.0.1 config eureka keycloak gateway notification parser core 
127.0.0.1 auth.alertjob.by alertjob.by logstash
```

### 8. Nginx

Откройте ссылку https://nginx.org/en/download.html и скачайте Stable версию Nginx. Распакуйте в удобное место. Откройте эту папку и перейдите к файлу конфигурации.
```bash
(your_path)\nginx-(version)\conf\nginx.conf
```
Здесь вам нужно заменить конфигурацию server следующим
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
        root YOUR_PATH
        #try_files  /$1 =404;
    }
    
    location /page {
      try_files $uri /index.html;
    }
}
```
Замените YOUR_PATH путем к папке front/dist проекта. Должно выглядеть примерно так
```bash 
root e:/Programming/Java_EE/alert-job/front/dist; 
```
<b> Пробелов не должно быть в строке пути и следите за использованным типом слешей (тут"/"). </b> Сохраните изменения и закройте файл. Откройте командную строку в папке nginx и выполните следующее
```bash 
cd (your_path)\nginx-(version)
nginx -t
```
"nginx.conf syntax is ok" и "test is successful" сообщения должны быть возвращены. 

Далее, запустите файл nginx.exe. Скорее всего, вам потребуется исполнять этот файл для запуска проекта после каждой перезагрузки компьютера.

### 9. Запуск проекта
Запустите сервисы проекта используя вашу IDE в следующем порядке:
1. config 
2. eureka 
3. gateway 
4. parser 
5. core 
6. notification 

После этого, проект доступен по [alertjob.by](http://alertjob.by/). Если все в порядке, тогда вы можете вернуться к шагу №2 и изменить ExecutionPolicy к вашему прежнему значению.