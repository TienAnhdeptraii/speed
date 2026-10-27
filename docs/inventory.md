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
{"id":42,"itemId":13703,"category":"VEHICLE","subcategory":"S","quantity":1,"avaIPeriod":-1,"status":true,"obtainTime":0,"expired":false,"selected":true}
```

`obtainTime` in API responses is the remaining duration in seconds. Following the entity's
period convention, `avaIPeriod=-1` means permanent, `0` means quantity-based,
and positive values mean timed. Timed items store an absolute `expiresAt` deadline;
their remaining seconds are calculated on each response, so offline time counts.
At or after the deadline they are expired. Negative granted seconds are invalid. `status` is passed
through unchanged; selection is still determined by `Player.selectedPlayerItemId`.
New records require a category. Existing rows get a nullable `category` database
column during rollout; API responses infer `VEHICLE` for configured car IDs and
`ITEM` for other unclassified legacy rows. Vehicle subcategories are loaded from
`items.json` and copied into newly granted records. The catalog supports
`vehicleCategory`, `outfitCategory`, `runeCategory`, `effectCategory`,
`utilityCategory`, and `petCategory`; each must match the item's main category.
The inventory API returns the matching value as one `subcategory` field.
Existing rows with null subtypes are resolved from that catalog in API responses.
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
Quantity-based grants (`avaIPeriod=0`) merge into the oldest compatible ownership
record, except for known car IDs. Compatibility means the same owner, itemId,
status and obtainTime, with no expiration timestamp. Existing compatible duplicate
rows are folded into that stack when the next grant arrives. Different statuses
and quantity-based versus duration-based ownership remain separate.
Timed/permanent ownership with the same owner, itemId and status merges into one
record. Timed rewards extend the existing deadline (or start from now if expired).
A permanent reward upgrades timed ownership; existing permanent ownership stays
permanent for any further timed/permanent reward. Legacy remaining durations are
combined, but any permanent duplicate takes precedence. The equipped ownership ID
is preserved during consolidation. Missing legacy deadlines contribute no time.
Timed/permanent grants require quantity 1; timed grants require positive seconds.
The returned inventory DTO contains the total quantity or remaining duration.
Grants lock the owner before looking up stacks, including the first grant when no
stack exists, and reject totals exceeding Integer.MAX_VALUE with 409. Callers
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
