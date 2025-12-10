## Launching app for development

### 1. Clone the projects

Open command line (cmd) and clone the projects

```bash
git clone https://github.com/gdevby/alert-job.git
git clone https://github.com/gdevby/alert-job-config-repo.git
```

### 2. Set Windows Execution Policy

Open PowerShell with administrator access and execute
```bash
Get-ExecutionPolicy
```
<b> Remember the value returned by this command. By next command it will be changed and better to return that value to default after completing this tutorial.</b>

Execute next command.
```bash
Set-ExecutionPolicy -ExecutionPolicy Remotesigned
```
Pass A parameter (Yes to everyone). <b> Return that setting to your default by using upper command with replaced Remotesigned to your default parameter after completing tutorial. You will be reminded about it at the very end of tutorial.</b>

### 3. Keycloak

Enter to keycloak folder inside of project folder and execute script
```bash
cd (path_to_project)\alert-job\keycloak
build.sh
```
### 4. Node installing
Go to link https://github.com/coreybutler/nvm-windows/releases and download nvm setup.

Install the nvm with no changes at properties through installing.

Open PowerShell with administrator access and execute
```bash
nvm ls
```
"No installations recognized" string must have been present if you do not install it before.

Then execute for install npm
```bash
nvm install lts
```
Check the npm version and installation result by executing again next line. Your version of installation must be present.
 ```bash
nvm ls
```
Go to your project folder and enter to the front folder (replace PATH_TO_PROJECT with your value). Replace YOUR_VERION_NUMBER with version of npm from previous command. Then execute npm building.
```bash
cd PATH_TO_PROJECT\alert-job\front
nvm use YOUR_VERION_NUMBER
npm run build
```

### 5. Docker 
Go to parent directory, create images and run containers 
```bash
cd ..
docker compose up -d keycloak
```

### 6. Config repo
There are two ways wo setup config:

1. Inside of alert-job-config service find application.properties at resource folder and uncomment next
```bash
#spring.cloud.config.server.native.search-locations=file:///....../alert-job-config-repo 
```
specify the local path to the config project. Should look like next lines
```bash
spring.cloud.config.server.native.search-locations=file:///e:/Programming/Java_EE/alert-job-config-repo/
```
<b> pay attention for type of slashes (here is "/") used at paths in all config files. </b>

Add native profile at same file to the line
```bash
spring.profiles.active=dev,native
```
This is needed to use local configuration

2. You may add Environment Variables in Running configuration in your IDE.

### 7. Hosts

Go by path and open file for edit 
```bash
c:\Windows\System32\drivers\etc\hosts
```

Add next lines to the end of file
```bash
127.0.0.1 config eureka keycloak gateway notification parser core 
127.0.0.1 auth.alertjob.by alertjob.by
```

### 8. Nginx

Open link https://nginx.org/en/download.html and download Stable version of Nginx. Unzip it to convenient place. Open that folder and proceed to configure file
```bash
(your_path)\nginx-(version)\conf\nginx.conf
```
Here you need to replace server configuration by that. 
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
Replace YOUR_PATH by path to front/dist folder of project. Should look like this
```bash 
root e:/Programming/Java_EE/alert-job/front/dist; 
```
<b> Whitespaces shouldn't be present at path string and pay attention to used type of slashes (here is "/").</b> Save changes and close file. Open command line at nginx folder and execute next
```bash 
cd (your_path)\nginx-(version)
nginx -t
```
"nginx.conf syntax is ok" and "test is successful" messages have to be presented.

 Then you need to execute nginx.exe. You may need to execute that file for project run after every computer restart.

### 9. Run project
Run project services using your IDE in next order: 
1. config 
2. eureka 
3. gateway 
4. parser 
5. core 
6. notification 

After that project available on [alertjob.by](http://alertjob.by/). If everything is alright then you can return to step #2 and change ExecutionPolicy to your default value.

### 9. Test account
* Login: test
* Password: test