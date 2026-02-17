# :monkey_face: Baboon — a Slack Foosball Matchmaking Bot

A Slack bot for office foosball matchmaking and scorekeeping. Manages 2v2 game lobbies, tracks live scores via interactive buttons, posts scoreboard results, and computes weekly player stats.

## Features

- **Matchmaking Lobby** — `@baboon play` creates/joins a lobby with team assignment, position switching, and shuffle
- **Live Scoring** — Interactive buttons for each player to track goals in real-time
- **Game Rules** — Win condition: 11+ goals with 2+ goal lead
- **Scoreboard** — Post-game summary with winners, goals per player, and game duration
- **Weekly Stats** — Player, Forward, and Goalie stats with GPG, GAA, +/-, and more

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- A Slack workspace where you can create apps

## Slack App Setup

1. Go to [api.slack.com/apps](https://api.slack.com/apps) and click **Create New App** → **From scratch**
2. Name it `baboon` and select your workspace

### Bot Token Scopes (OAuth & Permissions)
Add these **Bot Token Scopes**:
- `app_mentions:read`
- `chat:write`
- `users:read`

### Socket Mode
1. Go to **Socket Mode** and enable it
2. Create an **App-Level Token** with the `connections:write` scope — save this as `SLACK_APP_TOKEN`

### Event Subscriptions
Enable **Events** and subscribe to:
- `app_mention`


### Install to Workspace
1. Go to **Install App** and click **Install to Workspace**
2. Copy the **Bot User OAuth Token** — save this as `SLACK_BOT_TOKEN`
3. Copy the **Signing Secret** from **Basic Information** — save this as `SLACK_SIGNING_SECRET`

## Deployment

1. Clone the repository:
   ```bash
   git clone <repo-url> && cd baboon
   ```

2. Create your `.env` file:
   ```bash
   cp .env.example .env
   # Edit .env with your Slack tokens
   ```

3. Build and run with Docker Compose:
   ```bash
   docker compose up -d --build
   ```

4. Check logs:
   ```bash
   docker compose logs -f
   ```

## Usage

| Command | Description |
|---------|-------------|
| `@baboon play` | Start or join a foosball game |
| `@baboon stats` | Show weekly player statistics |

### Game Flow
1. Someone mentions `@baboon play` — a lobby is created
2. Others mention `@baboon play` to join (4 players needed)
3. Players can **Switch Positions** (F/G) and **Shuffle Teams**
4. All 4 players hit **Ready** to start the game
5. During the game, click player buttons to add goals
6. **Game Won** becomes available when a team has 11+ goals with a 2+ lead
7. **Match Over** ends the game early
8. A scoreboard is posted with the final results

## Development

### Local Build (requires Java 21+)
```bash
./gradlew build
./gradlew shadowJar
java -jar build/libs/baboon-1.0.0-all.jar
```

### Tech Stack
- Java 21, Micronaut 4.x, Gradle
- Slack Bolt for Java (Socket Mode)
- SQLite via JDBC + Flyway migrations
- Docker + Docker Compose
