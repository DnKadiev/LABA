# Cinema Booking System

REST API на Spring Boot для управления каталогом фильмов, залами, сеансами и продажей билетов в кинотеатре.

Система покрывает:
- каталог фильмов;
- расписание сеансов по залам;
- покупку и возврат билетов;
- контроль вместимости зала;
- JWT-аутентификацию с access/refresh токенами;
- HTTPS-конфигурацию через локальный keystore.

## Технологии

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security + JWT
- PostgreSQL
- H2 для тестов
- Maven

## Предметная область

### Customer
Покупатель билетов.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Идентификатор |
| fullName | String | ФИО клиента |
| email | String | Email, уникальный |
| phone | String | Телефон, уникальный |
| createdAt | LocalDateTime | Дата создания |

### Movie
Фильм, доступный для показа.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Идентификатор |
| title | String | Название |
| genre | String | Жанр |
| durationMinutes | Integer | Длительность в минутах |
| ageRating | String | Возрастной рейтинг |
| description | String | Описание |
| releaseDate | LocalDate | Дата релиза |
| baseTicketPrice | BigDecimal | Базовая цена билета |
| active | boolean | Активен ли фильм в репертуаре |

### Hall
Кинозал с ограниченной вместимостью.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Идентификатор |
| name | String | Название зала, уникальное |
| capacity | Integer | Вместимость |
| premium | boolean | Признак VIP/премиального зала |
| createdAt | LocalDateTime | Дата создания |

### Screening
Конкретный показ фильма в конкретном зале.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Идентификатор |
| movie | Movie | Фильм |
| hall | Hall | Зал |
| startTime | LocalDateTime | Время начала |
| endTime | LocalDateTime | Время окончания |
| ticketPrice | BigDecimal | Цена билета для сеанса |
| language | String | Язык показа |
| formatType | String | Формат показа: 2D, IMAX и т.д. |
| createdAt | LocalDateTime | Дата создания |
| updatedAt | LocalDateTime | Дата изменения |

### Ticket
Билет на сеанс, связанный с покупателем.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Идентификатор |
| screening | Screening | Сеанс |
| customer | Customer | Покупатель |
| seatNumber | Integer | Номер места |
| paidPrice | BigDecimal | Оплаченная цена |
| bookingCode | String | Уникальный код билета |
| status | Enum | `PURCHASED` или `REFUNDED` |
| purchasedAt | LocalDateTime | Время покупки |
| refundedAt | LocalDateTime | Время возврата |

## Бизнес-правила

Система реализует следующие правила:

1. Фильм показывается в конкретном зале в конкретный сеанс.
2. Билет всегда относится к одному сеансу и одному покупателю.
3. Количество активных билетов на сеанс не может превышать вместимость зала.
4. Номер места не может выходить за пределы вместимости зала.
5. Одно место нельзя продать дважды на один и тот же сеанс, пока билет не возвращён.
6. Возврат билета возможен только до начала сеанса.
7. В одном зале нельзя создать пересекающиеся по времени сеансы.

## Роли

- `ROLE_ADMIN` — полный доступ ко всем ресурсам.
- `ROLE_MANAGER` — управление клиентами, фильмами, залами, сеансами и операциями кинотеатра.
- `ROLE_CUSTOMER` — просмотр каталога, покупка и возврат билетов.

## Базовый URL

По умолчанию приложение стартует без SSL:

`http://localhost:8080`

HTTPS включается отдельно, только если у вас есть `keystore.p12` или вы сгенерировали его через `generate-certs.sh`.

## Аутентификация

### POST `/api/auth/register`

Пример тела:

```json
{
  "username": "manager2",
  "password": "Cinema1234!",
  "role": "ROLE_MANAGER"
}
```

### POST `/api/auth/login`

```json
{
  "username": "admin",
  "password": "Admin1234!"
}
```

Ответ:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### POST `/api/auth/refresh`

```json
{
  "refreshToken": "..."
}
```

## Основные REST-ресурсы

### Customers `/api/customers`

| Метод | Путь | Роли |
|------|------|------|
| GET | `/api/customers` | ADMIN, MANAGER |
| GET | `/api/customers/{id}` | ADMIN, MANAGER |
| POST | `/api/customers` | ADMIN, MANAGER |
| PUT | `/api/customers/{id}` | ADMIN, MANAGER |
| DELETE | `/api/customers/{id}` | ADMIN |

### Movies `/api/movies`

| Метод | Путь | Роли |
|------|------|------|
| GET | `/api/movies` | Любой авторизованный |
| GET | `/api/movies/active` | Любой авторизованный |
| GET | `/api/movies/{id}` | Любой авторизованный |
| POST | `/api/movies` | ADMIN, MANAGER |
| PUT | `/api/movies/{id}` | ADMIN, MANAGER |
| DELETE | `/api/movies/{id}` | ADMIN |

