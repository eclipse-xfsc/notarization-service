# Base
FROM node:18.12.1-bullseye as base

RUN apt-get update && \
    apt-get install -y --no-install-recommends tini && \
    rm -rf /var/lib/apt/lists/*

# Dependencies
FROM base as dependencies

ARG APP_HOME=/home/node/app
ENV NPM_CONFIG_UPDATE_NOTIFIER=false

WORKDIR ${APP_HOME}

COPY package.json ./
RUN npm i

# Build
FROM base as builder

ARG APP_HOME=/home/node/app
ARG NODE_ENV=production
ENV NODE_ENV=${NODE_ENV}
ENV NPM_CONFIG_UPDATE_NOTIFIER=false

WORKDIR ${APP_HOME}

COPY . .
RUN rm -rf node_modules dist
COPY --from=dependencies ${APP_HOME}/node_modules ${APP_HOME}/node_modules

RUN npm run build:production && npm prune --omit=dev

# Final
FROM base

ARG APP_HOME=/home/node/app
ARG NODE_ENV=production
ENV NODE_ENV=${NODE_ENV}
ENV APP_HOME=${APP_HOME}

ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["node", "dist/main"]

USER node
WORKDIR ${APP_HOME}

COPY --chown=node:node --from=builder ${APP_HOME}/package.json ${APP_HOME}/
COPY --chown=node:node --from=builder ${APP_HOME}/node_modules ${APP_HOME}/node_modules
COPY --chown=node:node --from=builder ${APP_HOME}/dist ${APP_HOME}/dist
