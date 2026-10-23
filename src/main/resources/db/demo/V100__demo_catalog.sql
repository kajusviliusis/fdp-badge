-- Demo data, loaded only with the "demo" Spring profile (see application-demo.yml). Never part of a production run.

INSERT INTO categories (name) VALUES ('Fiction'), ('Science'), ('Self-Help'), ('Technology');

INSERT INTO publishers (name) VALUES ('Penguin Books'), ('HarperCollins'), ('Pragmatic Bookshelf'), ('ABC Publishers');

INSERT INTO books (title, author, isbn, description, price, format, language, stock_quantity,
                   estimated_delivery_days, average_rating, total_reviews, copies_sold,
                   category_id, publisher_id, created_at, version)
SELECT d.title, d.author, d.isbn, d.description, d.price, d.format, d.language, d.stock,
       d.delivery_days, 0, 0, d.copies_sold, c.id, p.id, now() - (d.age_days || ' days')::interval, 0
FROM (VALUES
    ('Dune',                          'Frank Herbert',  '9780441013593', 'A desert planet, a prophecy and the spice that rules an empire.', 349.00, 'PAPERBACK', 'English', 40, 3, 120, 400, 'Fiction',    'Penguin Books'),
    ('The Hobbit',                    'J.R.R. Tolkien', '9780547928227', 'Bilbo Baggins is swept into a quest to reclaim a dragon''s treasure.', 299.00, 'HARDCOVER', 'English', 25, 4, 95, 300, 'Fiction', 'HarperCollins'),
    ('Joy of Minimalism',             'Daniel Reed',    '9781234567890', 'Declutter your home and your mind.', 149.00, 'PAPERBACK', 'English', 60, 2, 40, 30, 'Self-Help', 'ABC Publishers'),
    ('Atomic Habits',                 'James Clear',    '9780735211292', 'Tiny changes, remarkable results.', 499.00, 'PAPERBACK', 'English', 80, 2, 210, 200, 'Self-Help', 'Penguin Books'),
    ('Cosmos',                        'Carl Sagan',     '9780345539434', 'A personal voyage through the universe.', 399.00, 'PAPERBACK', 'English', 30, 3, 70, 150, 'Science', 'HarperCollins'),
    ('A Brief History of Time',       'Stephen Hawking','9780553380163', 'From the Big Bang to black holes.', 450.00, 'HARDCOVER', 'English', 20, 4, 85, 250, 'Science', 'Penguin Books'),
    ('Clean Code',                    'Robert C. Martin','9780132350884', 'A handbook of agile software craftsmanship.', 650.00, 'PAPERBACK', 'English', 15, 5, 60, 180, 'Technology', 'Pragmatic Bookshelf'),
    ('The Pragmatic Programmer',      'Andrew Hunt',    '9780135957059', 'Your journey to mastery.', 720.00, 'PAPERBACK', 'English', 12, 5, 55, 120, 'Technology', 'Pragmatic Bookshelf'),
    ('Effective Java',                'Joshua Bloch',   '9780134685991', 'Best practices for the Java platform.', 580.00, 'PAPERBACK', 'English', 18, 5, 45, 90, 'Technology', 'Pragmatic Bookshelf'),
    ('Java Concurrency in Practice',  'Brian Goetz',    '9780321349606', 'Writing correct concurrent programs.', 610.00, 'PAPERBACK', 'English', 10, 5, 35, 60, 'Technology', 'Pragmatic Bookshelf'),
    ('Rich Dad Poor Dad',             'Robert Kiyosaki','9781612680194', 'What the rich teach their kids about money.', 275.00, 'EBOOK', 'English', 500, 1, 150, 500, 'Self-Help', 'ABC Publishers'),
    ('Wings of Fire',                 'A.P.J. Abdul Kalam','9788173711466', 'An autobiography of India''s missile man.', 199.00, 'PAPERBACK', 'English', 0, 7, 110, 10, 'Science', 'ABC Publishers')
) AS d(title, author, isbn, description, price, format, language, stock, delivery_days, copies_sold, age_days, category, publisher)
JOIN categories c ON c.name = d.category
JOIN publishers p ON p.name = d.publisher;
