INSERT INTO authors (id, name)
SELECT X, 'Author ' || X FROM SYSTEM_RANGE(1, 20);

INSERT INTO books (id, title, publication_year, author_id)
SELECT X, 'Book ' || X, 2010 + MOD(X, 20), MOD(X - 1, 20) + 1
FROM SYSTEM_RANGE(1, 1000);
