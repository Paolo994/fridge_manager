# Fridge Manager

Webapp per gestire il contenuto del frigorifero: scadenze, lista della spesa e ricette, con riconoscimento del cibo dalle foto tramite IA.

- **backend/**: Spring Boot 3.5, Java 21, Spring Security (registrazione e login con JWT), JPA, H2 o PostgreSQL
- **frontend/**: React 18, TypeScript, Vite. Responsive, quindi utilizzabile anche dal browser dell'iPhone

📐 Architettura e diagrammi di flusso (in inglese): [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Avvio con Docker

```powershell
copy .env.example .env      # poi imposta JWT_SECRET (e ANTHROPIC_API_KEY se vuoi l'IA)
docker compose up -d --build
```

Apri http://localhost:3000. I container sono tre:

- **frontend**: nginx serve la webapp e inoltra `/api` al backend
- **backend**: Spring Boot con il profilo `postgres`
- **db**: PostgreSQL 16, con i dati nel volume `db-data`

Solo la porta del frontend è esposta. Per cambiarla usa `APP_PORT` nel file `.env`.

| Comando | |
|---|---|
| `docker compose logs -f backend` | log del backend |
| `docker compose up -d --build` | ricostruisce dopo modifiche al codice |
| `docker compose down` | ferma i container (i dati restano) |
| `docker compose down -v` | ferma i container e **cancella il database** |

## Avvio in sviluppo

Servono Java 21, Maven e Node 18 o superiore.

```powershell
# 1. Backend (porta 8080). La chiave API è facoltativa: senza, le funzioni IA sono disattivate.
cd backend
$env:ANTHROPIC_API_KEY = "sk-ant-..."
mvn spring-boot:run

# 2. Frontend (porta 5173), in un altro terminale
cd frontend
npm install
npm run dev
```

Poi apri http://localhost:5173 e registra un account.

Il database H2 viene salvato in `backend/data/`. Per usare PostgreSQL, avvia il backend con il profilo `postgres` e imposta `DB_URL`, `DB_USER` e `DB_PASSWORD`.

### Variabili d'ambiente del backend

| Variabile | Default | Descrizione |
|---|---|---|
| `ANTHROPIC_API_KEY` | (vuota) | Chiave da console.anthropic.com. Se manca, le API IA rispondono 503 |
| `ANTHROPIC_MODEL` | `claude-opus-5-5` | Modello Claude usato |
| `JWT_SECRET` | valore di sviluppo | **Da cambiare in produzione** (almeno 32 caratteri) |
| `CORS_ORIGINS` | `http://localhost:5173` | Origini autorizzate a chiamare le API |

## Funzionalità

| # | Funzione | Come è fatta |
|---|---|---|
| 1 | Inserimento manuale o da foto | Form, oppure foto: l'IA riconosce l'alimento e legge la scadenza dall'etichetta. L'utente rivede e conferma prima di salvare |
| 2 | Foto del frigo | L'IA elenca gli alimenti visibili; per quelli freschi stima la scadenza (segnata con ≈) |
| 3 | Notifiche di scadenza | Un job ogni mattina alle 8 (e all'avvio) crea gli avvisi per gli alimenti che scadono entro 2 giorni o sono scaduti. Compaiono nella campanella e come notifica del browser, se l'utente la permette |
| 4 | Lista della spesa | CRUD; il pulsante 🛒 sul frigo toglie l'alimento e lo aggiunge alla spesa; le ricette aggiungono gli ingredienti mancanti |
| 5 | Ricette | L'IA propone 3 ricette che usano prima gli alimenti in scadenza |
| + | Assistente di cucina | Chat con un agente che legge e modifica frigo e lista della spesa |

## Dove si usa l'IA (e dove no)

La regola è usare la soluzione più semplice che funziona.

- **Riconoscimento da foto (alimento e frigo)**: *una chiamata* a Claude con immagine e **output strutturato** (JSON validato da schema). Il compito è definito (immagine in ingresso, lista in uscita), quindi un agente aggiungerebbe solo costo e latenza. → `ai/VisionService.java`
- **Ricette**: *una chiamata* con output strutturato. Il contenuto del frigo è piccolo e va tutto nel prompt. → `ai/RecipeService.java`
- **Assistente di cucina**: qui un **agente** ha senso. Le richieste sono aperte ("ho finito latte e uova, cosa mi serve per una torta?") e Claude decide quali strumenti chiamare e in che ordine: `list_fridge_items`, `add_fridge_item`, `remove_fridge_item`, `list_shopping_list`, `add_to_shopping_list`. Il ciclo è scritto a mano, con un massimo di 8 passi, e gli strumenti agiscono solo sui dati dell'utente autenticato. → `ai/KitchenAssistantAgent.java`
- **Notifiche di scadenza**: *niente IA*. È un confronto tra date, deterministico e gratuito.

Tutte le chiamate usano l'SDK Java ufficiale di Anthropic. Il modello si sceglie con `ANTHROPIC_MODEL`: **Claude Haiku 5.5** (il più economico) è stato provato su tutte e tre le funzioni e basta per questi compiti. Con Opus 5.5, Sonnet 5.5 e Fable 5.1 si attiva anche il fallback lato server: se i filtri di sicurezza rifiutano una richiesta, il server riprova su un modello di riserva. Haiku non lo supporta.

## API principali

| Metodo | Percorso | |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | restituiscono `{ token, user }` |
| GET | `/api/auth/me` | utente corrente |
| GET/POST | `/api/food` | elenco (ordinato per scadenza) e inserimento |
| POST | `/api/food/batch` | inserimento multiplo (dopo il riconoscimento da foto) |
| PUT/DELETE | `/api/food/{id}` | `DELETE ...?addToShopping=true` rimette l'alimento nella spesa |
| GET/POST | `/api/shopping` | lista della spesa |
| PATCH/DELETE | `/api/shopping/{id}`, `DELETE /api/shopping/checked` | |
| GET | `/api/notifications` | `POST /{id}/read`, `POST /read-all` |
| GET | `/api/ai/status` | `{ enabled }` |
| POST | `/api/ai/recognize-food`, `/api/ai/scan-fridge` | multipart, campo `photo` |
| POST | `/api/ai/recipes`, `/api/ai/assistant` | |

## Test

```powershell
cd backend
mvn test
```

## Prossimi passi possibili

- App iPhone: le API sono già pronte per un client mobile (JWT stateless). Con Expo / React Native si può riusare buona parte della logica TypeScript
- Notifiche push vere (Web Push con VAPID, o APNs per iOS) ed email
- Frigo condiviso tra più persone della stessa casa
- Codice a barre (Open Food Facts) come alternativa gratuita all'IA per i prodotti confezionati
