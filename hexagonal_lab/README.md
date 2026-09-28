# Orders: Hexagonal Architecture example

Java 21 + Spring Boot 3 + PostgreSQL. Package layout follows the supplied image: domain, application ports/use case, inbound web adapter, outbound persistence adapter, config and Flyway resources.

## Run

Create a PostgreSQL database named `orders` and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` if your local credentials differ from the defaults. Then run `mvn spring-boot:run`.

## Example request

```http
POST /api/orders
Content-Type: application/json

{
  "customerId": "customer-123",
  "currency": "USD",
  "items": [
    { "sku": "BOOK-001", "quantity": 2, "unitPrice": 12.50 },
    { "sku": "PEN-002", "quantity": 3, "unitPrice": 1.25 }
  ]
}
```

The response contains the generated order id and calculated total. The web adapter depends on the inbound use-case port; the use case depends on the outbound repository port; the JPA adapter implements that port. Domain code has no Spring or persistence imports.
