FROM node:24-alpine AS build
WORKDIR /app
ARG VITE_API_URL=/api
ARG VITE_CANONICAL_ORIGIN=
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_CANONICAL_ORIGIN=${VITE_CANONICAL_ORIGIN}
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
