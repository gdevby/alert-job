# alert-job

Рабочий проект доступен по ссылке [aj.gdev.by](https://aj.gdev.by)

**Основная цель проекта**: быстрый, избирательный способ получения уведомлений о нужных заказах по вашим настроенным фильтрам.<br>
Имеется два типа фильтров позитивные и негативные. Сначала применяются фильтры для выбора заказов, потом негативные отсеивают заказы, которые вам не подходят.<br>
К примеру "Я хочу получать заказы, которые содержат в названии backend и не хочу получать заказы, которые содержат в названии nodejs".<br>

На данный момент на сайте доступны следующие биржи:[freelance.ru](https://freelance.ru), [freelance.habr.com](https://freelance.habr.com), [fl.ru](https://www.fl.ru).

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

Для запуска приложения Вам необходимо иметь на своём компьютере **Java 17**, **Maven**, **Docker**.<br>


Для начала необходимо добавить доменные имена к локальному хосту в файл etc/hosts/
```
127.0.0.1 alertjob.by auth.alertjob.by
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
Переходи в директорию keycloak и выполняем скрипт
```
cd keycloak
./build.sh
```
Далее необходимо вернуться в родительский каталог для запуска фронта
```
cd front
npm run build
```
После получаем все образы проекта и запускаем их
```
docker compose pull
docker compose create
docker compose start keycloak logstash
docker compose start
```
Проверить статус служб можно при помощи команды 
```
docker compose ps
```
После выполнения вышеперечисленных шагов вы можете открыть страницу [alertjob.by ](http://alertjob.by)
* Для запуска приложения необходимо чтобы был свободен 80 порт