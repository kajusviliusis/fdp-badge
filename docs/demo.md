# Demo walkthrough

Start the app with the demo profile, which loads 12 sample books on top of the normal migrations:

```bash
. ~/.bookworm/env
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

Swagger UI: <http://127.0.0.1:8080/api/v1/swagger-ui.html> (open without login). Base URL for curl: `http://127.0.0.1:8080/api/v1`.
Protected calls need the `accessToken` from step 1: in Swagger click **Authorize** and paste it; with curl send `-H "Authorization: Bearer $TOKEN"`.

## Sample catalog

| id | Title | Price | Notes |
|----|-------|-------|-------|
| 1 | The Hobbit | 299.00 | hardcover |
| 2 | Dune | 349.00 | |
| 3 | Wings of Fire | 199.00 | **out of stock** (try adding it to the cart) |
| 4 | Cosmos | 399.00 | |
| 5 | A Brief History of Time | 450.00 | |
| 6 | Joy of Minimalism | 149.00 | cheapest |
| 7 | Rich Dad Poor Dad | 275.00 | ebook |
| 8 | Atomic Habits | 499.00 | |
| 9 | Clean Code | 650.00 | |
| 10 | The Pragmatic Programmer | 720.00 | |
| 11 | Effective Java | 580.00 | |
| 12 | Java Concurrency in Practice | 610.00 | |

(Ids assume a fresh database.)

## 1. Register (or log in)

`POST /auth/register`
```json
{ "name": "Arjun Patel", "email": "arjun@example.com", "password": "S3cur3P@ss!" }
```
`POST /auth/login` takes `{ "email": "...", "password": "..." }`. Both return `{ "accessToken": "..." }`.

## 2. Browse books (no login)

| Request | Shows |
|---------|-------|
| `GET /books` | first page, most relevant/popular first |
| `GET /books?q=java` | full-text search: Effective Java, Java Concurrency in Practice |
| `GET /books?q=habit` | stemming: finds "Atomic Habits" |
| `GET /books?q=java -concurrency` | exclusion syntax |
| `GET /books?categoryId=4&sortBy=price_desc` | filter + sort |
| `GET /books?minPrice=200&maxPrice=400&format=PAPERBACK` | price range and format |
| `GET /books/8` | book detail |
| `GET /books/bestsellers` | this month's paid sales (empty until an order is paid) |
| `GET /books/new-launches` | newest books |

## 3. Add to cart

`POST /cart/items`
```json
{ "bookId": 1, "quantity": 2 }
```
then
```json
{ "bookId": 8, "quantity": 1 }
```
`GET /cart` returns subtotal 1097.00, tax 131.64 (12%), delivery 0.00 (free from ₹500), 3 items.
Errors worth showing: `bookId: 3` (out of stock, 400), `quantity: 0` (validation, 422).

## 4. Create a delivery address

`POST /addresses`
```json
{
  "firstName": "Arjun", "lastName": "Patel", "street": "12 MG Road", "city": "Mumbai",
  "state": "Maharashtra", "postalCode": "400001", "country": "India", "phone": "+919876543210"
}
```
Returns the new address `id` (use it as `addressId` below).

## 5. Place the order

`POST /orders`
```json
{ "addressId": 1, "couponCode": "SAVE100" }
```
Result: status `PENDING_PAYMENT`, subtotal 1097.00, discount 100.00, **total 1128.64** (1097 + 131.64 tax - 100 coupon). The cart is now empty and stock is reserved.

Coupons seeded: `SAVE100` (₹100 off, minimum subtotal ₹500) and `WELCOME10` (10% off, max ₹150). Codes are case-insensitive. Gift points can be combined: add `"giftPointsToRedeem": 50` (new users start with 0 points, so earn some first).

## 6. Get the order

`GET /orders/{orderId}` returns the same order; `GET /orders` lists your orders (`?status=CONFIRMED` filters).

## 7. Pay (simulated, nothing leaves the app)

`POST /orders/{orderId}/payment`

| Scenario | Body |
|----------|------|
| Approved card | `{ "method": "CREDIT_CARD", "cardDetails": { "cardNumber": "4111111111111111", "nameOnCard": "Arjun Patel", "cvv": "123", "expiryDate": "12/2030" } }` |
| Declined card (number ends in `0002`) | same, with `"cardNumber": "4111111111110002"` |
| UPI approved | `{ "method": "UPI", "upiDetails": { "upiId": "arjun@okbank" } }` |
| UPI declined (handle `fail`) | `{ "method": "UPI", "upiDetails": { "upiId": "arjun@fail" } }` |
| Wallet | `{ "method": "WALLET" }` |

A decline returns 200 with `"status": "FAILED"` and the order stays `PENDING_PAYMENT`, so you can retry. On success the order becomes `CONFIRMED`, the buyer earns gift points (1 per ₹10 paid, 112 here), and the book shows up in `GET /books/bestsellers`.

## 8. After payment

| Request | Shows |
|---------|-------|
| `GET /orders/{id}/payment` | the payment and its `TXN-...` reference |
| `GET /users/me/gift-points` | points balance |
| `POST /orders/{id}/cancel` | cancel within 48 hours: stock, points and the coupon use are given back |
| `POST /orders/{id}/buy-again` | puts the order's books back in the cart |
| `POST /books/{id}/reviews` | review a book you bought (`{ "rating": 5, "comment": "Loved it, a great read!" }`, comment 10-1000 chars) |
| `GET /books/{id}/related`, `GET /recommendations` | recommendations |
