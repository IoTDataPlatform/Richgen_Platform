# --- stage 1: build ---
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .


RUN npm run build   # или npm run build:prod, как у тебя принято

# --- stage 2: serve static via nginx ---
FROM nginx:alpine

# удаляем дефолтный конфиг, если нужен свой — можно скопировать
RUN rm /etc/nginx/conf.d/default.conf

# простой конфиг под SPA
RUN printf 'server {\n\
    listen 80;\n\
    server_name _;\n\
    root /usr/share/nginx/html;\n\
    index index.html;\n\
    location / {\n\
        try_files $uri /index.html;\n\
    }\n\
}\n' > /etc/nginx/conf.d/app.conf

COPY --from=build /app/build /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
