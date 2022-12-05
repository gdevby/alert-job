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
get Client secret open http://127.0.0.1:8080, insert admin admin, after select realm myrealm and click on clients-> Credentials and copy.
curl -u "client-test-service:{CLIENT_SECRET_REPLACE}" -d "grant_type=password&username=user-test-service&password=test"  -H "Accept: application/json" -X POST http://localhost:8080/realms/myrealm/protocol/openid-connect/token
<br>
and after execute with access_token 
curl -v -H "Accept: application/json" -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI3M0QyYmFSZjBNc2RWUDVfMk9GcENIc252OWJrNDlXQ0ZvZWEyRmtCZTVrIn0.eyJleHAiOjE2NzAyNDczODUsImlhdCI6MTY3MDI0NzA4NSwianRpIjoiNzkyMjIzYjMtNzdlOS00NmVhLWJjNjctZWQ0YzdjNTA4OTJhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9teXJlYWxtIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjE5ODg5ODkwLThhNmQtNGZmNC1hOWE0LTE1ZTE4YzVhZTIzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNsaWVudC10ZXN0LXNlcnZpY2UiLCJzZXNzaW9uX3N0YXRlIjoiM2JhY2FiN2QtZDI3Ny00ZWZhLTg3ZTYtOGQ2YzI2ZDBmOGIxIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIqIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkZWZhdWx0LXJvbGVzLW15cmVhbG0iLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwiYWRtaW4tdGVzdC1zZXJ2aWNlLXJvbGUiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJzaWQiOiIzYmFjYWI3ZC1kMjc3LTRlZmEtODdlNi04ZDZjMjZkMGY4YjEiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6InVzZXItdGVzdC1zZXJ2aWNlIiwiZ2l2ZW5fbmFtZSI6IiIsImZhbWlseV9uYW1lIjoiIn0.Vq9XGblOuotHOZN2W6lw6X7otim4cFfCj6lnTzqqoWSS1A7qNHWx-eeuOJ8_UDGveYkv7q0BZkFhyNdPYp3Ud_s-k2OGp1CoBnkQLMYg__R27yF2yE-LRO-F-r9GS8Z5mCpFtsWFdqH_nlWcbChv0upexXm-EHz_0PGsdm8zoiRVQntwuLJaNkuoJFL5qwiIYxAHSTmsamDxSIndlOuPDL7lEFerLgjVEVH3EAVVXjyl9p7-EaolCMNeQ_ZzSPBo7fulspzUVQQKGr85JLYn5gnOOxiRbE1qg0gquHLKWcsZj6byd2gvbZvZhQ7w5rz1T7qgGGXBFh2G2dqNAxYO5Q" http://127.0.0.1:8015/alert-job-test-service/secure/message

<br>

