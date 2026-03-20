# Система управления автосервисом (Auto Service Management System)

REST API на Spring Boot для управления заказ-нарядами автосервиса. Система позволяет вести учёт клиентов, автомобилей, механиков, запчастей и заказ-нарядов с полным контролем жизненного цикла каждого заказа.

---

## Технологический стек

- **Java 21**
- **Spring Boot 3.x**
- **Spring Security** + **JWT** (access + refresh токены с ротацией)
- **Spring Data JPA** / **Hibernate**
- **PostgreSQL 16**
- **Maven**
- **HTTPS / TLS** (самоподписанный сертификат)

---

## Сущности предметной области

### Customer (Клиент)
Владелец автомобиля.

| Поле       | Тип             | Описание                        |
|------------|-----------------|---------------------------------|
| id         | Long            | Идентификатор                   |
| name       | String          | ФИО клиента (обязательно)       |
| phone      | String (unique) | Телефон                         |
| email      | String (unique) | Email                           |
| createdAt  | LocalDateTime   | Дата создания записи            |

### Vehicle (Автомобиль)
Принадлежит клиенту.

| Поле         | Тип             | Описание                        |
|--------------|-----------------|---------------------------------|
| id           | Long            | Идентификатор                   |
| customer     | Customer (FK)   | Владелец                        |
| make         | String          | Марка (напр. Toyota)            |
| model        | String          | Модель (напр. Camry)            |
| year         | Integer         | Год выпуска                     |
| licensePlate | String (unique) | Государственный номер           |
| vin          | String (unique) | VIN-номер                       |
| createdAt    | LocalDateTime   | Дата создания записи            |

### Mechanic (Механик)
Сотрудник автосервиса.

| Поле           | Тип           | Описание                          |
|----------------|---------------|-----------------------------------|
| id             | Long          | Идентификатор                     |
| name           | String        | ФИО (обязательно)                 |
| specialization | String        | Специализация (напр. Двигатель)   |
| active         | boolean       | Активен ли сотрудник              |
| createdAt      | LocalDateTime | Дата создания записи              |

### Part (Запчасть)
Каталог запасных частей.

| Поле          | Тип             | Описание                  |
|---------------|-----------------|---------------------------|
| id            | Long            | Идентификатор             |
| name          | String          | Наименование              |
| partNumber    | String (unique) | Артикул                   |
| price         | BigDecimal      | Цена                      |
| stockQuantity | Integer         | Количество на складе      |
| createdAt     | LocalDateTime   | Дата создания записи      |

### ServiceOrder (Заказ-наряд)
Основная сущность — связывает автомобиль, механика и перечень работ/запчастей.

| Поле        | Тип              | Описание                              |
|-------------|------------------|---------------------------------------|
| id          | Long             | Идентификатор                         |
| vehicle     | Vehicle (FK)     | Автомобиль                            |
| mechanic    | Mechanic (FK)    | Назначенный механик (может быть null) |
| status      | OrderStatus enum | Статус заказа                         |
| totalCost   | BigDecimal       | Итоговая стоимость (авторасчёт)       |
| description | String           | Описание проблемы/работ               |
| items       | List<OrderItem>  | Позиции заказа                        |
| createdAt   | LocalDateTime    | Дата создания                         |
| updatedAt   | LocalDateTime    | Дата последнего изменения             |
| closedAt    | LocalDateTime    | Дата закрытия                         |

### OrderItem (Позиция заказа)
Строка в заказ-наряде: работа или запчасть.

| Поле         | Тип           | Описание                              |
|--------------|---------------|---------------------------------------|
| id           | Long          | Идентификатор                         |
| serviceOrder | ServiceOrder  | Заказ-наряд (FK)                      |
| type         | ItemType enum | WORK (работа) или PART (запчасть)     |
| description  | String        | Описание позиции                      |
| part         | Part (FK)     | Ссылка на запчасть (для type=PART)    |
| quantity     | Integer       | Количество                            |
| unitPrice    | BigDecimal    | Цена за единицу                       |
| mandatory    | boolean       | Обязательная работа                   |
| completed    | boolean       | Выполнена ли позиция                  |

---

## Машина состояний заказа

```
OPEN ──────────► IN_PROGRESS ──────► COMPLETED ──────► CLOSED (терминальный)
  │                   │                   │
  └──────────────────►└──────────────────►└──► IN_PROGRESS (возврат)
  │                   │
  └──► CANCELLED ◄────┘  (терминальный)
```

