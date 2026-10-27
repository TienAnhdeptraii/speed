# Speed backend

Spring Boot REST backend for a ZingSpeed/QQSpeed-inspired game. It currently supports player accounts, JWT authentication, a wallet, inventory, cars, and prize boxes. Direct communication with the QQSpeed client over its game protocol is not implemented.

## Requirements

- Java 25
- Maven
- MySQL

The project uses Spring Boot `4.1.2` and runs on port `8080` by default.

## Run locally

1. Create a MySQL database named `speed`:

   ```sql
   CREATE DATABASE speed;
   ```

2. Set the database credentials and a Base64-encoded JWT secret. In PowerShell:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/speed"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "your-password"

   $secretBytes = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($secretBytes)
   $env:APP_JWT_SECRET = [Convert]::ToBase64String($secretBytes)
   ```

3. Start the application:

   ```powershell
   mvn spring-boot:run
   ```

Database connection settings, JWT lifetime, and catalog file locations are in [`application.properties`](src/main/resources/application.properties). Hibernate uses `ddl-auto=update` to create/update tables during development. Keep the same JWT secret across restarts if existing tokens should remain valid.

## API

Only registration and login are public. Send `Authorization: Bearer <accessToken>` with the other requests. Admin routes require the `ADMIN` role.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a player and create their wallet and starter car |
| `POST` | `/api/auth/login` | Get a JWT access token |
| `POST` | `/api/auth/logout` | Revoke the current token |
| `GET` | `/api/players/me` | Read your profile and wallet |
| `GET` | `/api/players/{id}` | Read a player's public profile |
| `GET` | `/api/players/me/inventory` | Read your inventory |
| `GET` | `/api/players/me/selected-car` | Read your selected car |
| `PUT` | `/api/players/me/selected-car` | Select a car using `{ "playerItemId": 42 }` |
| `POST` | `/api/players/me/boxes/open` | Open one owned box |
| `POST` | `/api/admin/players/{playerId}/inventory` | Grant an item to a player's inventory |
| `POST` | `/api/admin/boxes` | Create a box definition and its reward table |

Register with `{ "username": "player1", "password": "secret", "confirmpassword": "secret" }`, then log in with `{ "username": "player1", "password": "secret" }`. The login response contains `accessToken`.

### Inventory and item IDs

Items are classified by their client `itemId`, using [`items.json`](src/main/resources/items.json). A definition can include a category and one matching subcategory, such as `VEHICLE` with `vehicleCategory: "S"`. IDs not configured there or in the inventory properties default to `ITEM`. Add new item definitions to the catalog file and restart the server to load them.

For inventory grants and box rewards, `avaIPeriod` means:

| Value | Meaning | `obtainTime` |
| --- | --- | --- |
| `-1` | Permanent ownership | `0` |
| `0` | Quantity-based item | `0` |
| Positive | Time-limited ownership | Duration in seconds |

Time-limited items continue to expire while the player is offline. Compatible quantity rewards add to an existing stack; compatible time-limited rewards add their remaining seconds. Permanent ownership stays permanent.

### Create and open a box

An admin can create a box definition:

```http
POST /api/admin/boxes
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "itemId": 18248,
  "rewards": [
    { "itemId": 13703, "quantity": 1, "avaIPeriod": 1, "status": true, "obtainTime": 259200, "weight": 70 },
    { "itemId": 13703, "quantity": 1, "avaIPeriod": -1, "status": true, "obtainTime": 0, "weight": 30 }
  ]
}
```

The server stores boxes created through this API in MySQL. `weight` is relative: in the example, the rewards have 70% and 30% chances. [`boxes.json`](src/main/resources/boxes.json) defines the initial box `18247`; a new box ID must not duplicate an existing one.

Creating a box defines its rewards but does not put the box in a player's inventory. Grant it through `POST /api/admin/players/{playerId}/inventory`:

```json
{ "itemId": 18248, "quantity": 10, "avaIPeriod": 0, "status": true, "obtainTime": 0 }
```

The player can then open one box per request:

```json
{ "playerItemId": 42, "requestId": "5b4273a9-e03d-4196-ae1b-51c282647e20" }
```

`playerItemId` is the ownership ID returned by inventory, not the box's client `itemId`. Use a new UUID for each opening; reuse the same UUID when retrying a request. The server consumes the box, grants the reward, and records the opening in one transaction.

### Admin access

New accounts are regular players. To grant admin access to an existing trusted account, update its row in MySQL:

```sql
UPDATE player SET is_admin = TRUE WHERE username = 'your-admin-account';
```

Roles are loaded from the database on each authenticated request. See [admin inventory grants](docs/admin-inventory.md) for request details.

## More details

- [Inventory behavior and item categories](docs/inventory.md)
- [Box opening and reward configuration](docs/boxes.md)
- [Admin inventory grants](docs/admin-inventory.md)
