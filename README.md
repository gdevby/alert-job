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
to test gateway routes http://127.0.0.1:8015/alert-job-test-service/public/message where test-service name of the service white routes by gateway<br>
//wait minute before test cas services should be registrated.<br>
to test secure endpoint you need to get token with curl <br>
get Client secret open http://127.0.0.1:8080, insert admin admin, after select realm myrealm and click on clients-> Credentials and copy.<br>
curl -u "client-test-service:{CLIENT_SECRET_REPLACE}" -d "grant_type=password&username=user-test-service&password=test"  -H "Accept: application/json" -X POST http://localhost:8080/realms/myrealm/protocol/openid-connect/token
<br>
and after execute with access_token <br>
curl -v -H "Accept: application/json" -H "Authorization: Bearer {TOKEN}" http://127.0.0.1:8015/alert-job-test-service/secure/message

<br>

