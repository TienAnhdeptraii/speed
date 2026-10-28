# Admin inventory grants

Authenticated admins can grant an item to a player's inventory:

`GET /api/admin/items` lists all IDs configured in `items.json` or inventory
properties, with their category and optional subcategory. This is useful for
selecting an item in an admin form. IDs absent from the catalog can still be
granted and are classified as `ITEM` by the current inventory rules.

```http
POST /api/admin/players/123/inventory
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "itemId": 18247,
  "quantity": 10,
  "avaIPeriod": 0,
  "status": true,
  "obtainTime": 0
}
```

`itemId` determines `category` and `subcategory` through `items.json`. Quantity
items (`avaIPeriod=0`) stack with compatible items already owned. Timed items
(`avaIPeriod>0`) take `obtainTime` in seconds. Permanent items use `-1` and `0`.
The response is the resulting inventory entry; for a stack it reports the new
total quantity, and for timed ownership it reports the resulting remaining time.
Unknown players return 404; invalid item fields return 400.

The role comes from `player.is_admin`. New accounts default to non-admin. Promote
an existing trusted admin account directly in MySQL:

```sql
UPDATE player SET is_admin = TRUE WHERE username = 'your-admin-account';
```

The account can use its existing valid JWT after promotion because authorities
are loaded from the database on each request. Requests without an admin role receive 403. The controller calls the same
transactional grant logic used by box rewards, so stacking and duration merging
stay consistent.
