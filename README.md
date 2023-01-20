# alert-job
Implemented alert job system to get proper messages with custom filters from lots sites:fl.ru, habr.ru<br>
You can save time.<br>
You can answer very fast<br>
You don't need to check sites to find order for yourself. System ping you when proper order will be created. It looks for 24 per day for you order.<br>

please add to /etc/hosts/ 
127.0.0.1 alertjob.by   auth.alertjob.by
You need to have java 17 and maven.
To run application, you need to clone alert-job-parent. After cd to this directory. and execute command mvn clean install.<br>
Now move to keycloak and execute build.sh<br>
Now build front move to front folder cd front and exeucte npm install, after nmp run build <br>
now you can run docker compose pull<br>
now you can run all modules with docker compose up<br>

after this you can check status of the services with docker compose ps <br>
if all services are running. you can open page http://alertjob.by<br>
test user :test test<br>
usefull links <br>
https://keycloak.discourse.group/t/keycloak-17-docker-container-how-to-export-import-realm-import-must-be-done-on-container-startup/13619/23

