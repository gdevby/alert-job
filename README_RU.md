# alert-job

Рабочий проект доступен по ссылке [aj.gdev.by](https://aj.gdev.by)

**Основная цель проекта**: быстрый, избирательный способ получения уведомлений о нужных заказах по вашим настроенным фильтрам.<br>
Имеется два типа фильтров позитивные и негативные. Сначала применяются фильтры для выбора заказов, потом негативные отсеивают заказы, которые вам не подходят.<br>
К примеру "Я хочу получать заказы, которые содержат в названии backend и не хочу получать заказы, которые содержат в названии nodejs".<br>

На данный момент на сайте доступны следующие биржи:[freelance.ru](https://freelance.ru), [freelance.habr.com](https://freelance.habr.com), [fl.ru](https://www.fl.ru),[weblancer.net](https://www.weblancer.net).

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

Для запуска приложения Вам необходимо иметь на своём компьютере **Java 17**, **Maven**, **Docker**.<br>

Для начала необходимо добавить доменные имена к локальному хосту в файл etc/hosts/, надо узнать какой айпи выдал вашему компьютеру модем, в линуксе команда ifconfig, использовать локалхост нельзя из-за gateway

```
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
Если здесь будет ошибка, то возможно проверьте версию джава, необходимо 17 следующей командой java -version,
для линукс можно установить sudo apt install openjdk-17-jdk
Так же возможна проблема с запретом доступа для создание контейнеров, поможет эта инструкция https://randini-senanayake.medium.com/docker-maven-build-problem-unix-localhost-80-permission-
```

Переходим в директорию keycloak и выполняем скрипт

```
cd keycloak
./build.sh
```

Далее необходимо вернуться в родительский каталог для запуска фронта

```
cd front
docker pull nginx:1.25.2
npm i
npm run build
построить образ с помощью этого скрипта build-prod-example.sh
вернуться в родительский каталог
После получаем все образы проекта и запускаем их

```

После получаем все образы проекта и запускаем их

```
Получаем зависимости
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