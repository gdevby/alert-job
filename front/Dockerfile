FROM nginx:1.25.2
# Must add json files before import, volume is not mounted yet
RUN apt update
RUN apt install -y npm

WORKDIR /opt/front
ADD . /opt/front/
RUN npm install
RUN npm run build
RUN cp -r /opt/front/dist /usr/share/nginx/html/front