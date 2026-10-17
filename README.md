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

# 2) Uygulamayı başlat
cd backend
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Veritabanı: `localhost:5432`, db/kullanıcı/şifre: `auction` (`.env` ile değiştirilebilir)

Gereksinimler: JDK 21, Maven 3.9+, Docker.

## Git kuralları

- `main` korumalı: PR + 1 onay, squash merge
- Dal adları: `feature/`, `fix/`, `docs/`, `db/`, `chore/`
