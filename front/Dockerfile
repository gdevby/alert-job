FROM node:22-alpine AS builder
WORKDIR /app
COPY ./package.json ./package-lock.json ./
RUN npm install
COPY ./ ./
RUN npm run build

FROM nginx:1.31-alpine AS runner
COPY --from=builder /app/dist /usr/share/nginx/html/front
