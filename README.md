# Açık Arttırma Oyunu

3 oyunculu, canlı açık arttırmayla 11 kişilik futbol kadrosu kurulan oyun.

## Klasör yapısı

| Klasör | İçerik |
| --- | --- |
| `backend/` | Spring Boot (Java 21, Maven) sunucusu |
| `frontend/` | İstemci |
| `docs/` | Kararlar, olay sözleşmesi, şema notları |
| `docker-compose.yml` | Yerel PostgreSQL 16 |

## Backend'i çalıştırma

```bash
# 1) Veritabanını başlat
docker compose up -d

# 2) Uygulamayı başlat (dev profili: sahte test verisiyle)
cd backend
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Windows PowerShell/cmd'de `./mvnw` yerine `.\mvnw` yazılır.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Veritabanı: `localhost:5433` (yerel PostgreSQL ile çakışmasın diye 5433), db/kullanıcı/şifre: `auction`
- Şemayı sıfırdan kurmak için: `docker compose down -v` sonra `docker compose up -d`

## Testler

Kural motoru testleri (engine) Docker gerektirmez. Veritabanı testi için Docker'daki PostgreSQL açık olmalıdır.

```bash
cd backend
./mvnw test
```

## Dev verisi

`dev` profili şemaya ek olarak `db/dev/V900__dev_seed.sql` dosyasını yükler:
1 sahte konsept ve her mevki ve tier için 4 kart. Her mevki için 1 U19 ceza kartı (reyting 50) ise `V2__penalty_cards.sql` ile gelir.
Gerçek konsept/futbolcu verisi veritabanı sorumlusu tarafından ayrıca girilir.

Gereksinimler: JDK 21, Docker.

## Git kuralları

- `main` korumalı: PR + 1 onay, squash merge
- Dal adları: `feature/`, `fix/`, `docs/`, `db/`, `chore/`