# alert-job
Russian version you can open by [link](https://github.com/gdevby/alert-job/blob/main/README_RU.md)<br>
The working project is available at the link [aj.gdev.by](https://aj.gdev.by)

**The main goal of the project**:a fast, selective way to get notifications about the needed orders based on your configured filters.<br>
There are two types of filters positive and negative. First, filters are applied to select orders, then negative ones filter out orders that do not suit you.<br>
For example "I want to receive orders that contain backend in the name and do not want to receive orders that contain nodejs in the name".<br>

At the moment, the following exchanges are available on the website:[freelance.ru](https://freelance.ru), [freelance.habr.com](https://freelance.habr.com), [fl.ru](https://www.fl.ru),[weblancer.net](https://www.weblancer.net)

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

- To run the application, port 80 must be free.

To run the application, you need to have on your computer **Java 17**, **Maven**, **Docker**.<br>

First, you need to add domain names to the local host in the etc/hosts/ file, you need to find out what IP was issued by your computer’s modem, in Linux the ifconfig command, you can’t use localhost because of the gateway


```
192.168.100.17 alertjob.by
```
Cloning the project
```
git clone https://github.com/gdevby/alert-job.git
```
Go to the directory and build the project
```
cd alert-job
mvn clean install
If there is an error here, then perhaps check the Java version, you need 17 the following command java -version,
for Linux you can install sudo apt install openjdk-17-jdk
There may also be a problem with access being denied for creating containers, this instruction will help https://randini-senanayake.medium.com/docker-maven-build-problem-unix-localhost-80-permission-
```
Go to the keycloak directory and execute the script
```
cd keycloak
./build.sh
```
Next, you need to return to the parent directory to start the front
```
cd front
docker pull nginx:1.25.2
npm i
npm run build
build the image using this script build-prod-example.sh
return to parent directory
Afterwards we get all the project images and run them
```
After we get all the images of the project and run them
```
get dependenices
docker compose pull grafana logstash elasticsearch prometheus nginx-proxy
docker compose create
docker compose start keycloak logstash
wait 15 seconds
docker compose start
```
You can check the status of services with the command
```
docker compose ps -a
```
After completing the above steps, you can open [alertjob.by ](http://alertjob.by)