### Halls `/api/halls`

| Метод | Путь | Роли |
|------|------|------|
| GET | `/api/halls` | Любой авторизованный |
| GET | `/api/halls/{id}` | Любой авторизованный |
| POST | `/api/halls` | ADMIN, MANAGER |
| PUT | `/api/halls/{id}` | ADMIN, MANAGER |
| DELETE | `/api/halls/{id}` | ADMIN |

### Screenings `/api/screenings`

| Метод | Путь | Роли |
|------|------|------|
| GET | `/api/screenings` | Любой авторизованный |
| GET | `/api/screenings/{id}` | Любой авторизованный |
| GET | `/api/screenings/movie/{movieId}` | Любой авторизованный |
| GET | `/api/screenings/hall/{hallId}` | Любой авторизованный |
| GET | `/api/screenings/date/{date}` | Любой авторизованный |
| POST | `/api/screenings` | ADMIN, MANAGER |
| PUT | `/api/screenings/{id}` | ADMIN, MANAGER |
| DELETE | `/api/screenings/{id}` | ADMIN |

Пример создания сеанса:

```json
{
  "movieId": 1,
  "hallId": 1,
  "startTime": "2030-06-15T19:00:00",
  "ticketPrice": 720.00,
  "language": "RU",
  "formatType": "IMAX"
}
```

### Tickets `/api/tickets`

| Метод | Путь | Роли |
|------|------|------|
| GET | `/api/tickets` | Любой авторизованный |
| GET | `/api/tickets/{id}` | Любой авторизованный |
| GET | `/api/tickets/customer/{customerId}` | Любой авторизованный |
| GET | `/api/tickets/screening/{screeningId}` | Любой авторизованный |
| DELETE | `/api/tickets/{id}` | ADMIN |

## Бизнес-операции

### 1. Покупка билета

`POST /api/screenings/{id}/tickets/purchase`

Пример:

```json
{
  "customerId": 1,
  "seatNumber": 14,
  "paidPrice": 720.00
}
```

Правила:
- сеанс должен ещё не начаться;
- место должно попадать в диапазон вместимости зала;
- место не должно быть уже занято;
- число проданных активных билетов не должно превышать вместимость.

### 2. Возврат билета

`POST /api/tickets/{id}/refund`

Возврат разрешён только до `startTime` связанного сеанса.

### 3. Отчёт по заполненности сеанса

`GET /api/screenings/{id}/occupancy`

Ответ содержит:
- вместимость зала;
- число проданных билетов;
- число возвращённых билетов;
- количество свободных мест;
- процент заполненности.

### 4. Расписание зала за период

`GET /api/halls/{id}/schedule?from=2030-06-15&to=2030-06-20`

Возвращает список сеансов в выбранном зале с количеством проданных и свободных мест.

### 5. Доступные будущие сеансы фильма

`GET /api/movies/{id}/available-screenings`

Возвращаются только будущие сеансы, в которых ещё есть свободные места.

## Демоданные

При первом старте приложение создаёт:

### Пользователей

| Логин | Пароль | Роль |
|------|--------|------|
| admin | Admin1234! | ROLE_ADMIN |
| manager1 | Cinema1234! | ROLE_MANAGER |
| customer1 | Cust1234! | ROLE_CUSTOMER |

### Данные кинотеатра

- 3 клиента
- 3 фильма
- 3 зала
- 5 сеансов
- 7 билетов, включая возвращённый билет

## Локальный запуск

### 1. PostgreSQL

Создайте БД:

```sql
CREATE DATABASE cinema_booking_db;
```

### 2. Переменные окружения

Используйте `.env.example` или задайте переменные вручную:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cinema_booking_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SSL_ENABLED=false
SERVER_PORT=8080
```

### 3. Запуск

```bash
./mvnw spring-boot:run
```

Для Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Тесты

Тестовый профиль использует in-memory H2, поэтому проверка не зависит от локальной PostgreSQL:

```bash
./mvnw test
```

## Postman

В репозитории лежит готовая коллекция:

`postman_collection.json`

Коллекция содержит:
- логин всех ролей;
- CRUD по Customer, Movie, Hall, Screening;
- запросы на просмотр Ticket;
- покупку и возврат билета;
- отчёты по заполненности и расписанию.

По умолчанию переменная `baseUrl` в коллекции указывает на `http://localhost:8080`.

## HTTPS и сертификаты

Скрипт `generate-certs.sh` создаёт локальную цепочку сертификатов и keystore:

```bash
chmod +x generate-certs.sh
./generate-certs.sh
```

После этого можно включить HTTPS:

```bash
SSL_ENABLED=true SERVER_PORT=8443 SSL_KEY_STORE_PASSWORD=changeit ./mvnw spring-boot:run
```
