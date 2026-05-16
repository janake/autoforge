FROM node:22-alpine

WORKDIR /app

COPY services/provider-proxy/package.json ./package.json
COPY services/provider-proxy/src ./src

USER node

EXPOSE 8080

CMD ["node", "src/server.mjs"]
