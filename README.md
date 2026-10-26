# Açık Arttırma Oyunu

3 oyunculu, canlı açık arttırmayla 11 kişilik futbol kadrosu kurulan oyun.
Her oyuncu 1000 milyon € ile başlar, 22 kartlık ihalelerde teklif verir, kalan slotlar otomatik doldurulur. Oyun sonunda toplam reytinge göre kazanan belirlenir.

## Klasör yapısı

| Yer | İçerik |
| --- | --- |
| `backend/` | Spring Boot (Java 21, Maven) sunucusu |
| repo kökü (`src/`, `package.json`) | Frontend (React, Vite) |
| `docs/` | Olay sözleşmesi ve diğer dokümanlar |
| `docker-compose.yml` | Yerel PostgreSQL 16 |

## Gereksinimler

- JDK 21
- Docker (PostgreSQL için)
- Node.js (frontend için)

## Backend'i çalıştırma

```bash
# 1) Veritabanını başlat (repo kökünde)
docker compose up -d

# 2) Uygulamayı başlat (dev profili: test verisiyle)
cd backend
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Windows PowerShell'de `./mvnw` yerine `.\mvnw` yazılır. Konsolda `Started AcikArttirmaApplication` yazısını bekle.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Basit WebSocket test sayfası: http://localhost:8080/ws-test.html
- Veritabanı: `localhost:5433` (yerel PostgreSQL ile çakışmasın diye 5433), db/kullanıcı/şifre: `auction`

### Veritabanını sıfırlama

Yeni bir migration içeren değişiklik çektikten sonra (V6 gibi) veya şema bozulduysa:

```bash
docker compose down -v
docker compose up -d
```

Önemli: `down -v` veritabanını siler, ardından mutlaka `docker compose up -d` çalıştır. Yoksa backend `Connection to localhost:5433 refused` hatasıyla açılmaz.

## Frontend'i çalıştırma

```bash
npm install
npm run dev
```

Adres: http://localhost:5173. Backend'in açık olması gerekir.

## Testler

```bash
cd backend
./mvnw clean test
```

67 test geçmelidir. Kural motoru testleri Docker gerektirmez. Entegrasyon testleri için Docker'daki PostgreSQL açık olmalıdır.

## API ve olay sözleşmesi

REST uçları, WebSocket olayları, hata kodları ve örnek mesajlar: [docs/olay-sozlesmesi.md](docs/olay-sozlesmesi.md).

Kısa akış: `POST /api/guests` → `POST /api/rooms` / `join` → `POST /api/rooms/{id}/start` → WebSocket `ws://localhost:8080/ws` (başlık `X-Player-Token`) → `GET /api/rooms/{id}/result`.

## Dev verisi

`dev` profili şemaya ek olarak `db/dev/V900__dev_seed.sql` dosyasını yükler: 1 sahte konsept, her mevki ve tier için 4 kart, her mevki için 4 U19 ceza kartı. Gerçek konseptler ve U19 havuzları V4 migration'ındadır.

## Sunucu ayarları (isteğe bağlı)

`application.yml` veya komut satırı ile değiştirilebilir:

| Ayar | Varsayılan |
| --- | --- |
| `auction.first-delay-ms` | 5000 |
| `auction.next-delay-ms` | 3000 |
| `auction.bid-timeout-ms` | 10000 |

## Git kuralları

- `main` korumalı: PR + 1 onay, squash merge
- Dal adları: `feature/`, `fix/`, `docs/`, `db/`, `chore/`
- Derleme çıktıları (`backend/target/`) ve `node_modules/` commit'e girmez