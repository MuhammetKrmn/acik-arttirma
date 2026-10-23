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
1 sahte konsept, her mevki ve tier için 4 kart ve her mevki için 4 U19 ceza kartı. Gerçek konseptlerin U19 havuzu (konsept başına her mevkide en az 3 kart) V4 dosyasında gelir.

Yeni bir migration (V5, V6...) içeren değişiklik çektikten sonra dev veritabanını sıfırla: docker compose down -v, ardından docker compose up -d. (V900 dev seed'i en son sürüm olduğu için yeni migration'lar sırasız görünür.)
## Git kuralları

- `main` korumalı: PR + 1 onay, squash merge
- Dal adları: `feature/`, `fix/`, `docs/`, `db/`, `chore/`