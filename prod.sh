export database_user_password=test
export database_user_login=test
docker compose -f ./docker-compose-prod.yml $1 $2
