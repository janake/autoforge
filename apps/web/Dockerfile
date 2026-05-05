FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
COPY apps/web/package.json apps/web/package.json

RUN npm ci

COPY apps/web apps/web

RUN npm run build --workspace @autoforge/web

FROM nginx:1.27-alpine
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/web/dist /usr/share/nginx/html
COPY apps/web/docker-entrypoint.sh /docker-entrypoint.d/10-autoforge-config.sh
RUN chmod +x /docker-entrypoint.d/10-autoforge-config.sh

EXPOSE 8080