| Из            | В             | Условие                                    |
|---------------|---------------|--------------------------------------------|
| OPEN          | IN_PROGRESS   | Назначен механик                           |
| OPEN          | CANCELLED     | Отмена клиентом                            |
| IN_PROGRESS   | COMPLETED     | Все работы завершены                       |
| IN_PROGRESS   | CANCELLED     | Отмена                                     |
| COMPLETED     | CLOSED        | Все обязательные позиции выполнены         |
| COMPLETED     | IN_PROGRESS   | Возврат в работу (доработка)               |

---

## API Эндпоинты

Базовый URL: `https://localhost:8443`

### Аутентификация `/api/auth`

| Метод | Путь               | Доступ | Описание                                  |
|-------|--------------------|--------|-------------------------------------------|
| POST  | /api/auth/register | Все    | Регистрация нового пользователя           |
| POST  | /api/auth/login    | Все    | Вход (получение access + refresh токенов) |
| POST  | /api/auth/refresh  | Все    | Обновление токенов по refresh-токену      |

**Пример регистрации:**

`POST /api/auth/register`
```json
{
  "username": "mechanic_ivan",
  "password": "Secure1!",
  "role": "ROLE_MECHANIC"
}
```

**Пример входа:**

`POST /api/auth/login`
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

---

### Клиенты `/api/customers`

| Метод  | Путь                | Роли            | Описание             |
|--------|---------------------|-----------------|----------------------|
| GET    | /api/customers      | ADMIN, MECHANIC | Список всех клиентов |
| GET    | /api/customers/{id} | ADMIN, MECHANIC | Клиент по ID         |
| POST   | /api/customers      | ADMIN           | Создать клиента      |
| PUT    | /api/customers/{id} | ADMIN           | Обновить клиента     |
| DELETE | /api/customers/{id} | ADMIN           | Удалить клиента      |

---

### Автомобили `/api/vehicles`

| Метод  | Путь                        | Роли                      | Описание                   |
|--------|-----------------------------|---------------------------|----------------------------|
| GET    | /api/vehicles               | ADMIN, MECHANIC, CUSTOMER | Список всех автомобилей    |
| GET    | /api/vehicles/{id}          | ADMIN, MECHANIC, CUSTOMER | Автомобиль по ID           |
| GET    | /api/vehicles/customer/{id} | ADMIN, MECHANIC, CUSTOMER | Автомобили клиента         |
| POST   | /api/vehicles               | ADMIN, CUSTOMER           | Добавить автомобиль        |
| PUT    | /api/vehicles/{id}          | ADMIN, CUSTOMER           | Обновить данные автомобиля |
| DELETE | /api/vehicles/{id}          | ADMIN                     | Удалить автомобиль         |

---

### Механики `/api/mechanics`

| Метод  | Путь                  | Роли            | Описание                 |
|--------|-----------------------|-----------------|--------------------------|
| GET    | /api/mechanics        | ADMIN, MECHANIC | Список всех механиков    |
| GET    | /api/mechanics/{id}   | ADMIN, MECHANIC | Механик по ID            |
| GET    | /api/mechanics/active | ADMIN, MECHANIC | Только активные механики |
| POST   | /api/mechanics        | ADMIN           | Добавить механика        |
| PUT    | /api/mechanics/{id}   | ADMIN           | Обновить данные механика |
| DELETE | /api/mechanics/{id}   | ADMIN           | Удалить механика         |

---

### Запчасти `/api/parts`

| Метод  | Путь            | Роли           | Описание          |
|--------|-----------------|----------------|-------------------|
| GET    | /api/parts      | Авторизованные | Список запчастей  |
| GET    | /api/parts/{id} | Авторизованные | Запчасть по ID    |
| POST   | /api/parts      | ADMIN          | Добавить запчасть |
| PUT    | /api/parts/{id} | ADMIN          | Обновить запчасть |
| DELETE | /api/parts/{id} | ADMIN          | Удалить запчасть  |

---

### Заказ-наряды `/api/orders`

| Метод  | Путь                                     | Роли                      | Описание                     |
|--------|------------------------------------------|---------------------------|------------------------------|
| GET    | /api/orders                              | Авторизованные            | Все заказы                   |
| GET    | /api/orders/{id}                         | Авторизованные            | Заказ по ID                  |
| GET    | /api/orders/vehicle/{vehicleId}          | Авторизованные            | Заказы по автомобилю         |
| GET    | /api/orders/mechanic/{id}                | Авторизованные            | Заказы по механику           |
| GET    | /api/orders/status/{status}              | Авторизованные            | Заказы по статусу            |
| POST   | /api/orders                              | ADMIN, CUSTOMER           | Создать заказ-наряд          |
| PUT    | /api/orders/{id}/status                  | ADMIN, MECHANIC           | Изменить статус заказа       |
| POST   | /api/orders/{id}/items                   | ADMIN, MECHANIC           | Добавить позицию в заказ     |
| PUT    | /api/orders/{id}/items/{itemId}/complete | ADMIN, MECHANIC           | Отметить позицию выполненной |
| DELETE | /api/orders/{id}                         | ADMIN                     | Удалить заказ                |

