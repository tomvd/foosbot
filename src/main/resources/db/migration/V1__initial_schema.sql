CREATE TABLE players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    slack_user_id TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL
);

CREATE TABLE games (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'LOBBY',
    start_time TEXT,
    end_time TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE game_sets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id INTEGER NOT NULL REFERENCES games(id),
    set_number INTEGER NOT NULL,
    blue_score INTEGER NOT NULL DEFAULT 0,
    red_score INTEGER NOT NULL DEFAULT 0,
    winning_team TEXT
);

CREATE TABLE game_players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id INTEGER NOT NULL REFERENCES games(id),
    player_id INTEGER NOT NULL REFERENCES players(id),
    team TEXT NOT NULL,
    position TEXT NOT NULL,
    goals INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_games_channel_status ON games(channel_id, status);
CREATE INDEX idx_game_players_game ON game_players(game_id);
CREATE INDEX idx_game_players_player ON game_players(player_id);
CREATE INDEX idx_game_sets_game ON game_sets(game_id);
CREATE INDEX idx_games_start_time ON games(start_time);
