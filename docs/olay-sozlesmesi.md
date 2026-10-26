# Olay Sözleşmesi (Backend ↔ Frontend)

Bu doküman frontend'in backend ile konuşurken ihtiyaç duyduğu her şeyi içerir.
Backend adresi: `http://localhost:8080`, WebSocket: `ws://localhost:8080/ws`.
Tüm uçların şeması ayrıca Swagger'da: `http://localhost:8080/swagger-ui.html`.

## 1. Genel kurallar

- Oyun 3 kişiliktir. Her oyuncunun başlangıç bütçesi **1000** (milyon €).
- **Konsept rastgele** belirlenir (oda kurulurken). Oyuncu konsept veya zorluk seçmez. Konsept adı odanın cevabında `conceptName` olarak gelir.
- Kimlik: misafir girişinden dönen `playerToken`. REST isteklerinde `X-Player-Token` başlığı, WebSocket'te STOMP CONNECT başlığı olarak gönderilir.
- Kartların reyting ve fiziksel değerleri (`overall_rating`, `pac`, `sho`...) oyun bitene kadar hiçbir REST/WebSocket cevabında yoktur. Sadece oyun sonu sonucunda (`game_finished` olayı ve `GET /api/rooms/{id}/result`) puanlar görünür.
- Fiyatlar tamsayıdır (milyon €).

## 2. Oyun akışı

1. Misafir girişi → `playerToken` alınır.
2. Oda kur (`POST /api/rooms`) veya açık odaya katıl (`POST /api/rooms/{id}/join`). Oda `LOBBY` durumundadır.
3. 3. oyuncu katılınca oda `SETUP` olur.
4. Odanın bir oyuncusu `POST /api/rooms/{id}/start` çağırır (202). Oda `/topic/rooms/{id}` üzerinden `game_started` yayınlar.
5. Yaklaşık 5 sn sonra ilk ihale açılır (`auction_started`). 22 ihale sırayla yapılır (T1 ve T2 kartları).
6. Her ihalede: açan oyuncu açılış fiyatını otomatik verir ve lider olur. Diğerleri +1/+5/+10 ile yükseltir veya çekilir. Son kabul edilen tekliften **10 sn** sonra ihale kapanır, lider kartı alır (`auction_closed`). Sonraki ihale yaklaşık 3 sn sonra açılır.
7. 22 ihale bitince `auctions_finished`, kısa bir beklemeden sonra boş kalan slotlar için Tier 3 / U19 ataması (`tier3_assigned`) ve oyun sonu (`game_finished`).

Oda durumları: `LOBBY` → `SETUP` → `AUCTION_ACTIVE` → ... → `FINISHED`.

Slotlar (11 adet): `GK, LB, CB_1, CB_2, RB, CDM, CM, CAM, LW, ST, RW`.
Mevkiler: `GK, LB, CB, RB, CDM, CM, CAM, LW, ST, RW` (CB_1 ve CB_2 ikisi de CB mevkisinden kart alır).

## 3. REST uçları

| Uç | İstek | Başarılı cevap |
| --- | --- | --- |
| `POST /api/guests` | gövde: `{ "username": "Kursat" }` (2-20 karakter, JSON) | 201 `{ "userId", "username", "playerToken" }` |
| `POST /api/rooms` | başlık: `X-Player-Token`. Gövde gerekmez (isteğe bağlı `{ "seed": 123 }` hata ayıklama içindir) | 201 `RoomView` (durum `LOBBY`) |
| `GET /api/rooms` | - | 200 katılıma açık (`LOBBY`) odaların `RoomView` listesi |
| `POST /api/rooms/{roomId}/join` | başlık: `X-Player-Token` | 200 `RoomView`. 3. oyuncu girince durum `SETUP` olur |
| `GET /api/rooms/{roomId}` | - | 200 `RoomView` |
| `POST /api/rooms/{roomId}/start` | başlık: `X-Player-Token`. Odanın herhangi bir oyuncusu, oda `SETUP` olmalı | 202 (gövdesiz) |
| `GET /api/rooms/{roomId}/live` | - | 200 `LiveStateView` (bağlanan veya kopup dönen istemci kendini buradan senkronlar) |
| `GET /api/rooms/{roomId}/result` | - | 200 `GameResultView` (oyun bitince). Bitmediyse 409 `GAME_NOT_FINISHED` |

### RoomView