---

## Бизнес-операции

### 1. Автоназначение механика
```
POST /api/orders/{id}/auto-assign
Роли: ADMIN, MECHANIC
```
Автоматически назначает на заказ активного механика с наименьшим количеством активных заказов (OPEN + IN_PROGRESS). После назначения статус заказа меняется с OPEN на IN_PROGRESS.

**Пример ответа:** полный объект ServiceOrder с назначенным механиком.

---

### 2. Закрытие заказа
```
PUT /api/orders/{id}/close
Роли: ADMIN, MECHANIC
```
Переводит заказ из статуса COMPLETED в CLOSED. Операция завершается ошибкой 400, если хотя бы одна обязательная позиция (mandatory=true) не отмечена выполненной (completed=false). При успешном закрытии фиксируется дата закрытия (closedAt).

**Ошибка:** `"Cannot close order: 2 mandatory item(s) not completed"`

---

### 3. Детализация стоимости заказа
```
GET /api/orders/{id}/cost
Роли: ADMIN, MECHANIC, CUSTOMER
```
Возвращает разбивку стоимости заказа на работы и запчасти.

**Пример ответа:**
```json
{
  "orderId": 1,
  "workTotal": 800.00,
  "partsTotal": 3000.00,
  "grandTotal": 3800.00,
  "items": [
    {
      "type": "WORK",
      "description": "Замена моторного масла",
      "quantity": 1,
      "unitPrice": 800.00,
      "lineTotal": 800.00
    },
    {
      "type": "PART",
      "description": "Моторное масло 5W-40",
      "quantity": 1,
      "unitPrice": 2200.00,
      "lineTotal": 2200.00
    }
  ]
}
```

---

### 4. Нагрузка механиков
```
GET /api/reports/mechanics
Роли: ADMIN, MECHANIC
```
Возвращает статистику нагрузки по каждому механику.

**Пример ответа:**
```json
[
  {
    "mechanicId": 1,
    "mechanicName": "Алексей Смирнов",
    "openOrders": 0,
    "inProgressOrders": 2,
    "completedOrders": 1,
    "totalOrders": 3
  }
]
```

---

### 5. Деактивация механика с переназначением заказов
```
PUT /api/mechanics/{id}/deactivate?reassignToMechanicId={newId}
Роли: ADMIN, MECHANIC
```
Деактивирует механика (isActive=false) и переназначает все его активные заказы (OPEN, IN_PROGRESS) другому механику. Новый механик должен быть активен.

**Ошибка:** `"New mechanic is not active: 2"`

---

## Безопасность

### JWT-аутентификация

Используется двухуровневая схема токенов:

| Параметр              | Значение                          |
|-----------------------|-----------------------------------|
| Алгоритм подписи      | HMAC-SHA256                       |
| Access-токен          | 15 минут                          |
| Refresh-токен         | 7 дней                            |
| Хранение сессий       | В БД (таблица user_sessions)      |
| Ротация токенов       | Да (при refresh старая — REVOKED) |
| Хеширование refresh   | SHA-256 в БД                      |

**Использование:**
1. Войдите: `POST /api/auth/login` — получите `accessToken` и `refreshToken`
2. Добавляйте заголовок: `Authorization: Bearer <accessToken>`
3. По истечении access-токена используйте `POST /api/auth/refresh` с `refreshToken`

### Роли

| Роль           | Описание                                         |
|----------------|--------------------------------------------------|
| ROLE_ADMIN     | Полный доступ ко всем операциям                  |
| ROLE_MECHANIC  | Просмотр и работа с заказами, отчёты             |
| ROLE_CUSTOMER  | Управление своими автомобилями, создание заказов |

### Требования к паролю
- Минимум 8 символов
- Хотя бы одна заглавная буква
- Хотя бы одна цифра
- Хотя бы один специальный символ (`!@#$%^&*` и др.)

---

## Установка и запуск

### Предварительные требования
- Java 21+
- Maven 3.9+
- PostgreSQL 16+

### Шаги установки

**1. Создайте базу данных:**
```sql
CREATE DATABASE autoservicedb;
```

