# Imagen de DESARROLLO (Vite dev server). Para producción: `npm run build` y servir /dist con nginx.
FROM node:20-alpine
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
