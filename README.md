# alert-job

The working project is available at the link [aj.gdev.by](https://aj.gdev.by)

**The main goal of the project**:a fast, selective way to get notifications about the needed orders based on your configured filters.<br>
There are two types of filters positive and negative. First, filters are applied to select orders, then negative ones filter out orders that do not suit you.<br>
For example "I want to receive orders that contain backend in the name and do not want to receive orders that contain nodejs in the name".<br>

At the moment, the following exchanges are available on the website:[freelance.ru](https://freelance.ru), [freelance.habr.com](https://freelance.habr.com), [fl.ru](https://www.fl.ru).

The project has a microservice architecture.<br>
Used technologies:
<ol>
<li>Spring Framework</li>
<li>Spring Cloud</li>
<li>ELK</li>
<li>Keycloak</li>
<li>Spring WebFlux</li>
<li>React JS</li>
<li>Docker</li>
<li>Java 17</li>
<li>Maven</li>
</ol> 

### Launching the application on your computer

To run the application, you need to have on your computer **Java 17**, **Maven**, **Docker**.<br>

First you need to add the domain names to the local host in the file etc/hosts/
```
127.0.0.1 alertjob.by auth.alertjob.by
```
Cloning the project
```
git clone https://github.com/gdevby/alert-job.git
```
Go to the directory and build the project
```
cd alert-job
mvn clean install
```
Go to the keycloak directory and execute the script
```
cd keycloak
./build.sh
```
Next, you need to return to the parent directory to start the front
```
cd front
npm run build
```
After we get all the images of the project and run them
```
docker compose pull
docker compose create
docker compose start keycloak logstash
docker compose start
```
You can check the status of services with the command
```
docker compose ps
```
After completing the above steps, you can open [alertjob.by ](http://alertjob.by)
* To run the application, port 80 must be free.