export database_user_password=test
export database_user_login=test
#used this param by default, after change, cas another can't be worked
export keycloak_admin_password=admin
export keycloak_admin=admin
export local_config_directory=/home/ubuntu/alert-job-config-repo
export keycloak_url=https://auth.gdev.by
export configuration_override_parameters_directory_docker=/alert-job-config-repo
export telegram_chat_token=
export mail_login=
export mail_password=
export parser_categories_file_path=
export swag_config=
export front_directory=
export logstash_config_directory=

docker compose -f docker-compose-prod.yml $1 $2
