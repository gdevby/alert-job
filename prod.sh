#every times when you change some parameters you need to recreate images of the docker ./prod.sh create
export elasticsearch_directory=/home/app/service/aj/files_prod/elasticsearch
export database_directory=/home/app/service/aj/files_prod/apps
export path_to_docker_compose_prod_yml=/home/app/service/aj/sources/alert-job/docker-compose-prod.yml
export database_user_password=test
export database_user_login=test
#used this param by default, after change, cas another can't be worked
export keycloak_admin_password=admin
export keycloak_admin=admin
#local configuration files
export local_config_directory=/home/app/service/aj/sources/alert-job-config-repo
export keycloak_url=https://auth.gdev.by
#path to locale DIRECTORY which contains this file https://github.com/gdevby/alert-job/blob/main/parser-alert-job/src/main/resources/hubr.txt 
export parser_config_directory=/home/app/service/aj/parser
#you can run swag and after changed files swag/nginx/site-confs/default.conf to configurated resolving services
export swag_config_directory=/home/app/service/aj/swag
export front_directory=/home/app/service/aj/sources/alert-job/front
export logstash_config_directory=/home/app/service/aj/sources/alert-job/logstash/config
export logger_level=INFO
export kibana_secret_path=
docker compose -f path_to_docker_compose_prod_yml $1 $2