**2. Настройте переменные окружения:**

| Переменная                 | По умолчанию                                   | Описание        |
|----------------------------|------------------------------------------------|-----------------|
| SERVER_PORT                | 8443                                           | Порт сервера    |
| SSL_ENABLED                | true                                           | Включить HTTPS  |
| SSL_KEY_STORE_PATH         | classpath:keystore.p12                         | Путь к keystore |
| SSL_KEY_STORE_PASSWORD     | MyPassword1!                                   | Пароль keystore |
| SPRING_DATASOURCE_URL      | jdbc:postgresql://localhost:5432/autoservicedb | URL БД          |
| SPRING_DATASOURCE_USERNAME | postgres                                       | Пользователь БД |
| SPRING_DATASOURCE_PASSWORD | (пусто)                                        | Пароль БД       |

**3. Сгенерируйте TLS-сертификат:**
```bash
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 \
  -storepass MyPassword1! -validity 365 \
  -dname "CN=localhost, OU=AutoService, O=AutoService, L=Moscow, S=Moscow, C=RU"
```

**4. Соберите и запустите:**
```bash
mvn clean package -DskipTests
java -jar target/autoservice-1.0-SNAPSHOT.jar
```

**5. Или через Maven:**
```bash
mvn spring-boot:run
```

**6. Проверьте работу:**
```bash
curl -k https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234!"}'
```

---

## Тестовые данные (DataInitializer)

При первом запуске автоматически создаются:

**Пользователи системы:**

| Логин     | Пароль     | Роль          |
|-----------|------------|---------------|
| admin     | Admin1234! | ROLE_ADMIN    |
| mechanic1 | Mech1234!  | ROLE_MECHANIC |
| customer1 | Cust1234!  | ROLE_CUSTOMER |

**Клиенты:**
- Иванов Иван Иванович (2 автомобиля: Toyota Camry 2020, BMW X5 2019)
- Петров Пётр Петрович (2 автомобиля: Lada Vesta 2022, Ford Focus 2018)
- Сидорова Анна Сергеевна (1 автомобиль: Hyundai Solaris 2021)

**Механики:**
- Алексей Смирнов (специализация: Двигатель)
- Дмитрий Козлов (специализация: Ходовая часть)
- Ольга Новикова (специализация: Электрика)

**Запчасти:** масляный фильтр, воздушный фильтр, свеча зажигания, тормозные колодки, тормозные диски, моторное масло, антифриз, ремень ГРМ

**Заказ-наряды:**
1. Toyota Camry — ТО (замена масла) — статус IN_PROGRESS
2. BMW X5 — диагностика тормозной системы — статус OPEN
3. Lada Vesta — замена свечей зажигания — статус COMPLETED
4. Ford Focus — замена ремня ГРМ — статус IN_PROGRESS
5. Hyundai Solaris — замена антифриза — статус CANCELLED

---

## Запуск тестов

```bash
mvn test
```

Для тестов используется профиль `test` с конфигурацией из `src/test/resources/application.properties`. SSL отключён, подключение к БД настраивается через переменные окружения.

---

## CI/CD (GitHub Actions)

Файл: `.github/workflows/ci.yml`

Pipeline выполняет:
1. Поднимает PostgreSQL 16 как сервис
2. Восстанавливает keystore из GitHub Secret (`KEYSTORE_BASE64`)
3. Компилирует проект
4. Запускает тесты
5. Упаковывает JAR
6. Публикует JAR как артефакт

Для настройки CI добавьте в GitHub Secrets:
- `KEYSTORE_BASE64` — содержимое keystore.p12 в base64:

```bash
base64 -w 0 src/main/resources/keystore.p12
```

---

## Postman

Коллекция запросов: `postman_collection.json`

### Переменные коллекции

| Переменная    | Значение по умолчанию  | Описание                 |
|---------------|------------------------|--------------------------|
| baseUrl       | https://localhost:8443 | Базовый URL              |
| adminToken    | (пусто)                | JWT токен администратора |
| mechanicToken | (пусто)                | JWT токен механика       |
| customerToken | (пусто)                | JWT токен клиента        |

Токены заполняются автоматически через test scripts при выполнении запросов Login.

### Импорт коллекции
1. Откройте Postman
2. File → Import → выберите `postman_collection.json`
3. Настройте переменную `baseUrl` (по умолчанию `https://localhost:8443`)
4. Отключите проверку SSL: Settings → SSL certificate verification → OFF
5. Выполните Register → Login для получения токенов
6. Остальные запросы автоматически используют полученные токены
