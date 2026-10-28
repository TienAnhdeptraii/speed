# Opening boxes

`POST /api/players/me/boxes/open`, authenticated with a Bearer JWT:

```json
{"playerItemId":42,"requestId":"5b4273a9-e03d-4196-ae1b-51c282647e20"}
```

`playerItemId` is the ownership ID from inventory, not the client item code.
Each call opens one box and awards one configured reward entry. Generate a new
UUID for each intentional opening, and reuse that same UUID for retries.

The receipt contains the opening ID, request ID, box ownership/client IDs,
remaining box quantity at opening, reward ownership ID and granted fields,
and opening time. Retrying returns the original receipt even if the last box
has been removed or the reward table has changed. Granted seconds in receipts
are historical; query inventory for a timed reward's current remaining seconds.
Reusing a key for a different box returns 409. Missing/unowned boxes return 404;
expired or unconfigured boxes return 400.

The transaction locks the player then the owned box, consumes one unit, grants
the reward through InventoryService and writes `box_opening`. Failure rolls all
changes back. A unique player/request key and per-player lock prevent duplicate
rewards. Keep the receipt history for as long as clients may retry old requests.
Exhausted box records are deleted because inventory quantities must be positive.

Quantity-based rewards (`avaIPeriod=0`, excluding known cars) are stacked by
InventoryService when their metadata matches. A receipt always reports the amount
awarded by that opening, not the stack's new total. For example, winning 5 engines
when the stack has 10 returns reward quantity 5, while inventory now shows 15.
Retrying the receipt does not increment the stack again. Legacy duplicate stack
IDs in older receipts remain historical references after stacks are consolidated.

Timed rewards extend compatible existing ownership: 3 remaining days plus a
7-day reward becomes 10 days. Expired ownership restarts from the grant time.
Permanent rewards upgrade timed ownership, and permanent ownership stays permanent
when won again. Receipts still show the prize for that opening; retries never add
time again. The inventory response shows the resulting total duration.

## Reward configuration

Admins can create a new box with `POST /api/admin/boxes` and a Bearer JWT from
an account with the `ADMIN` role. For example:

```json
{
  "itemId": 18248,
  "rewards": [
    { "itemId": 13703, "quantity": 1, "avaIPeriod": 1, "status": true, "obtainTime": 259200, "weight": 70 },
    { "itemId": 13703, "quantity": 1, "avaIPeriod": -1, "status": true, "obtainTime": 0, "weight": 30 }
  ]
}
```

The API returns `201` with the saved box and rewards. An existing box ID returns
`409`. Invalid or empty reward lists return `400`. Definitions created through
this API are stored in `box_definition` and `box_definition_reward` and can be
opened immediately, including after a restart. The box item is not granted to
any player by creation; use the admin inventory grant API for that. Box IDs
already present in `boxes.json` cannot be created again.

`GET /api/admin/boxes` lists the box definitions and their reward tables from
both the startup JSON file and the database, ordered by box item ID. It also
requires an admin JWT.

`PUT /api/admin/boxes/{itemId}` replaces the full reward table. Send the same
JSON shape as `POST`, with `itemId` matching the URL. The box ID cannot be
changed. Updating a box from `boxes.json` creates a database override, so the
new table takes effect without editing the source file. The original JSON
remains in place; later edits replace the override.

The Swing app at `app-desktop/BoxAdminDesktopApp.java` provides a form for
creating and editing boxes. Run `app-desktop/run-box-admin.cmd` on Windows. It
shows the calculated probability for each reward based on its weight. Sign in
there with an admin account.

Box **18247** is enabled in `src/main/resources/boxes.json` under `boxes.18247`.
It grants one permanent car: 13703, 21901, 12720, 26931 or 24859, each with
weight 20 (20% probability). Each reward is categorized as `VEHICLE`, with quantity 1, period -1, status true
and obtainTime 0. Duplicate permanent cars reuse the existing ownership. This configuration
does not grant boxes to players automatically.

Each box ID maps to an array of possible rewards. Each entry needs only `itemId`;
inventory classifies the item by its ID when granting it. The same itemId can appear
multiple times with different durations and weights. Example table (not the
active table; item 999 below is an illustrative quantity-item ID):

```json
{
  "boxes": {
    "18247": [
      { "itemId": 13703, "quantity": 1, "avaIPeriod": 1, "status": true, "obtainTime": 259200, "weight": 40 },
      { "itemId": 13703, "quantity": 1, "avaIPeriod": 1, "status": true, "obtainTime": 604800, "weight": 30 },
      { "itemId": 13703, "quantity": 1, "avaIPeriod": -1, "status": true, "obtainTime": 0, "weight": 10 },
      { "itemId": 999, "quantity": 5, "avaIPeriod": 0, "status": true, "obtainTime": 0, "weight": 20 }
    ]
  }
}
```

Probability is weight divided by the sum of weights (40%, 30%, 10%, 20% above).
Weights must be positive integers. Permanent rewards use period -1; quantity
items use 0; timed rewards use a positive period and positive granted seconds.
All rewards are validated before consuming a box. Cars must have quantity 1.
The JSON file is also validated at startup. Missing files, malformed JSON, unknown
or missing reward fields, and invalid values stop startup instead of silently
loading a partial table. All six fields are required for each reward.

`application.properties` now contains only the file location for box configuration:

```properties
game.box.config=classpath:boxes.json
```

To edit a file outside the packaged application, use for example
`game.box.config=file:./config/boxes.json` and create that file relative to the
server working directory. Restart the server after edits. Classpath edits require
rebuilding/copying resources before restarting a packaged application. The former
`game.box.rewards[...]` property entries are no longer used. Known car IDs come
from `game.inventory.car-item-ids`; known rune IDs go in
`game.inventory.rune-item-ids`. For other known IDs, set
`items.json` maps an item ID to its category and optional subtype. For example,
`"13703": { "category": "VEHICLE", "vehicleCategory": "S" }` identifies an
S-class vehicle. Other categories can use `outfitCategory`, `runeCategory`,
`effectCategory`, `utilityCategory`, or `petCategory`. Unmapped IDs default to `ITEM`.

Only inventory item rewards are supported here; money rewards and batch opening
are not implemented. Trusted game flows can grant boxes via InventoryService;
players cannot grant themselves boxes through a public API.

The existing Hibernate schema-update setting creates the history table on startup.
Run tests with `mvn test`.
