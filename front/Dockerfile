FROM node:22-alpine AS builder
ARG REVISION=build_version
WORKDIR /app
COPY ./package.json ./package-lock.json ./
RUN npm install
COPY ./ ./
RUN VITE_REVISION=$REVISION npm run build

FROM nginx:1.31-alpine AS runner
COPY --from=builder /app/dist /usr/share/nginx/html