```json
{
  "roomId": 5,
  "status": "LOBBY",
  "conceptName": "Şampiyonlar Ligi",
  "startingBudget": 1000,
  "players": [
    { "roomPlayerId": 11, "userId": 3, "seatNumber": 1, "username": "Kursat", "budget": 1000 }
  ]
}
```

### LiveStateView

```json
{
  "finished": false,
  "processedAuctions": 4,
  "totalAuctions": 22,
  "current": {
    "sequenceNo": 5, "slot": "CB_1", "tier": "T1",
    "card": { "cardId": 91, "playerName": "...", "nationality": "...", "photoUrl": "...",
              "club": "...", "position": "CB", "startingPrice": 20 },
    "price": 25, "leaderRoomPlayerId": 12,
    "deadlineMs": 1790000000000, "serverNowMs": 1789999995000
  },
  "players": [
    { "roomPlayerId": 11, "userId": 3, "seatNumber": 1, "username": "Kursat",
      "budget": 940, "connected": true, "filledSlots": ["GK", "ST"] }
  ]
}
```

`current` ihale yokken (iki ihale arası) `null` olabilir.

### Hata biçimi

İş kuralı hataları: `{ "code": "...", "message": "..." }`.

| HTTP | code | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Gövde var ama alan geçersiz (ör. kullanıcı adı 2-20 karakter değil) |
| 401 | `TOKEN_REQUIRED` / `TOKEN_INVALID` | `X-Player-Token` yok veya geçersiz |
| 403 | `NOT_IN_ROOM` | Odanın oyuncusu değilsin |
| 404 | `ROOM_NOT_FOUND` / `GAME_NOT_STARTED` | Oda yok / oyun başlamadı |
| 409 | `ROOM_NOT_IN_LOBBY`, `ALREADY_IN_ROOM`, `ROOM_FULL`, `GAME_ALREADY_STARTED`, `GAME_NOT_FINISHED` | Duruma uygun olmayan istek |
| 503 | `NO_PLAYABLE_CONCEPT` | Oynanabilir konsept yok |

Not: İstek gövdesi hiç yoksa veya bozuk JSON ise Spring'in standart 400 cevabı gelir (`timestamp`, `status`, `error`, `path`), `code` alanı olmaz.

## 4. WebSocket (STOMP)

- Bağlantı: `ws://localhost:8080/ws` (düz WebSocket, **SockJS yok**).
- CONNECT başlığı: `X-Player-Token: <playerToken>`. Geçersizse bağlantı reddedilir.
- Abonelikler:
    - `/topic/rooms/{roomId}`: oda olayları (herkese)
    - `/user/queue/bids`: sadece sana giden olaylar (`bid_rejected`, `withdraw_rejected`)
    - `/user/queue/errors`: iş kuralı hataları `{ "code", "message" }` (ör. odada değilsin)
- Göndereceğin mesajlar:
    - `/app/rooms/{roomId}/bid` gövde `{ "expectedPrice": 25, "increment": 5 }`. `expectedPrice` ekranda gördüğün güncel fiyat, `increment` 1, 5 veya 10.
    - `/app/rooms/{roomId}/withdraw` gövdesiz. Bu ihaleden çekilir, bir daha giremezsin. Lider çekilemez.

## 5. Olaylar

Her olay şu zarfla gelir: `{ "type": "...", "data": { ... } }`.

`CardInfo` (ihaleye çıkan kartın herkese açık bilgisi): `{ "cardId", "playerName", "nationality", "photoUrl", "club", "position", "startingPrice" }`.

### Oda olayları (`/topic/rooms/{roomId}`)

| type | data |
| --- | --- |
| `game_started` | `{ "totalAuctions": 22 }` |
| `auction_started` | `{ "sequenceNo", "totalAuctions", "slot", "tier", "card": CardInfo, "price", "openerRoomPlayerId", "deadlineMs", "serverNowMs" }`. `price` açılış fiyatı, `openerRoomPlayerId` açan ve ilk lider olan oyuncudur |
| `bid_accepted` | `{ "sequenceNo", "price", "leaderRoomPlayerId", "increment", "deadlineMs", "serverNowMs" }` (sayaç sıfırlanır) |
| `player_withdrew` | `{ "roomPlayerId" }` |
| `auction_closed` | `{ "sequenceNo", "slot", "winnerRoomPlayerId", "price", "budgets": { "roomPlayerId": kalanBütçe } }` |
| `auction_skipped` | `{ "sequenceNo", "slot", "reason": "NO_OPENER" }`. Kimse ihaleyi açamadı, kart satılmadı, slot sonra Tier 3 / U19 ile dolar |
| `auctions_finished` | `{ "budgets": { ... } }` (22 ihale bitti) |
| `player_disconnected` | `{ "roomPlayerId" }` |
| `player_reconnected` | `{ "roomPlayerId" }` |
| `tier3_assigned` | `{ "entries": [ { "roomPlayerId", "slot", "type", "price", "card": CardInfo } ], "budgets": { ... } }`. `type`: `TIER3_AUTO` veya `U19_PENALTY` |
| `game_finished` | `GameResultView` (aşağıda) |

