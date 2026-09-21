# Multistage Dockerfile: build Angular app in Node and serve with Nginx

# --- Build stage ---
FROM node:24-alpine AS build-stage

WORKDIR /app

# Copy package manifests and install dependencies (including devDeps needed for build)
COPY package.json package-lock.json* ./
RUN npm ci --silent

# Copy application source and build
COPY . .
# Build (this calls `ng build --configuration production` from package.json)
RUN npm run build --if-present --silent

# --- Production stage ---
FROM nginx:stable-alpine AS production

# Clear default nginx static content
RUN rm -rf /usr/share/nginx/html/*

# Copy built app from build stage
COPY --from=build-stage /app/dist/myapp/browser /usr/share/nginx/html/

# Copy SPA-friendly nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

# Run nginx in the foreground
CMD ["nginx", "-g", "daemon off;"]
