# alert-job
Implemented load balancer of configuration git, eureka and test service<br>
To test, you need to run application with command<br>
 build every module
cd config &&  mvn clean install<br>
cd eureka && mvn clean install<br>
cd test-service && mvn clean install<br>
cd gateway && mvn clean install<br>
docker compose up<br>
wait one minute and test<br>
and wait 30 seconds and execute  http://127.0.0.1:8014/testbalancer, it should be return message with balancer<br>
to test gateway routes http://aj.by/alert-job-test-service/public/message where test-service name of the service white routes by gateway<br>
//wait minute before test cas services should be registrated.<br>
to test secure endpoint you need to get token with curl <br>
get Client secret open http://aj.by, insert admin admin, after select realm myrealm and click on clients-> Credentials and copy.<br>
curl -u "client-test-service:{CLIENT_SECRET_REPLACE}" -d "grant_type=password&username=test&password=test"  -H "Accept: application/json" -X POST http://aj.by/keycloak/realms/myrealm/protocol/openid-connect/token
<br>
and after execute with access_token <br>
curl -v -H "Accept: application/json" -H "Authorization: Bearer {TOKEN}" http://aj.by/alert-job-test-service/secure/message

<br>
to test secure api with auth <br> http://aj.by/alert-job-test-service/secure/message 
and insert username and password <br>
test<br>
test
to use one api aj.by you need to configurate nginx file <br>
server{<br>
        listen 80;<br>
        server_name aj.by;<br>
location / {<br>
                        proxy_pass http://127.0.0.1:8015;<br>
                        proxy_set_header Host $host;<br>
                        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;<br>
                        proxy_set_header X-Forwarded-Proto $scheme;<br>
                        proxy_set_header X-Real-IP $remote_addr;<br>
                }<br>
location /keycloak {<br>
                        rewrite  ^/keycloak/(.*)  /$1 break;<br>
                        proxy_pass http://127.0.0.1:8080/;<br>
                        proxy_set_header Host $host;<br>
                        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;<br>
                        proxy_set_header X-Forwarded-Proto $scheme;<br>
                        proxy_set_header X-Real-IP $remote_addr;<br>
                }<br>
}<br>
and add to /etc/hosts
127.0.0.1 aj.by

usefull links <br>
https://keycloak.discourse.group/t/keycloak-17-docker-container-how-to-export-import-realm-import-must-be-done-on-container-startup/13619/23