### Sana özel olaylar (`/user/queue/bids`)

| type | data |
| --- | --- |
| `bid_rejected` | `{ "reason", "price", "leaderRoomPlayerId" }`. Güncel fiyat ve lider de gelir, ekranını senkronla |
| `withdraw_rejected` | `{ "reason" }` |

`bid_rejected.reason` değerleri: `AUCTION_CLOSED`, `UNKNOWN_PLAYER`, `PLAYER_WITHDRAWN`, `PLAYER_DISCONNECTED`, `SLOT_FULL`, `INVALID_INCREMENT`, `STALE_PRICE` (fiyat sen basmadan değişti), `ALREADY_LEADER`, `INSUFFICIENT_BUDGET`.

`withdraw_rejected.reason` değerleri: `AUCTION_CLOSED`, `UNKNOWN_PLAYER`, `LEADER_CANNOT_WITHDRAW`, `ALREADY_WITHDRAWN`.

### Sayaç

`deadlineMs` ihalenin kapanacağı an (sunucu saatiyle, epoch ms). İstemci saati kaymış olabilir. Kalan süreyi `deadlineMs - serverNowMs` ile hesapla, sonra kendi saatinle geri say. Her `bid_accepted` yeni bir `deadlineMs` getirir.

### GameResultView (`game_finished` olayı ve `GET /result`)

```json
{
  "roomId": 5,
  "draw": false,
  "winnerRoomPlayerIds": [11],
  "players": [
    {
      "roomPlayerId": 11, "userId": 3, "seatNumber": 1, "username": "Kursat",
      "totalRating": 812, "remainingBudget": 140, "totalSpent": 860, "winner": true,
      "squad": [
        { "slot": "GK", "playerName": "...", "nationality": "...", "photoUrl": "...",
          "club": "...", "position": "GK", "overallRating": 84, "paidPrice": 35,
          "acquisitionType": "AUCTION" }
      ]
    }
  ]
}
```

`acquisitionType`: `AUCTION`, `TIER3_AUTO`, `U19_PENALTY`. Beraberlikte `draw: true` ve `winnerRoomPlayerIds` boş olabilir. Kazanan sırası: toplam reyting, eşitse kalan bütçe, eşitse beraberlik.

## 6. Kopma ve yeniden bağlanma

- Bağlantı kopunca (oyuncunun son WebSocket oturumu kapanınca) `player_disconnected` yayınlanır. Aktif ihalede lider değilse o ihale için çekilmiş sayılır. Lider ise teklifi geçerli kalır.
- Aynı token ile yeniden bağlanınca `player_reconnected` yayınlanır. Mevcut ihaleye giremez, sonrakilere girer.
- Dönen istemci `GET /api/rooms/{id}/live` ile kendini senkronlar.
- Kopuk oyuncular da oyun sonunda Tier 3 / U19 kartlarını alır.

## 7. Süre ayarları (sunucu tarafı)

| Ayar | Varsayılan |
| --- | --- |
| `auction.first-delay-ms` | 5000 (oyun başlayınca ilk ihaleye kadar) |
| `auction.next-delay-ms` | 3000 (ihaleler arası) |
| `auction.bid-timeout-ms` | 10000 (son tekliften sonra kapanış) |

## 8. Tier 3 ve U19 kuralı

22 ihaleden sonra her slot için (`GK` → `RW` sırasıyla): sabit fiyatla otomatik atama. Hücum slotları (`LW`, `ST`, `RW`) 30, diğerleri 20. Bütçesi yetiyorsa oyuncu o slotun Tier 3 kartını alır (`TIER3_AUTO`). Yetmiyorsa fiyatı 0 olan, konseptin U19 havuzundan rastgele bir ceza kartı verilir (`U19_PENALTY`). Aynı futbolcu bir kadroda iki kez bulunmaz.