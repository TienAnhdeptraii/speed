# Inventory API

All routes require `Authorization: Bearer <token>`. The owner is resolved from
the authenticated username; clients cannot supply a player ID.

| Method | Route | Response |
| --- | --- | --- |
| GET | `/api/players/me/inventory` | All owned records, including expired items; empty array for an empty inventory |
| GET | `/api/players/me/selected-car` | Usable selected car, or 204 if none is available |
| PUT | `/api/players/me/selected-car` | Select an owned car and return its inventory record |

Selection request: `{"playerItemId":42}`. Here 42 is the ownership record's `id`
from the inventory response, **not** the client item code.

Example response:

```json
{"id":42,"itemId":13703,"quantity":1,"avaIPeriod":-1,"status":true,"obtainTime":0,"expired":false,"selected":true}
```

`obtainTime` in API responses is the remaining duration in seconds. Following the entity's
period convention, `avaIPeriod=-1` means permanent, `0` means quantity-based,
and positive values mean timed. Timed items store an absolute `expiresAt` deadline;
their remaining seconds are calculated on each response, so offline time counts.
At or after the deadline they are expired. Negative granted seconds are invalid. `status` is passed
through unchanged; selection is still determined by `Player.selectedPlayerItemId`.
Reads do not update the database or restart the timer. No scheduler is needed.
The database's `obtain_time` stores the original granted seconds for timed items;
the API uses the deadline instead. Expired, depleted, or unrecognized cars cannot be selected.
An expired selection is hidden by the selected-car GET
and marked `selected:false` in inventory, without deleting its ownership history.
Future race-start logic must recheck availability at race start.

Missing/unowned records return 404; invalid selection or request fields return
400. Authentication uses the existing security configuration (currently 403 for
unauthenticated requests).

The server's car allowlist is configured in `application.properties`:

```properties
game.inventory.car-item-ids=13703,21901,12720,26931,24859
```

Client classification/display remains based on `itemId`. This server list only
controls whether an owned item can be equipped as a car; it does not grant cars.

`InventoryService.grantItem(playerId, itemId, quantity, avaIPeriod, status, obtainTime)` is an internal
entry point for future reward/purchase flows. There is no public grant endpoint.
Every grant creates a new record, preserving separate durations. Timed grants
require positive remaining seconds; permanent/quantity-based grants accept zero.
Cars are
granted individually. Other item quantities are stored without automatic merging;
stacking/use rules will be added when those item types are implemented. Callers
must handle reward/purchase idempotency in their transaction.

Registration grants one permanent car (13703) through the existing PlayerService
transaction, with `status=true` and `obtainTime=0`. It does not auto-select the car.
Existing accounts are not modified.
For local manual testing, after the application creates the schema, substitute
an existing test player's ID in this SQL (running it repeatedly grants duplicates):

```sql
INSERT INTO player_item (player_id, item_id, quantity, avai_period, status, obtain_time)
VALUES (YOUR_TEST_PLAYER_ID, 13703, 1, -1, TRUE, 0);
```

The existing `spring.jpa.hibernate.ddl-auto=update` setting creates `player_item`
and adds nullable `player.selected_player_item_id` at startup. No migration tool
was added. Selection locks the player and owned item within one transaction.
Existing databases created with the old expiry model may retain obsolete columns;
schema update adds the nullable `expires_at` column but cannot recover deadlines
for existing timed items that have only stored seconds. Such rows are treated as
unavailable until their actual deadlines are backfilled. No existing data is
automatically rewritten or granted a fresh duration. Permanent/quantity-based
items do not require a deadline.

New timed grants set `expires_at` to server time plus granted seconds. Direct
JPA inserts also initialize it through `@PrePersist` if the caller omitted it;
updates and reads never initialize it. Raw SQL inserts must supply the deadline
for timed items explicitly.

Run focused tests with:

```sh
mvn -Dtest=PlayerMeTests,InventoryServiceTests,InventoryControllerTests test
```
