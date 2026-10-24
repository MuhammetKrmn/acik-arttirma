-- Misafir oyuncunun gizli anahtarı. İstemci her istekte X-Player-Token başlığıyla yollar.
ALTER TABLE users ADD COLUMN player_token VARCHAR(36);
UPDATE users SET player_token = gen_random_uuid()::text WHERE player_token IS NULL;
ALTER TABLE users ALTER COLUMN player_token SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_player_token UNIQUE (player_token);