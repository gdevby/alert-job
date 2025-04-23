# alert-job
Version for developers available by links [for Windows](README_FOR_DEVELOPERS_WNDOWS_EN.md) and [for Linux](README_FOR_DEVELOPERS_LINUX_EN.md)<br>
Russian version you can open by [link](README_RU.md)<br>
The working project is available at the link [aj.gdev.by](https://aj.gdev.by)

**The main goal of the project**:a fast, selective way to get notifications about the needed orders based on your configured filters.<br>
There are two types of filters positive and negative. First, filters are applied to select orders, then negative ones filter out orders that do not suit you.<br>
For example "I want to receive orders that contain backend in the name and do not want to receive orders that contain nodejs in the name".<br>

At the moment, the following exchanges are available on the website: [freelance.ru](https://freelance.ru), [fl.ru](https://www.fl.ru), [weblancer.net](https://www.weblancer.net), [freelancehunt](https://freelancehunt.com/), [youdo](https://youdo.com/), [kwork](https://kwork.ru/), [freelancer](https://www.freelancer.com/), [truelancer](https://www.truelancer.com/).

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

To run the application, you need to have on your computer **Java 17**, **Maven**, **[Docker](#docker-setup-instruction)**.<br>

First, you need to add domain names to the local host in the `/etc/hosts/` file (you can’t use localhost because of the gateway), you need to find out what IP was issued by your computer’s modem. For that Linux has command `hostname -I`, if it hasn't worked install package for work with network via command `sudo apt install net-tools` and try again. After that copy first IP address and write down next command: 

```
sudo /etc/hosts

paste in this file your copied IP and add after space alertjob.by

Example:
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
```

Go to the parent directory and after that go to the keycloak directory and execute the script
```
cd ..
cd keycloak
./build.sh
```

Next, you need to return to the parent directory to start the front
```
cd ..
cd front
docker pull nginx:1.25.2
sudo apt install npm
npm i
npm run build
build-prod-example.sh
```

After that returning to the parent directory, getting all images and running them
```
cd ..
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


You may encounter next problems:
1) After running `mvn clean install` project's build may fail. For that check java version (`java --version`) it's should be 17. And if necessary install appropriate java version via command `sudo apt install openjdk-17-jdk`
2) Access being denied for creating containers. Follow docker setup [instruction](#docker-setup-instruction).
3) Cannot start ElasticSearch. For that change access right to public directory `sudo chmod 777 public`. Also problem may encounter due to high memory consumption. For that in `docker-compose.yml` in elasticsearch settings either increase parameter `mem_limit`, or decrease heap size (ES_JAVA_OPTS=-Xms512m -Xmx512m)
   (heap min size, heap max size)

### Docker setup instruction

Next command will delete all previous docker version on your computer.
```bash
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do sudo apt-get remove $pkg; done
```

Run next commands
```bash
# Add Docker's official GPG key:
sudo apt-get update
sudo apt-get install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
```

> [!NOTE]
> If you use an Ubuntu derivative distro, such as Linux Mint, you may need to use UBUNTU_CODENAME instead of VERSION_CODENAME.

Run the Docker installation command

```bash
sudo apt-get install docker-ce docker-ce-cli [containerd.io](http://containerd.io/) docker-buildx-plugin docker-compose-plugin
```
Add user `docker` in your group `sudo usermod -a -G $USER docker`<br>
Reboot PC

