# Marlow

A review desk for neighborhood shops. Paste a review or upload a CSV of old ones. Marlow scores the mood, files it under a subject, and drafts a short reply in the shop's voice. Each shop's key only opens that shop's book.

This is a personal project. It is not a company product.

## What is in here

- **Reading reviews.** Spring AI (`ChatClient` through an OpenAI-compatible chat API) turns a review into sentiment, a score from -1 to 1, a category, and a draft reply. The default model is Gemini. With no API key, the same path runs an on-desk reader so the app still works offline.
- **Bulk CSV.** `POST /api/uploads` stores the file and publishes a message on RabbitMQ (`marlow.uploads`). A listener starts a Spring Batch job that reads the CSV off the request thread.
- **One database, many shops.** PostgreSQL tables for tenants, staff users, API keys, reviews, and upload jobs. Keys are stored as SHA-256 hashes. Every review and upload query is scoped by `tenant_id`.
- **The numbers.** `/api/analytics/summary`, `/api/analytics/categories`, `/api/analytics/trends`, and `/api/analytics/sources` feed the desk.

## Run it

You need Java 21, Maven, PostgreSQL, and RabbitMQ.

```bash
# PostgreSQL
sudo -u postgres psql -c "CREATE USER marlow WITH PASSWORD 'marlow';"
sudo -u postgres psql -c "CREATE DATABASE marlow OWNER marlow;"

# or, if you use Docker:
docker compose up -d

mvn spring-boot:run
```

The API listens on [http://127.0.0.1:8097](http://127.0.0.1:8097).

```bash
cd frontend
npm install
npm run dev
```

The desk listens on [http://127.0.0.1:43123](http://127.0.0.1:43123).

### Demo shops

These keys are local sample data, seeded on first boot. They are not secrets.

| Shop | City | API key |
| --- | --- | --- |
| Elm & Oak Bakery | Chandigarh | `marlow_demo_elm_oak_7f3a` |
| Brightline Dental | Patiala | `marlow_demo_brightline_2c91` |

Send the key as `X-Api-Key`.

### AI analysis

Reviews are analysed with Spring AI. The default provider is Gemini:

```bash
export GEMINI_API_KEY=...
mvn spring-boot:run
```

To use OpenAI instead:

```bash
export OPENAI_API_KEY=sk-...
export MARLOW_AI_BASE_URL=https://api.openai.com
export MARLOW_AI_COMPLETIONS_PATH=/v1/chat/completions
export MARLOW_AI_MODEL=gpt-4o-mini
export MARLOW_AI_MIN_INTERVAL_MS=0
mvn spring-boot:run
```

Set `MARLOW_AI_MODE=heuristic`, or leave out the key, to use the on-desk reader only.

If the model call fails, that review falls back to the on-desk reader and is marked `heuristic-fallback`.

## CSV

```csv
author,rating,source,reviewed_at,text
Ananya,5,Google,2026-10-01,"The almond croissant was fresh and perfect"
```

`reviewer`, `stars`, `channel`, `date`, and `comment` are accepted as header aliases. A sample file lives at `frontend/public/sample-reviews.csv`.

## Tests

```bash
mvn test
```

Tests use H2 and do not start the Rabbit listener. The batch test launches the import job directly.

## API sketch

- `GET /api/meta` — app name and which reader is on
- `GET /api/tenant` — shop, staff, key prefixes
- `GET|POST /api/reviews`
- `POST /api/reviews/{id}/redraft?tone=warm|brief|accountable`
- `PATCH /api/reviews/{id}` — edit a draft or mark it sent
- `POST /api/uploads` — multipart field `file`, returns `202`
- `GET /api/uploads` and `GET /api/uploads/{id}`
- `GET /api/analytics/summary|categories|trends|sources?from=YYYY-MM-DD&to=YYYY-MM-DD`
