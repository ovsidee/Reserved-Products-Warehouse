# Reserved Products Warehouse

A Spring Boot REST service designed to handle product reservations.



---



## Architecture & Design

### Compare-And-Swap (CAS)
Implemented a custom **Compare-And-Swap (CAS)** mechanism using pure SQL within the repository layer `ItemInventoryRepository` to achieve optimistic locking.

* **Mechanism:** When updating an `InventoryItem`, the SQL query is constructed to only apply the changes if the `version` field in the database **matches** the `version` read initially by the service.
* **Implementation:** This is implemented in `ItemInventoryRepository.tryUpdate()` using a custom `@Query` with the `WHERE sku = :sku AND version = :version` clause. The `version` is incremented by the database only after a successful update.

### Transactions
To keep inventory updates and event creation atomic, without holding long database transactions that hurt concurrency, used:

* The `InventoryService.reserve()` method is not wrapped in a single large transaction.
* Only the critical part verifying the CAS update and saving the event is executed inside a transaction using **`TransactionTemplate`**.
* This guarantees that if the CAS update succeeds, the `ItemReservedEvent` is also saved, or the entire operation rolls back.

### Retry Mechanism
If the CAS update fails (meaning another transaction updated the version first), the application detects the failure `updated == 0` and enters a retry loop.

* **Logic:** The service retries the entire process (fetch item, check stock, attempt CAS) up to a **maximum of 3 times**.
* **Failure:** If all three attempts fail, a `ConflictException` is thrown, resulting in an `HTTP 409 Conflict` response.

### Code Architecture
The following code architecture was used:

* **web (Controller layer):** Handles incoming HTTP requests and maps exceptions to HTTP status codes `GlobalExceptionHandler`.
* **service (Business Layer):** Contains the core logic. It handles the retry logic, manages transactions, and saves data.
* **repository (Data Access Layer):** Talk to the database. This is where the custom SQL query for safe updates (CAS) is written.
* **entity (Domain Model):** Represents the database tables `InventoryItem`, `ItemReservedEvent`.
* **dto (Data Transfer Objects):** Simple objects used just for sending data in and out of the API, so we don't expose our database entities directly.
* **exception:** Custom error classes that we throw when something goes wrong (e.g., `InsufficientStockException`).



---



## Tech Stack I used

* **Language:** Java 21
* **Framework:** Spring Boot 4 
* **Persistence:** Spring Data JPA (Hibernate)
* **Database:** PostgreSQL 
* **Migration:** Flyway
* **Concurrency:** Optimistic Locking (CAS) + retry mechanism
* **Build Tool:** Maven
* **Testing:** Mockito, JUnit5



---



## How to run the application?

### Prerequisites
* Java 21 installed.
* Docker & Docker Compose are installed (for the database).
* Maven.

### 1. Running the Database
The project uses PostgreSQL defined in `docker-compose.yml`.

1.  Open your terminal in the project **root directory** (where `pom.xml` is located).
2.  Start the database container:

```bash
  docker compose up -d
```

3.  Ensure the container `reserved_products_db` is running.

### 2. Running the Application
Once the database is up, open your terminal in the project **root directory** (where `pom.xml` is located) and start the application

```bash
  ./mvnw spring-boot:run
```

Upon startup, the application will automatically run migration scripts. The `inventory_item` table will be seeded with the following records:
```text
'Iphone-17-pro-max', 50 available, 10 reserved, 1 version,
'Logitech-G-Pro-X', 120 available, 0 reserved, 1 version,
'NVIDIA-RTX-4070', 30 available, 2 reserved, 1 version,
'AMD-Ryzen-7-5800x', 5 available, 0 reserved, 1 version,
'Playstation-5', 250 available, 10 reserved, 1 version
```

### 2.1) Running Tests 
To run all tests for the service and controller classes. Run this command (from **root directory**):

```bash 
  ./mvnw test
```

If you want to run explicit service tests, use:

```bash
  ./mvnw test -Dtest=InventoryServiceTest
```

and for controller:

```bash
  ./mvnw test -Dtest=InventoryControllerTest
```



---



## API Usage (Curl for Git Bash)
### All curl examples below are intended to be used in Git Bash Windows, Linux, or macOS. If you're using Windows PowerShell, the syntax is different.

Primary endpoint for reservation is `POST /inventory/{sku}/reserve`.

### 1. Successful Reservation (200 OK)
Reserves 5 units of "Iphone-17-pro-max"

```
curl -X POST http://localhost:8080/inventory/Iphone-17-pro-max/reserve \
-H "Content-Type: application/json" \
-d '{"qty": 5}'
```
Response: `Item reserved successfully`

---

### 2. Item Not Found (404 NOT FOUND)
Attempts to reserve an item that does not exist in the database.

```
curl -X POST http://localhost:8080/inventory/notExistingSKU/reserve \
-H "Content-Type: application/json" \
-d '{"qty": 1}'
```

Response: `json {"error": "Provided item: notExistingSKU not found."}`

---

### 3. Concurrency Conflict (409 CONFLICT)
This is triggered if the CAS fails 3 times due to concurrent updates on the same SKU. Requires simulating concurrent load to reliably trigger.
```
curl -X POST http://localhost:8080/inventory/AMD-Ryzen-7-5800x/reserve \
-H "Content-Type: application/json" \
-d '{"qty": 1}'
```

Response: `{"error": "Could not reserve item due to concurrent updates (sku: AMD-Ryzen-7-5800x)"}`

---

### 4. Invalid Quantity (400 BAD REQUEST)
Occurs when qty is 0 or any negative number.

Example with `qty = 0:`
```
curl -X POST http://localhost:8080/inventory/Iphone-17-pro-max/reserve \
-H "Content-Type: application/json" \
-d '{"qty": 0}'
```

Example with `qty = -5:`
```
curl -X POST http://localhost:8080/inventory/Iphone-17-pro-max/reserve \
-H "Content-Type: application/json" \
-d '{"qty": -5}'
```

Response for both:
`{"error": "qty must be > 0"}`
