# alert-job
Версия для разработчиков доступна по ссылкам [для Windows](README_FOR_DEVELOPERS_WINDOWS_RU.md) и [для Linux](README_FOR_DEVELOPERS_LINUX_RU.md)<br>
Рабочий проект доступен по ссылке [aj.gdev.by](https://aj.gdev.by)

**Основная цель проекта**: быстрый, избирательный способ получения уведомлений о нужных заказах по вашим настроенным фильтрам.<br>
Имеется два типа фильтров позитивные и негативные. Сначала применяются фильтры для выбора заказов, потом негативные отсеивают заказы, которые вам не подходят.<br>
К примеру "Я хочу получать заказы, которые содержат в названии backend и не хочу получать заказы, которые содержат в названии nodejs".<br>

На данный момент на сайте доступны следующие биржи: [freelance.ru](https://freelance.ru), [fl.ru](https://www.fl.ru), [weblancer.net](https://www.weblancer.net), [freelancehunt](https://freelancehunt.com/), [youdo](https://youdo.com/), [kwork](https://kwork.ru/), [freelancer](https://www.freelancer.com/), [truelancer](https://www.truelancer.com/).

Проект писался на микросервисной архитектуре.<br>
Используемые технологии:

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

### Запуск приложения на своём компьютере

- Для запуска приложения необходимо чтобы был свободен 80 порт<br>

Для запуска приложения Вам необходимо иметь на своём компьютере **Java 17**, **Maven**, **[Docker](#инструкция-по-установке-docker)**.<br>

Для начала необходимо добавить доменные имена к локальному хосту в файл `/etc/hosts` (использовать `localhost` нельзя из-за gateway). Для этого надо узнать какой айпи выдал вашему компьютеру модем. В линуксе команда `hostname -I` , если три IP не вывелись на экран, тогда установите пакет `sudo apt install net-tools`. После того как три IP вывелись на экран скопируйте первый из них и введите: 

```
sudo nano /etc/hosts

добавте ваш IP и строку alertjob.by
Пример:
192.168.100.17 alertjob.by
```

Клонируем проект

```
git clone https://github.com/gdevby/alert-job.git
```

Переходим в директорию и собираем проект

```
cd alert-job
mvn clean install
```

Переходим в родительскую директорию, а затем в директорию keycloak и выполняем скрипт

```
cd ..
cd keycloak
./build.sh
```

Далее необходимо вернуться в родительский каталог для запуска фронта

```
cd ..
cd front
docker pull nginx:1.25.2
sudo apt install npm
npm i
npm run build
build-prod-example.sh
```

После снова переходим в родительский каталог и получаем все образы проекта, и запускаем их
```
cd ..
docker compose pull grafana logstash elasticsearch prometheus nginx-proxy
docker compose create
docker compose start keycloak logstash

ждем 15 секунд

docker compose start
```

Проверить статус служб можно при помощи команды

```
docker compose ps -a
```

После выполнения вышеперечисленных шагов вы можете открыть страницу [alertjob.by ](http://alertjob.by)

Тестовый аккаунт:
* Логин: test
* Пароль: test

Возможные проблемы:
1) При запуске `mvn clean install` сборка проекта может упасть. Для этого проверьте версию Java (`java --version`). При необходимости установите Java 17 с помощью команды `sudo apt install openjdk-17-jdk`
2) Так же возможна проблема с запретом доступа на создание контейнеров. Следуйте инструкции по установке [Docker](#инструкция-по-установке-docker).
3) Не запускается ElasticSearch. Для этого измените права к директории public `sudo chmod 777 public`. Также проблема может возникнуть из-за большого потребления памяти. Для этого в `docker-compose.yml` в настройках ElasticSearch либо увеличьте параметр `mem_limit`, либо уменьшите размер кучи (ES_JAVA_OPTS=-Xms512m -Xmx512m)
(heap min size, heap max size)

### Инструкция по установке Docker
Следующая команда удалит все предыдущие версии Docker
```bash
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do sudo apt-get remove $pkg; done
```

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
> Если вы используете дистрибутив Linux не Ubuntu, к примеру Linux Mint, вы должны использовать `UBUNTU_CODENAME` вместо `VERSION_CODENAME`.

Запустите команду по установке Docker

```bash
sudo apt-get install docker-ce docker-ce-cli [containerd.io](http://containerd.io/) docker-buildx-plugin docker-compose-plugin
```
Добавьте пользователя `docker` в вашу группу `sudo usermod -a -G $USER docker`<br>
Перезагрузите ПК

