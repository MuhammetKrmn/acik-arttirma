# 1. Node 22 kullanıyoruz
FROM node:22-alpine

# 2. Konteyner içinde çalışacağımız klasör
WORKDIR /app

# 3. Sadece package.json'ı alıyoruz
COPY package.json ./

# 4. Windows/Linux işletim sistemi çakışmalarını ezip geçmek için --force ile zorla kurduruyoruz
RUN npm install --force

# 5. Projedeki tüm kodları kopyalıyoruz
COPY . .

# 6. Vite'ın varsayılan portunu dışarı açıyoruz
EXPOSE 5173

# 7. Projeyi dışarıdan erişilebilir şekilde başlatıyoruz
CMD ["npm", "run", "dev", "--", "--host"]