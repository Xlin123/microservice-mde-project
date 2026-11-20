# Microservice UML Mapping Specification

This document defines how microservice concepts map to UML elements and stereotypes, as well as the UML relationships used between them. It documents the **modeling rules**; it does not imply that code generation or EVL validation is already implemented.

## Table of Contents

- [1. Project Information](#1-project-information)
- [2. Mapping Summary](#2-mapping-summary)
- [3. Individual Mapping Rules](#3-individual-mapping-rules)
  - [MAP-00 — Generic Container Service](#map-00--generic-container-service)
  - [MAP-01 — Application Service Container](#map-01--application-service-container)
  - [MAP-02 — Database Service Container](#map-02--database-service-container)
  - [MAP-04 — HTTP Endpoint](#map-04--http-endpoint)
  - [MAP-05 — HTTP Request Method Enumeration](#map-05--http-request-method-enumeration)
  - [MAP-06 — PostgreSQL Table](#map-06--postgresql-table)
  - [MAP-07 — Business Service Module](#map-07--business-service-module)
  - [MAP-08 — FastAPI Router](#map-08--fastapi-router)
  - [MAP-09 — Data-Access Repository](#map-09--data-access-repository)
- [4. Relationship Mappings](#4-relationship-mappings)
  - [4.1 Structural Ownership Relationships](#41-structural-ownership-relationships)
  - [4.2 Dependency Relationships](#42-dependency-relationships)
  - [4.3 Association Relationships](#43-association-relationships)
  - [4.4 UML Property and Type Mappings](#44-uml-property-and-type-mappings)
  - [4.5 Stereotype Generalization](#45-stereotype-generalization)
  - [4.6 UML Primitive Types to Python and PostgreSQL](#46-uml-primitive-types-to-python-and-postgresql)
  - [4.7 SQL Mappings for Entity Associations and Keys](#47-sql-mappings-for-entity-associations-and-keys)
- [5. Constraints and Validation](#5-constraints-and-validation)
  - [5.1 EVL Implementation](#51-evl-implementation)
  - [5.2 Route Call Chains and Isolated Router Scopes](#52-route-call-chains-and-isolated-router-scopes-con-21-con-24-to-con-27)
  - [5.3 Router and Service Dependencies Match Method Calls](#53-router-and-service-dependencies-match-method-calls-con-22-con-23)
  - [5.4 Repository Access Matches Application Database Dependencies](#54-repository-access-matches-application-database-dependencies-con-28)
  - [5.5 Application Routes Have Unique Full URL Patterns](#55-application-routes-have-unique-full-url-patterns-con-29)
  - [5.6 Required, Unique and Usable Names](#56-required-unique-and-usable-names-con-30-to-con-32)
  - [5.7 Application and Database Container Roles Are Exclusive](#57-application-and-database-container-roles-are-exclusive-con-33)
  - [5.8 Router Prefixes, Route Paths and Path Parameters](#58-router-prefixes-route-paths-and-path-parameters-con-02-con-07-con-08-con-34-to-con-39)
  - [5.9 Single-Column Primary Keys and Association Consistency](#59-single-column-primary-keys-and-association-consistency-con-11-con-12-con-18-con-19-con-40-to-con-48)
  - [5.10 Method Signatures, Returns and Declared Usage](#510-method-signatures-returns-and-declared-usage-con-49-to-con-55)
- [6. Open Modeling Decisions](#6-open-modeling-decisions)
- [7. Completion Checklist](#7-completion-checklist)

## 1. Project Information

| Property | Value |
|---|---|
| Project | Microservice Modeling DSL |
| Target Technologies | FastAPI, PostgreSQL, Docker |
| UML Profile | MicroserviceProfile |
| Modeling Tool | Eclipse Papyrus |

## 2. Mapping Summary

| ID | Microservice concept | UML element / stereotype | UML metaclass | Status |
|---|---|---|---|---|
| MAP-00 | Generic container service | `«ContainerizedService»` | Component | Implemented |
| MAP-01 | Application service container | `«ApplicationService»` | Component | Implemented |
| MAP-02 | Database service container | `«DatabaseService»` | Component | Implemented |
| MAP-04 | HTTP endpoint | `«Route»` | Operation | Implemented |
| MAP-05 | HTTP request method | `HttpMethod` | Enumeration | Implemented |
| MAP-06 | PostgreSQL table | `«Entity»` | Class | Implemented |
| MAP-07 | Business service module | `«Service»` | Class | Implemented |
| MAP-08 | FastAPI router | `«Router»` | Class | Implemented |
| MAP-09 | Data-access repository | `«Repository»` | Class | Implemented |



## 3. Individual Mapping Rules

### MAP-00 — Generic Container Service

| Property | Specification |
|---|---|
| Source element | Generic Docker service container |
| UML metaclass | `Component` |
| Applied stereotype | `«ContainerizedService»` |
| Mapping rule | Represent a containerized service as a UML Component. `«ApplicationService»` and `«DatabaseService»` specialize this stereotype. |

**Tagged-value mappings**

| Source property | Tagged value | Type |
|---|---|---|
| Published host port | `hostPort` (default: `8080`) | Integer |

### MAP-01 — Application Service Container

| Property | Specification |
|---|---|
| Source element | Docker container running a FastAPI application service |
| UML metaclass | `Component` |
| Applied stereotype | `«ApplicationService»` |
| Mapping rule | Map each modeled FastAPI application container to a Component with `«ApplicationService»`. |

**Tagged-value mappings**

| Source property | Tagged value | Type |
|---|---|---|
| Internal application port | `containerPort` (default: `8080`) | Integer |

### MAP-02 — Database Service Container

| Property | Specification |
|---|---|
| Source element | Docker container running PostgreSQL |
| UML metaclass | `Component` |
| Applied stereotype | `«DatabaseService»` |
| Mapping rule | Map each modeled PostgreSQL container to a Component with `«DatabaseService»`. |

**Tagged-value mappings**

| Source property | Tagged value | Type |
|---|---|---|
| Database name | `databaseName` | String |
| Database volume path | `databaseVolume` | String |
| Internal database port | `containerPort` (default: `5432`) | Integer |

### MAP-04 — HTTP Endpoint

| Property | Specification |
|---|---|
| Source element | Individual FastAPI HTTP endpoint |
| UML metaclass | `Operation` |
| Applied stereotype | `«Route»` |
| Mapping rule | Map each endpoint to a UML Operation owned by a `«Router»` Class and apply `«Route»`, including its HTTP method and relative path. |

**Tagged-value mappings**

| Source property | Tagged value | Type |
|---|---|---|
| HTTP method (e.g., GET, POST) | `method` | `HttpMethod` |
| Route path extension (e.g., `/` or `/{user_id}`) | `path` | String |

*The effective URL is the owning `«Router».prefix` plus the route's `path`.
Use `/` for the router's base endpoint. Prefix, path and parameter constraints
are defined in Section 5.8.*

### MAP-05 — HTTP Request Method Enumeration

| Property | Specification |
|---|---|
| Source element | HTTP request method |
| UML metaclass | `Enumeration` |
| Applied stereotype | None — standard UML element |
| Mapping rule | Represent each supported HTTP method as an `EnumerationLiteral` of `HttpMethod`; use this enumeration as the type of `«Route».method`. |

**Enumeration literals**

| HTTP method | UML literal |
|---|---|
| `GET` | `HttpMethod::GET` |
| `POST` | `HttpMethod::POST` |
| `PUT` | `HttpMethod::PUT` |
| `PATCH` | `HttpMethod::PATCH` |
| `DELETE` | `HttpMethod::DELETE` |

### MAP-06 — PostgreSQL Table

| Property | Specification |
|---|---|
| Source element | PostgreSQL database table |
| UML metaclass | `Class` |
| Applied stereotype | `«Entity»` |
| Mapping rule | Represent each modeled database table as a UML Class with `«Entity»`. Column and key mappings are described in Section 4.4, primitive type mappings in Section 4.6, and SQL association mappings in Section 4.7. |


### MAP-07 — Business Service Module

| Property | Specification |
|---|---|
| Source element | Application service class or module containing business logic |
| UML metaclass | `Class` |
| Applied stereotype | `«Service»` |
| Mapping rule | Represent each modeled business service as a UML Class with `«Service»`. |


### MAP-08 — FastAPI Router

| Property | Specification |
|---|---|
| Source element | FastAPI `APIRouter` |
| UML metaclass | `Class` |
| Applied stereotype | `«Router»` |
| Mapping rule | Represent each modeled router as a UML Class with `«Router»`, serving as the main API modeling element and directly owning its endpoint operations (MAP-04). |

**Tagged-value mappings**

| Source property | Tagged value | Type |
|---|---|---|
| Required static router URL prefix (e.g., `/users`) | `prefix` | String |

### MAP-09 — Data-Access Repository

| Property | Specification |
|---|---|
| Source element | Application repository class or module for database access |
| UML metaclass | `Class` |
| Applied stereotype | `«Repository»` |
| Mapping rule | Represent each modeled data-access repository as a UML Class with `«Repository»`. |



## 4. Relationship Mappings

This section defines the permitted relationships and structural
mappings between UML elements in the microservice model.

### 4.1 Structural Ownership Relationships

These mappings define which UML elements own other elements.
Ownership is represented through standard UML properties,
not Dependency relationships.

| ID | Owner | UML Mechanism | Owned Element | Meaning |
|---|---|---|---|---|
| REL-01 | `«ApplicationService»` | `packagedElement` | `«Router»` | Application owns its API routers |
| REL-02 | `«ApplicationService»` | `packagedElement` | `«Service»` | Application owns its business services |
| REL-03 | `«ApplicationService»` | `packagedElement` | `«Repository»` | Application owns its data-access repositories |
| REL-04 | `«DatabaseService»` | `packagedElement` | `«Entity»` | Database owns its SQL tables |
| REL-05 | `«Router»` | `ownedOperation` | `«Route»` | Router owns its HTTP endpoint operations |
| REL-06 | `«Entity»` | `ownedAttribute` | `Property` | Entity owns scalar column attributes and may own association-end properties |
| REL-07 | `HttpMethod` | `ownedLiteral` | `EnumerationLiteral` | HTTP enumeration owns its method values |

Each Router belongs to an ApplicationService. An ApplicationService
exposes the endpoints defined by its owned Routers according to the
project's transformation rules.

### 4.2 Dependency Relationships

These mappings define usage and runtime dependencies
between elements.

| ID | Source | UML Relationship | Target | Meaning |
|---|---|---|---|---|
| REL-09 | `«ApplicationService»` | `Dependency` | `«DatabaseService»` | Application depends on a database container |
| REL-10 | `«Router»` | `Dependency` | `«Service»` | Router invokes business logic |
| REL-11 | `«Router»` | `Dependency` | `«Repository»` | Router accesses a repository directly |
| REL-12 | `«Service»` | `Dependency` | `«Repository»` | Service accesses persistence operations |
| REL-13 | `«Service»` | `Dependency` | `«Service»` | One service uses another service |
| REL-14 | `«Repository»` | `Dependency` | `«Entity»` | Repository accesses a database table |
| REL-16 | `«Route»` | `Dependency` | `Operation` owned by `«Repository»` | Endpoint calls one or more repository methods |
| REL-17 | `«Route»` | `Dependency` | `Operation` owned by `«Service»` | Endpoint calls a service method |
| REL-18 | `Operation` owned by `«Service»` | `Dependency` | `Operation` owned by `«Repository»` | Service method calls a repository method |
| REL-19 | `Operation` owned by `«Service»` | `Dependency` | `Operation` owned by `«Service»` | Service method calls another service method, including a utility method |

**Notes:**
- Routers may bypass the Service layer for simple operations.
- Service-to-Service dependencies are optional.
- Dependencies do not imply structural ownership.
- REL-16 to REL-19 are operation-level dependencies. Service and repository methods are
  plain UML Operations with no additional stereotype. These links are standard UML
  dependencies; their permitted roles and chain constraints are validated by EVL.
- Class dependencies describe which classes use each other; operation dependencies
  identify the specific methods involved. The proposed consistency rules are CON-22
  and CON-23; route dependency chains and isolated router scopes are defined in Section 5.2.

### 4.3 Association Relationships

These mappings define structural relationships between
database entities.

| ID | Source | UML Relationship | Target | Meaning |
|---|---|---|---|---|
| REL-15 | `«Entity»` | `Association` | `«Entity»` | Represents a supported binary relationship mapped to one foreign key under Section 4.7 |

The initial supported relationships have one reference end with multiplicity
`1..1` or `0..1` and one collection end with multiplicity `0..*`. SQL generation
is defined in Section 4.7, and model constraints in Section 5.9. Entities do not
have to participate in associations.

### 4.4 UML Property and Type Mappings

These mappings use existing UML features rather than
creating additional stereotypes or dependencies.

| ID | UML Element / Feature | Target Concept | Mapping Rule |
|---|---|---|---|
| PROP-01 | Scalar `«Entity»::ownedAttribute` with no association | SQL column | Each supported single-valued primitive attribute generates one scalar column; association-end properties use Section 4.7 |
| PROP-02 | Scalar `Property::isID = true` | SQL primary key | The entity's single identifying attribute generates its single-column primary key |
| PROP-03 | `Property::type` | SQL column type | Supported UML primitive types are translated to the PostgreSQL types in Section 4.6 |
| PROP-04 | `«Route».method` typed by `HttpMethod` | HTTP method | Enumeration value determines the endpoint HTTP method |
| PROP-05 | `Property::type`, `Parameter::type` | Python scalar type | Supported UML primitive types are translated to the Python types in Section 4.6 |
| PROP-06 | `Association::memberEnd` and referenced entity's identifying attribute | SQL foreign key | Supported multiplicities determine the referencing table; the reference role and target primary key determine the generated column name and type under Section 4.7 |

**Primary-key constraints:**
- Each Entity has exactly one identifying scalar attribute with multiplicity `1..1`.
- Its type is the standard UML `Integer` or `String`; composite keys are unsupported.
- Association-end properties cannot be identifying attributes. See Section 5.9.

### 4.5 Stereotype Generalization

These relationships exist in the UML Profile, rather
than between individual application-model instances.

| Child Stereotype | UML Relationship | Parent Stereotype | Meaning |
|---|---|---|---|
| `«ApplicationService»` | `Generalization` | `«ContainerizedService»` | Inherits common container properties |
| `«DatabaseService»` | `Generalization` | `«ContainerizedService»` | Inherits common container properties |

### 4.6 UML Primitive Types to Python and PostgreSQL

The following table defines the initial scalar type mappings for generated Python
code and PostgreSQL columns. Use the actual types from the standard UML primitive
type library; a custom type with the same name does not automatically qualify.
These are specification choices for this DSL, with generation and validation still
to be implemented.

| UML primitive type | Python type | PostgreSQL column type | Mapping notes |
|---|---|---|---|
| `String` | `str` | `TEXT` | Variable-length text; no maximum length is specified by this mapping |
| `Integer` | `int` | `BIGINT` | Persisted values must fit the signed 64-bit database range |
| `Boolean` | `bool` | `BOOLEAN` | Logical true/false values |
| `Real` | `float` | `DOUBLE PRECISION` | Approximate floating-point values; no exact decimal mapping is defined |
| `UnlimitedNatural` | No application scalar mapping | No column mapping | Supported as UML multiplicity metadata, including `*`; not supported as a scalar attribute or parameter type |

Apply the Python mappings to scalar attributes and operation parameters, including
return parameters. Apply the SQL mappings to entity attributes that represent
scalar database columns. Associations and scalar-column nullability are covered
in Section 4.7. Section 5.10 permits entity parameters and collection returns at
the model level; their API representation is a separate generation decision.
Collection-valued SQL columns, enumerations and custom scalar types remain
unsupported until their mappings are defined. Optionality is separate from the
scalar type.



Date and datetime types have no dedicated mapping in the current specification;
whether to introduce extended primitives for them is an open decision in Section 6.

### 4.7 SQL Mappings for Entity Associations and Keys

This section specifies what the UML-to-YAML transformation derives and what the
downstream SQL generator must preserve. These are proposed mappings, not an
implemented YAML schema or generator. Validation checks that the UML model supplies
the required information; foreign-key columns are generated from associations.

A scalar entity attribute has no association, a supported primitive type and
multiplicity `1..1` or `0..1`. A property participating in an association is an
association end and is handled through that association instead. Process each
association once, using its two `memberEnd` properties whether those ends are
owned by entities or by the association itself.

For a reference end typed by entity `A` with multiplicity `1..1` or `0..1`, and
the other end typed by entity `B` with multiplicity `0..*`, generate the foreign
key on `B`, referencing `A`'s single primary key. For example, an end named
`customer` typed by `Customer` at `1..1`, paired with an end named `orders` typed
by `Order` at `0..*`, places the foreign key on `Order`.

| ID | UML information | SQL mapping |
|---|---|---|
| SQL-01 | Scalar entity attribute | Generate one column using Section 4.6; `1..1` produces `NOT NULL` and `0..1` permits `NULL` |
| SQL-02 | Entity's single identifying attribute | Generate a single-column `PRIMARY KEY` using its approved type mapping |
| SQL-03 | Supported reference/collection multiplicity pair | Generate one foreign-key column on the entity typed by the collection end, referencing the entity typed by the reference end |
| SQL-04 | Reference role name and referenced primary-key name | Name the foreign-key column `<reference_role>_<primary_key_name>`, for example `customer_id` |
| SQL-05 | Referenced primary-key type | Give the generated foreign-key column exactly the same mapped SQL type and corresponding Python scalar type as the referenced key: `Integer` becomes `BIGINT`/`int`, and `String` becomes `TEXT`/`str` |
| SQL-06 | Reference-end multiplicity | `1..1` produces a `NOT NULL` foreign-key column; `0..1` permits `NULL` |
| SQL-07 | Collection end at `0..*` | Generate no additional scalar column for the inverse collection and no uniqueness constraint on the foreign-key column |
| SQL-08 | Deletion of a referenced row | Generate `ON DELETE RESTRICT` on every foreign key as the DSL default |

The reference property remains typed by the target entity, for example
`Order.customer: Customer`; its generated column type is derived from
`Customer`'s primary-key attribute. Modelers do not separately declare a scalar
`customer_id` for the same association. A conflicting scalar attribute or another
association producing the same column name is a validation error, not a request
to reuse that column. SQL identifiers must be rendered consistently, and collision
checks must use the same identifier rules as the generator.

For a required customer reference with integer keys, the generated SQL is
equivalent to:

```sql
CREATE TABLE "Customer" (
    "id" BIGINT PRIMARY KEY
);

CREATE TABLE "Order" (
    "id" BIGINT PRIMARY KEY,
    "customer_id" BIGINT NOT NULL,
    FOREIGN KEY ("customer_id") REFERENCES "Customer" ("id") ON DELETE RESTRICT
);
```

Under the DSL's delete default, a customer still referenced by an order cannot
be deleted. The application must first remove or reassign those references;
an optional reference may be explicitly set to `NULL`. Deleting an order does
not cause deletion of its customer. The default applies to required and optional
references alike; UML aggregation or composition does not select cascading deletion.
`RESTRICT` is an explicit DSL choice. PostgreSQL's built-in default is `NO ACTION`.
Key-update actions use PostgreSQL's default `ON UPDATE NO ACTION` pending a separate
update policy. See [PostgreSQL foreign-key actions](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-FK).

One-to-one, many-to-many, n-ary and other multiplicity combinations require later
mapping decisions and are rejected by the initial constraints. Identifier value
generation is also a separate decision: `isID = true` does not imply an identity
column or automatic value generation.

---

## 5. Constraints and Validation

Keep all EVL validation rules here instead of repeating them in every element mapping.
Rules are specified for EVL (Epsilon Validation Language) and run against the model from
a separate `.evl` file; they are not stored in the profile. The table records proposed
modeling requirements; current implementation coverage is described in Section 5.1.

`constraint` blocks report errors, `critique` blocks report warnings.

Here, a **route** is an operation on a `«Router»` class with `«Route»` applied.
A **service method** is an operation on a `«Service»` class, and a **repository method**
is an operation on a `«Repository»` class.

| ID | Name | Applies to | Rule | Kind | Source | Status |
|---|---|---|---|---|---|---|
| CON-01 | Router belongs to an application | `«Router»` | Every router is owned by an `«ApplicationService»` component | constraint | REL-01 | Proposed |
| CON-02 | Router prefix starts with a slash and has no trailing slash | `«Router»` | Every router has a non-empty `prefix` starting with `/` and not ending with `/`; neither an empty prefix nor `/` alone is allowed | constraint | MAP-08, Section 5.8 | Proposed |
| CON-03 | Router methods are routes | `«Router»` | Every operation owned by a router has `«Route»` applied | constraint | REL-05 | Proposed |
| CON-04 | Routes have unique HTTP methods and paths | `«Router»` | No two routes in the same router share the same (`method`, `path`) pair | constraint | MAP-04 | Proposed |
| CON-05 | Router has at least one route | `«Router»` | Every router owns at least one route | critique | MAP-08 | Proposed |
| CON-06 | Route belongs to a router | `«Route»` | Every route is owned by a `«Router»` class | constraint | MAP-04, REL-05 | Proposed |
| CON-07 | Route has an HTTP method and a path extension | `«Route»` | Every route has a `method` and a non-empty `path` that is either `/` or starts with `/` without ending with `/` | constraint | MAP-04, Section 5.8 | Proposed |
| CON-08 | Each route path parameter has exactly one matching input | `«Route»` | Every `{name}` in a route's path matches exactly one owned `in` parameter called `name` | constraint | MAP-04, Section 5.8 | Proposed |
| CON-09 | Services and repositories belong to an application | `«Service»`, `«Repository»` | Every service and repository is owned by an `«ApplicationService»` component | constraint | REL-02, REL-03 | Proposed |
| CON-10 | Entity belongs to a database | `«Entity»` | Every entity is owned by a `«DatabaseService»` component | constraint | REL-04 | Proposed |
| CON-11 | Entity has exactly one primary-key attribute | `«Entity»` | Every entity has exactly one owned scalar attribute with `isID = true`; composite primary keys are rejected | constraint | PROP-02, Section 5.9 | Proposed |
| CON-12 | Entity scalar columns have supported types | Scalar attributes owned by `«Entity»` | Every attribute with no association has a standard UML primitive type with a supported SQL mapping in Section 4.6; association ends are validated as relationships | constraint | PROP-01, PROP-03, Section 5.9 | Proposed |
| CON-13 | Container ports are in range | `«ContainerizedService»` | Every container's `hostPort` and `containerPort` are in `1..65535` | constraint | MAP-00, MAP-01, MAP-02 | Proposed |
| CON-14 | Containers use different host ports | `«ContainerizedService»` | No two containerized services share the same `hostPort` | constraint | MAP-00 | Proposed |
| CON-15 | Database has a name and volume | `«DatabaseService»` | Every database has non-empty `databaseName` and `databaseVolume` values | constraint | MAP-02 | Proposed |
| CON-16 | Application depends on a database | `«ApplicationService»` | Every application depends on at least one `«DatabaseService»` | critique | REL-09 | Proposed |
| CON-17 | Dependencies connect permitted types | `Dependency` (including `Usage`) | Dependencies between containers, routers, services, repositories, entities and methods follow the directions listed in Section 4.2 | constraint | REL-09 to REL-14, REL-16 to REL-19 | Proposed |
| CON-18 | Entity association has exactly two entity ends | Application-model `Association` | Every supported database association has exactly two distinct member-end properties, each typed by a `«Entity»` class | constraint | REL-15, Section 5.9 | Proposed |
| CON-19 | Association multiplicities identify one foreign-key location | Entity `Association` | Exactly one end has multiplicity `1..1` or `0..1`, and the other has multiplicity `0..*`; one-to-one, many-to-many and other combinations are rejected initially | constraint | Section 4.7, Section 5.9 | Proposed |
| CON-20 | Class has at most one microservice role | `Class` | A class has at most one of `«Entity»`, `«Service»`, `«Repository»` or `«Router»` applied | constraint | MAP-06 to MAP-09 | Proposed |
| CON-21 | Route call chain uses exactly one repository | `«Route»` | Follow every method call from a route, including calls through services and utilities; all repository methods reached belong to exactly one repository | constraint | REL-16 to REL-19, Section 5.2 | Proposed |
| CON-22 | Router and service dependencies have matching method calls | Dependencies from a `«Router»` or `«Service»` to a `«Service»` or `«Repository»` | At least one route on the router, or method on the service, calls a method on the specific service or repository it depends on (see Section 5.3) | constraint | REL-10 to REL-13, REL-16 to REL-19 | Proposed |
| CON-23 | Method calls have matching router and service dependencies | Calls from routes or service methods to service or repository methods | Every call between different classes has the matching router-to-service, router-to-repository, service-to-repository or service-to-service dependency described in Section 5.3 | constraint | REL-10 to REL-13, REL-16 to REL-19 | Proposed |
| CON-24 | Router uses exactly one repository | `«Router»` | Across the router's direct dependencies and all service dependencies in its scope, exactly one repository is reached; every route uses that same repository | constraint | REL-10 to REL-13, CON-21, Section 5.2 | Proposed |
| CON-25 | Repository is used by at most one router scope | `«Repository»` | Two different routers cannot reach the same repository, either directly or through services | constraint | REL-10 to REL-13, Section 5.2 | Proposed |
| CON-26 | Service is used by at most one router scope | `«Service»` | A service, including a utility service, cannot be shared between different router scopes; routes within the same router may share it | constraint | REL-10, REL-13, Section 5.2 | Proposed |
| CON-27 | Router scope stays within one application | `«Router»` | The router and all services and repositories in its scope are owned by the same `«ApplicationService»` component | constraint | REL-01 to REL-03, Section 5.2 | Proposed |
| CON-28 | Repository's application depends on each accessed entity's database | `«Repository»` | For every entity the repository accesses, the `«ApplicationService»` owning that repository has a dependency on the `«DatabaseService»` owning that entity | constraint | REL-03, REL-04, REL-09, REL-14, Section 5.4 | Proposed |
| CON-29 | Application routes have unique HTTP methods and full URL patterns | `«ApplicationService»` | Across all routers in an application, no two routes have the same HTTP method and full URL pattern, even when their path parameter names differ | constraint | MAP-04, MAP-08, REL-01, REL-05, Section 5.5 | Proposed |
| CON-30 | Modeled containers, classes and members have names | Containers, routers, services, repositories, entities, methods, entity columns and non-return parameters | Each element has a non-empty name with no leading or trailing whitespace; return parameters may be unnamed | constraint | MAP-00 to MAP-02, MAP-04, MAP-06 to MAP-09, Section 5.6 | Proposed |
| CON-31 | Names are unique within their defined scopes | Named elements covered by CON-30 | Container, class, method, column and parameter names are unique in the scopes listed in Section 5.6 | constraint | Section 5.6 | Proposed |
| CON-32 | Python class, method, column and parameter names are valid identifiers | Routers, services, repositories, entities, methods, entity columns and non-return parameters | Names are valid Python identifiers and are not reserved Python keywords for the target Python version | constraint | MAP-04, MAP-06 to MAP-09, Section 5.6 | Proposed |
| CON-33 | Container has at most one application or database role | `Component` | A component cannot have both `«ApplicationService»` and `«DatabaseService»` applied | constraint | MAP-01, MAP-02, Section 5.7 | Proposed |
| CON-34 | Router prefix contains only fixed path segments | `«Router»` | Router prefixes contain no `{` or `}` characters and cannot declare path parameters | constraint | MAP-08, Section 5.8 | Proposed |
| CON-35 | Router prefix and route path contain only path content | `«Router»`, `«Route»` | Prefixes and paths contain no whitespace, control characters, backslashes, `//`, query strings, fragments, schemes, authorities or standalone `.` and `..` segments | constraint | MAP-04, MAP-08, Section 5.8 | Proposed |
| CON-36 | Route placeholders are complete named path segments | `«Route»` | Each placeholder occupies a whole segment and has exactly the form `{name}`, where `name` matches `[A-Za-z_][A-Za-z0-9_]*`; empty, unmatched, nested, embedded or converter-style placeholders are invalid | constraint | MAP-04, Section 5.8 | Proposed |
| CON-37 | Route path parameter names are not repeated | `«Route»` | No placeholder name occurs more than once in the same route path | constraint | MAP-04, Section 5.8 | Proposed |
| CON-38 | Route path inputs are required scalar values | Inputs matched by CON-08 | Each matching `in` parameter has multiplicity `1..1`, no default value and a supported scalar type from Section 5.8 | constraint | MAP-04, Section 5.8 | Proposed |
| CON-39 | Full route URL is the validated prefix plus path | `«Route»` | Concatenate the owning router's validated prefix and the route's validated path exactly; the result satisfies CON-35 to CON-37 and participates in application-wide uniqueness under CON-29 | constraint | MAP-04, MAP-08, CON-29, Section 5.8 | Proposed |
| CON-40 | Primary-key attribute is required and scalar | Entity identifying attributes and entity-association ends | Every property with `isID = true` is an entity-owned scalar attribute with no association and multiplicity `1..1`; association ends cannot be primary-key attributes | constraint | PROP-02, Section 5.9 | Proposed |
| CON-41 | Primary-key type is Integer or String | Entity's primary-key attribute | The identifying attribute uses the actual standard UML `Integer` or `String` type, mapped to `BIGINT` or `TEXT` respectively | constraint | Section 4.6, Section 5.9 | Proposed |
| CON-42 | Association properties have consistent membership and ownership | Entity association ends | Each member-end property's `association` refers to that association; its owner is either that association or the entity typed by the opposite end | constraint | REL-15, Section 5.9 | Proposed |
| CON-43 | Associated entities belong to the same database | Entity `Association` | Both end types are entities owned by the same `«DatabaseService»` component | constraint | REL-04, REL-15, Section 5.9 | Proposed |
| CON-44 | Association role names are usable and unambiguous | Entity association ends | Both ends have non-empty Python identifier role names that are not reserved keywords; roles interpreted as properties on the same entity are unique and do not duplicate scalar attribute names | constraint | Section 4.7, Section 5.9 | Proposed |
| CON-45 | Generated foreign-key column names do not collide | Referencing `«Entity»` | Each derived foreign-key name is distinct from scalar column names and every other generated foreign-key name in that table under the target SQL identifier rules | constraint | SQL-04, Section 5.9 | Proposed |
| CON-46 | Entity references participate in explicit associations | Entity-typed attributes owned by `«Entity»` | An attribute typed by another entity or by its own entity class participates in a valid association and is included in that association's member ends | constraint | REL-15, Section 5.9 | Proposed |
| CON-47 | Entity reference resolves to one mapped primary key | Reference end of an entity association | The target entity has exactly one valid primary-key attribute satisfying CON-11, CON-40 and CON-41, from which SQL and Python foreign-key types can be derived | constraint | SQL-05, Section 5.9 | Proposed |
| CON-48 | Scalar entity attributes are single-valued | Attributes owned by `«Entity»` with no association | Each scalar attribute has multiplicity `1..1` or `0..1`; collection-valued scalar columns require a separate mapping | constraint | PROP-01, SQL-01, Section 5.9 | Proposed |
| CON-49 | Method parameters reference supported types | Parameters of router, service and repository methods | Every input and return parameter resolves to the standard UML `String`, `Integer`, `Boolean` or `Real` type, or to a class with `«Entity»` applied | constraint | MAP-04, MAP-06 to MAP-09, Section 5.10 | Proposed |
| CON-50 | Method parameters use input or return directions | Parameters of router, service and repository methods | Only `in` and `return` directions are supported; `out` and `inout` are rejected | constraint | Section 5.10 | Proposed |
| CON-51 | Method has at most one return parameter | Router, service and repository methods | Each method has zero or one owned parameter with direction `return`; zero means no result value | constraint | Section 5.10 | Proposed |
| CON-52 | Method parameters use supported multiplicities | Parameters of router, service and repository methods | Inputs use `1..1` or `0..1`; returns use `1..1`, `0..1` or `0..*`, with collections allowed for both primitive and entity results | constraint | CON-38, Section 5.10 | Proposed |
| CON-53 | Method parameter defaults match their declarations | Parameters of router, service and repository methods | Input defaults follow the literal type and nullability rules in Section 5.10; return parameters have no default value | constraint | Section 5.10 | Proposed |
| CON-54 | Declared method usage connects one caller to one method | Operation-level `Dependency` (REL-16 to REL-19) | Each dependency connects exactly one route or service method to exactly one service or repository method in a permitted direction | constraint | REL-16 to REL-19, Section 5.10 | Proposed |
| CON-55 | Declared called methods resolve to owned operations | Called operations in REL-16 to REL-19 | Each target resolves to an actual operation owned by the referenced service or repository class, whose signature satisfies CON-49 to CON-53 | constraint | REL-16 to REL-19, Section 5.10 | Proposed |

### 5.1 EVL Implementation

The existing rules live in [`validation/microservices.evl`](../validation/microservices.evl);
each message starts with its `CON-xx` id. See the README for how to run it in Eclipse.
The dependency-chain proposal is not yet implemented there: CON-17 currently permits
only REL-16 at operation level, CON-21 still requires a direct route-to-repository
dependency, and CON-22 to CON-27 have not been added.
CON-28 to CON-39, which cover database access consistency, application-wide route
uniqueness, names, container roles and URL validation, are also not yet implemented.
The existing CON-02, CON-07 and CON-08 checks have not yet been updated to enforce
the stronger prefix, path and parameter requirements specified here.
CON-40 to CON-48 and the SQL mappings in Section 4.7 are not yet implemented.
The existing CON-11, CON-12, CON-18 and CON-19 checks also need updating for the
single-column key, scalar-property and supported-association requirements.
CON-49 to CON-55, covering method signatures, return types and declared method
usage, are also proposed and not yet implemented in EVL.

### 5.2 Route Call Chains and Isolated Router Scopes (CON-21, CON-24 to CON-27)

A **router scope** consists of one router and the services and repositories reached
by following its class dependencies (REL-10 to REL-13). Follow service-to-service
dependencies through any number of services, including utility services, and stop
at repositories. This is a logical scope derived from dependencies; it does not add
a stereotype or change ownership of the classes by their application component.

**Each router scope contains exactly one repository (CON-24), and two router scopes
cannot share that repository (CON-25).** Both direct router-to-repository access and
router-to-service-to-repository access count. Services may call further services,
but every repository dependency in the scope must lead to that same repository.

Services also belong to at most one router scope, including services used only for
utility methods (CON-26). Routes on the same router may share those services.
The router, its services and its repository must all be owned by the same
`«ApplicationService»` component (CON-27). These rules encapsulate access within
either `Router -> Repository` or `Router -> Service -> ... -> Repository`.

Repository exclusivity applies across different routers even when those routers
belong to the same application. A router with no repository, a router that reaches
two repositories, or two routers that reach the same repository fails validation.
CON-25 and CON-26 do not require an otherwise unused repository or service to have
a router scope.

Start with a route and follow its calls to service or repository methods. For each
service method, follow its calls to other service methods and repository methods,
including utility methods. These calls are modeled as dependencies using REL-16 to
REL-19, pointing from the route or service method making the call to the service or
repository method being called. Stop following a branch when it reaches a repository
method, and record which repository owns that method.

**All repository methods reached from one route must belong to exactly one repository.**
Count repositories across every branch of that route's calls. Multiple calls to the
same repository, including calls to different methods, count as one repository.
All routes on the same router must use the repository of that router's scope;
different routers must use different repositories. A route that reaches no repository
fails, as does a route that reaches two different repositories, even when each branch
uses only one. Only explicitly modeled method calls count: declaring that a service
depends on a repository does not identify which service method calls which repository
method.

A utility service method may finish without calling a repository. It does not need
its own repository call, provided the route's complete call chain reaches exactly one
repository. If a utility branch does access persistence, its repository must be the
same one used elsewhere in the route's call chain. Service methods that the route
never calls, directly or through other methods, do not affect its CON-21 repository
count. Their class dependencies still count toward the router scope under CON-24
to CON-27.

For example, these method calls form a valid route call chain:

```text
OrdersRouter.createOrder -> OrderService.createOrder
OrderService.createOrder -> OrderRepository.save
OrderService.createOrder -> PricingService.calculateTotal
```

`PricingService.calculateTotal` may finish without calling any other method. The corresponding class
dependencies must be `OrdersRouter -> OrderService`, `OrderService -> OrderRepository`,
and `OrderService -> PricingService` (CON-23). Each of those class dependencies is
supported by a method dependency (CON-22). Adding a call from
`PricingService.calculateTotal` to `PricingRepository.findPrice` would fail CON-21
because the route would then reach two distinct repositories.

### 5.3 Router and Service Dependencies Match Method Calls (CON-22, CON-23)

The following table shows the two levels of dependency that must agree:

| Dependency between classes | Matching method call |
|---|---|
| Router depends on a service | A route on that router calls a method on that service |
| Router depends on a repository | A route on that router calls a method on that repository |
| Service depends on a repository | A method on that service calls a method on that repository |
| Service depends on another service | A method on the first service calls a method on the second service, including a utility method |

**CON-22:** Each router or service dependency in the left column must have at least
one matching method call in the right column. For example, `OrderService` depending
on `OrderRepository` requires a method on `OrderService` to call a method on
`OrderRepository`. Individual routes may use only some of the dependencies declared
by their router or by the services they call.

**CON-23:** Each method call in the right column must have the matching class
dependency in the left column. For example, a route on `OrdersRouter` calling
`OrderService.createOrder` requires `OrdersRouter` to depend on `OrderService`.
Calls between methods on the same service are allowed by REL-19 without requiring
that service to depend on itself.

When checking CON-21, remember which methods have already been visited so repeated
calls, shared utilities or circular calls do not cause validation to run forever.
Rejecting circular calls would require a separate rule; CON-21 only counts repositories.

### 5.4 Repository Access Matches Application Database Dependencies (CON-28)

For every dependency from a repository to an entity (REL-14), identify the
`«ApplicationService»` component owning the repository (REL-03) and the
`«DatabaseService»` component owning the entity (REL-04). That same application
component must have a dependency on that same database component (REL-09).

Check every entity accessed by the repository. A dependency on a different database
does not satisfy this rule. For example, if a repository accesses an entity owned
by `OrdersDatabase`, the repository's application must depend on `OrdersDatabase`.
Repository and entity ownership are required by CON-09 and CON-10; CON-28 connects
those ownership rules to the application-to-database dependency.

### 5.5 Application Routes Have Unique Full URL Patterns (CON-29)

Check every route in every router owned by the same `«ApplicationService»`.
First validate prefixes and paths according to Section 5.8. Construct each full URL
by concatenating its router's `prefix` and its route's `path` exactly (CON-39).
For comparison, replace every supported `{name}` path parameter in that
full URL with the same placeholder marker, `{}`. Keep literal text, letter case,
slashes and trailing slashes unchanged. The pair consisting of the HTTP method
and this URL pattern must be unique within the application.

For example, `GET /orders/{id}` and `GET /orders/{order_id}` both compare as
`GET /orders/{}` and conflict, including when declared in different routers.
Likewise, different prefix/path combinations that produce the same full URL
conflict. `GET /orders/{id}` and `POST /orders/{id}` are allowed because their
HTTP methods differ. Separate applications may expose the same endpoint.

CON-04 remains the local check within a router; CON-29 also checks across routers
and ignores placeholder names when comparing URL patterns. This rule does not
reject a fixed route such as `/orders/latest` alongside `/orders/{id}`; those
patterns differ, and their registration order is a separate routing concern.

### 5.6 Required, Unique and Usable Names (CON-30 to CON-32)

CON-30 requires names for modeled application and database containers, router,
service, repository and entity classes, their owned methods, entity column
attributes, and non-return parameters of those methods. Names cannot be empty,
whitespace-only or padded with leading or trailing whitespace. A UML return
parameter may be unnamed because it describes a result rather than an input name.
These rules apply to application-model elements, rather than the profile's own
stereotype definitions and extension elements.

CON-31 checks names within these scopes:

| Elements whose names must be unique | Scope |
|---|---|
| Application and database containers, considered together | The same enclosing UML Model |
| Router, service and repository classes, considered together | The same owning application component |
| Entity classes | The same owning database component |
| Methods, including routes | The same owning class |
| Entity column attributes | The same owning entity class |
| Non-return parameters | The same owning method |

Classes or methods in different scopes may have the same name. Within a class,
different parameter signatures do not permit duplicate method names for this
Python target. Method-name uniqueness is separate from URL-pattern uniqueness:
two differently named route methods can still expose the same endpoint.

CON-32 requires class, method, column and non-return parameter names to be usable
as Python identifiers and not reserved Python keywords for the target version.
For example, `OrderService`, `create_order` and `order_id` are acceptable;
`order-service`, `create order`, `123id` and `class` are not. Compare Python names
using Python's identifier normalization so equivalent spellings cannot bypass
CON-31. See the [Python identifier rules](https://docs.python.org/3/reference/lexical_analysis.html#identifiers).
Container names are subject to CON-30 and CON-31 but are not Python identifiers;
for example, a container name such as `orders-service` is allowed by these naming
rules. SQL name rendering and any generated-name transformations require their
own mappings; these rules do not establish those mappings.

### 5.7 Application and Database Container Roles Are Exclusive (CON-33)

A UML Component may represent an application container with `«ApplicationService»`
or a database container with `«DatabaseService»`, but it cannot carry both roles.
Their common parent `«ContainerizedService»` describes inherited container
properties and does not count as an additional role. CON-33 does not require
ordinary UML Components to carry either stereotype.

### 5.8 Router Prefixes, Route Paths and Path Parameters (CON-02, CON-07, CON-08, CON-34 to CON-39)

A router prefix is required and static: it starts with `/`, contains at least one
fixed segment, and does not end with `/`. For example, `/orders` and
`/api/v1/orders` are valid. Empty prefixes, `/` alone and parameterized prefixes
such as `/shops/{shop_id}` are outside the current DSL. Put variable parameters
in route paths instead (CON-02, CON-34).

A route path extension is required. Use `/` for the router's base endpoint, or
one or more slash-prefixed segments such as `/{order_id}` or `/{order_id}/items`.
Only `/` itself may end with a slash. An empty path is invalid (CON-07).

For both prefix and path, reject whitespace anywhere, control characters,
backslashes, repeated slashes (`//`) and standalone `.` or `..` segments.
These values describe URL paths only: a hostname, URL scheme, query string (`?`)
or fragment (`#`) cannot be included. Query inputs are modeled separately from
the path. These checks also apply to the combined URL (CON-35).

Every brace in a route path must belong to a complete placeholder occupying one
whole segment. A placeholder has the form `{name}`, with its name matching
`[A-Za-z_][A-Za-z0-9_]*`. Reject `{}`, unmatched or nested braces,
`order-{id}`, and converter syntax such as `{id:int}`. A placeholder name may
occur only once per route path (CON-36, CON-37).

Each placeholder matches exactly one owned `in` parameter with the same name
(CON-08). That input is required and single-valued: its UML lower and upper bounds
are both `1`, and it has no default value (CON-38). Its name also satisfies CON-32.
Other route inputs may exist without appearing in the path, for example body or
query inputs; CON-08 does not require every input to be a path parameter.

The initial supported path-input types are the standard UML primitive types
`String`, `Integer`, `Boolean` and `Real`, using the Python mappings in Section 4.6.
Resolve the standard UML type rather than accepting
any custom type with the same name. Classes, collections, enumerations and custom
data types require an explicit scalar conversion mapping before they can be used
as path inputs. Infer path-value validation from the UML parameter type; do not
put type-converter syntax in the URL placeholder (CON-38).

Construct the full URL by exact concatenation, with no trimming, slash insertion,
slash removal or other automatic repair of invalid values (CON-39):

| Router prefix | Route path | Full URL | Result |
|---|---|---|---|
| `/orders` | `/` | `/orders/` | Valid base endpoint |
| `/orders` | `/{order_id}` | `/orders/{order_id}` | Valid with a matching required scalar input |
| `/orders` | `/{order_id}/items` | `/orders/{order_id}/items` | Valid with a matching required scalar input |
| `/orders/` | `/{id}` | `/orders//{id}` | Invalid trailing slash in prefix and repeated slash |
| `/orders` | `/{id}/` | `/orders/{id}/` | Invalid trailing slash in path extension |
| `/orders` | `/{id}/{id}` | `/orders/{id}/{id}` | Invalid repeated placeholder name |
| `/orders` | `/{id` | `/orders/{id` | Invalid unmatched brace |

The collection endpoint uses a trailing slash because its path extension is `/`;
other endpoints do not end with a slash. Compare valid full URLs and HTTP methods
using CON-29, including the rule that different placeholder names do not make
otherwise identical URL patterns distinct. These are DSL modeling conventions;
they do not rely on redirects or route-registration order to repair the model.

### 5.9 Single-Column Primary Keys and Association Consistency (CON-11, CON-12, CON-18, CON-19, CON-40 to CON-48)

Validate application-model entities and associations, excluding the profile's own
stereotype definitions and extension associations. A primary key is exactly one
owned scalar attribute with `isID = true`, multiplicity `1..1` and the standard
UML `Integer` or `String` type. No association-end property can carry the
identifying flag. SQL generation creates the database constraint that enforces
uniqueness of actual stored values; model validation checks the key definition.

Classify an owned property using its `association` reference. Without an
association, it must have a supported primitive type and scalar multiplicity;
an entity-typed property without an association is invalid. With an association,
it must occur among that association's member ends and is validated through the
relationship rules rather than translated as an ordinary scalar column.

Each association has two distinct end properties with entity types, one end at
`1..1` or `0..1`, and the other at `0..*`. Its entities share one database owner.
An end may be owned by the association itself or by the opposite end's entity
type. For example, `Order.customer: Customer` belongs to `Order`, whereas an
association-owned `customer` end is also valid if the opposite end identifies
`Order`. Do not require duplicate class-owned properties for association-owned
ends. A self-reference is permitted when the two distinct ends and their roles
make the same storage mapping unambiguous.

Interpret an end's role on the entity typed by the opposite end, even when that
property is owned by the association. Check role-name uniqueness within that
entity, including its scalar attribute names, using the Python identifier rules
from Section 5.6. This distinguishes multiple references to the same target,
such as `billing_customer` and `shipping_customer`. Separately check the derived
SQL column names for collisions with scalar columns and other foreign keys.

For every reference, resolve the target entity's single approved primary key.
The reference property retains its entity type; the transformation derives the
foreign key's SQL and Python types from the target key. There is no independently
modeled foreign-key scalar attribute or datatype selector in this design.
This keeps type consistency a generation invariant grounded in a validated key,
rather than comparing an entity-typed reference property to a primitive-typed key.

### 5.10 Method Signatures, Returns and Declared Usage (CON-49 to CON-55)

These rules apply to operations owned by `«Router»`, `«Service»` and
`«Repository»` classes. Router operations remain subject to the existing `«Route»`
requirements. Each input and return parameter must reference an actual supported
type: a standard UML `String`, `Integer`, `Boolean` or `Real`, or a class carrying
`«Entity»`. Matching a type name alone is insufficient; missing or unresolved
types, unstereotyped classes, router/service/repository classes, enumerations,
`UnlimitedNatural` and unapproved custom data types are rejected (CON-49).

Parameters have direction `in` or `return` only (CON-50). Each method has at most
one return parameter (CON-51). The return parameter may be unnamed, as allowed
by CON-30; its direction and type identify its role. A method with no return
parameter has no result value. This does not select an HTTP response status.

| Parameter role | Allowed multiplicity | Meaning |
|---|---|---|
| Input | `1..1` | One required primitive or entity value |
| Input | `0..1` | One optional primitive or entity value |
| Return | `1..1` | One required primitive or entity result |
| Return | `0..1` | One optional primitive or entity result |
| Return | `0..*` | A collection of primitive or entity results, which may be empty |

Other multiplicities are outside the initial method policy (CON-52). A return
collection uses its element type as `Parameter.type` and `0..*` as its
multiplicity; for example, a parameter typed by `Order` at `0..*` means a
collection of orders. It does not introduce a separate `List` UML type or a
collection-valued SQL column. Route path inputs retain the stricter CON-38
requirements: a required supported primitive, multiplicity `1..1` and no default.

For CON-53, a modeled non-null input default must use a compatible primitive
literal: `LiteralString` for `String`, `LiteralInteger` for `Integer`,
`LiteralBoolean` for `Boolean`, or `LiteralReal` for `Real`. `LiteralNull` is
allowed only for an optional input at `0..1`, including an optional entity input.
Non-null entity defaults and expression-based defaults require a future mapping
and are rejected initially. Absence of a default is allowed. Return parameters
have no modeled defaults; their values are produced by the method implementation.

Every operation-level dependency identifies one calling operation and one called
operation. Its direction is route-to-service-method, route-to-repository-method,
service-method-to-repository-method or service-method-to-service-method
(CON-54). The called operation must be an actual owned operation of the indicated
service or repository, rather than a method name stored as text or an unresolved
reference, and its signature must be valid (CON-55).

Existing constraints continue to check corresponding class dependencies and
router-scope isolation. Handwritten method bodies may calculate arguments and
transform results, so caller and called method signatures need not match.
These constraints validate declared usage; argument bindings, execution order and
proof that the handwritten implementation makes the call are separate concerns.

Entity inputs and primitive/entity returns are permitted by these model rules.
Generated Python/API schemas, field selection, collection rendering and HTTP
response handling remain generation decisions. Acceptance of an entity return
does not prescribe nested relationship expansion or introduce a new stereotype.

## 6. Open Modeling Decisions

The initial relationship directions, router-scope boundaries, key structure and
supported multiplicities are defined in Sections 4 and 5. The remaining questions
concern generation policies and optional extensions.

- **Primary-key value generation:** For the single-column `Integer` or `String` key allowed by CON-11 and CON-41, does the application supply the value, or does the database generate it? Define the generation policy and its effect on creation inputs. `isID = true` currently identifies the key without selecting a value-generation strategy.
- **SQL identifier rendering:** How are entity, scalar-column and generated foreign-key names quoted or transformed for PostgreSQL? Define identifier length handling and comparison rules so CON-45 checks exactly the names the generator emits. The foreign-key naming pattern in SQL-04 is already defined.
- **Python/API generation:** How do the primitive and entity parameters and collection returns permitted by Section 5.10 become Python/API types and schemas? Define input/output fields, relationship representation, collection rendering, placement of non-path inputs in requests, and HTTP response handling. Entity returns and their permitted multiplicities are already allowed by CON-49 to CON-52.
- **Route registration order:** How should generated routes with fixed and parameterized segments be ordered, for example `/orders/latest` and `/orders/{id}`? CON-29 permits these different patterns; generation needs a deterministic ordering policy.
- **Deferred date and datetime extension:** Should the DSL introduce shared extended UML primitives such as `Date` and/or `DateTime`? If added, define their Python/PostgreSQL mappings, timezone policy and validation of defaults and runtime values. The current baseline uses the primitive mappings in Section 4.6.
- **Deferred custom association actions:** Should UML `Association` gain a stereotype with an `onDelete` tagged value and an enumeration of supported actions? The current delete policy is `RESTRICT`; key updates use PostgreSQL's default `NO ACTION`. Any future overrides should be per association and include compatibility rules, such as requiring an optional reference for `SET NULL`. Decide separately whether an `onUpdate` override is needed.

The two deferred extensions are optional future work. The current baseline can
be implemented using its existing primitive types and fixed foreign-key actions.

## 7. Completion Checklist

Checked items record completed specification work. EVL validation and generation
remain separate implementation tasks; constraint rows retain their `Proposed`
status until their implementation and verification are reviewed.

- [x] Document all 9 concepts with unique mapping IDs, source elements, UML metaclasses, stereotypes and tagged-value mappings.
- [x] Define structural ownership, permitted class/method dependency directions, stereotype generalization and the `HttpMethod` literals.
- [x] Specify UML primitive-to-Python/PostgreSQL mappings and the unsupported scalar types (Section 4.6).
- [x] Define all 55 constraints with unique IDs (`CON-01` to `CON-55`), contexts, error/warning kinds and sources.
- [x] Specify router-scope isolation, one exclusive repository per router, service isolation and application-to-database consistency (CON-21 to CON-28).
- [x] Define naming, role exclusivity, prefix/path syntax, path-input validation and application-wide endpoint uniqueness (CON-02, CON-04, CON-07, CON-08, CON-29 to CON-39).
- [x] Define single-column primary keys, supported associations, foreign-key derivation, type/nullability rules and default deletion behavior (CON-11, CON-12, CON-18, CON-19, CON-40 to CON-48; SQL-01 to SQL-08).
- [x] Define method parameter types, directions, defaults, return counts/multiplicities and declared method usage (CON-49 to CON-55).
- [ ] Verify stereotype names, inherited tags, primitive type references and enumeration literals against the Papyrus profile and standard type library.
- [ ] Settle the baseline generation policies in Section 6; keep optional date/type and association-action extensions deferred unless selected for implementation.
- [ ] Update existing EVL checks CON-01 to CON-21 to match the current specification, including stronger URL/key/association rules, transitive repository access, inherited container-role detection and exclusion of profile definitions.
- [ ] Implement the missing EVL checks CON-22 to CON-55.
- [ ] Verify every constraint with positive and negative cases built from the profile, including scope isolation, URL conflicts, foreign-key derivation and method signatures; avoid relying on existing repository models as the specification.
- [ ] Define the YAML schema and implement/test the UML-to-YAML transformation, preserving type references, multiplicities, dependencies and derived SQL mappings.
- [ ] Implement/test YAML-to-Python/FastAPI, PostgreSQL and Docker generation, including scoped dependency wiring and declared-call guidance for handwritten methods.
- [ ] Verify generated applications and databases together, including key uniqueness, nullability, foreign keys, delete restrictions and the selected API representations.

