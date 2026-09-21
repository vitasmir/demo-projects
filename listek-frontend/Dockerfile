FROM node:22-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .
ARG BACKEND_URL=http://listek-backend:8080
ARG ADMIN_BACKEND_URL=http://listek-admin:8090
ENV BACKEND_URL=${BACKEND_URL}
ENV ADMIN_BACKEND_URL=${ADMIN_BACKEND_URL}
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ARG ADMIN_BACKEND_URL=http://listek-admin:8090
ENV ADMIN_BACKEND_URL=${ADMIN_BACKEND_URL}

COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]
