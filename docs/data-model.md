# Bookstore — Database Design

## Entities

| Entity | Attributes | 
|---|---|
| **User** | id (PK), name, email, password_hash, gift_points |
| **Category** | id (PK), name |
| **Publisher** | id (PK), name |
| **Book** | id (PK), title, author, description, price, stock_quantity, estimated_delivery_days, category_id (FK), publisher_id (FK) |
| **Address** | id (PK), user_id (FK), street, city, postal_code, country |
| **Cart** | id (PK), user_id (FK) |
| **CartItem** | id (PK), cart_id (FK), book_id (FK), quantity |
| **Order** | id (PK), user_id (FK), address_id (FK), status, total_amount, created_at |
| **OrderItem** | id (PK), order_id (FK), book_id (FK), quantity, unit_price |
| **Payment** | id (PK), order_id (FK), method, status, amount |

## Relationships

- **User → Address:** One-to-many (one user can have multiple delivery addresses).
- **User → Cart:** One-to-one (one active cart per user).
- **Cart → CartItem:** One-to-many.
- **Book → CartItem:** One-to-many.
- **Category → Book:** One-to-many.
- **Publisher → Book:** One-to-many.
- **User → Order:** One-to-many.
- **Address → Order:** One-to-many.
- **Order → OrderItem:** One-to-many.
- **Book → OrderItem:** One-to-many.
- **Order → Payment:** One-to-one (one payment record per order).

## Required Features

- User login and authentication.
- Browse the book catalog and view book details.
- Search books and filter by category or publisher.
- View book availability and estimated delivery dates.
- View related books and recommendations based on order history.
- Add books to cart, update quantities, and remove items.
- Select a delivery address and payment method at checkout.
- Redeem gift points during checkout.
- Complete a purchase and receive order confirmation.
- View order history and buy previously ordered books again.
- Cancel an order within 48 hours.
