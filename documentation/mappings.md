# Microservice UML Mapping Specification

This document defines how microservice concepts map to UML elements and stereotypes, as well as the UML relationships used between them. It documents the **modeling rules**; it does not imply that code generation or OCL validation is already implemented.

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
| Endpoint path (e.g., `/{user_id}`) | `path` | String |

*When a shared `«Router».prefix` is used, the effective URL is the prefix plus the endpoint path.*

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
| Mapping rule | Represent each modeled database table as a UML Class with `«Entity»`. Column and key mappings remain to be specified. |


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
| Shared router URL prefix (e.g., `/users`) | `prefix` | String |

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
| REL-06 | `«Entity»` | `ownedAttribute` | `Property` | Entity owns SQL column definitions |
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

**Notes:**
- Routers may bypass the Service layer for simple operations.
- Service-to-Service dependencies are optional.
- Dependencies do not imply structural ownership.

### 4.3 Association Relationships

These mappings define structural relationships between
database entities.

| ID | Source | UML Relationship | Target | Meaning |
|---|---|---|---|---|
| REL-15 | `«Entity»` | `Association` | `«Entity»` | Represents a relationship between SQL tables, potentially generating foreign keys |

**Constraints:**
- Associations may specify multiplicities (1, 0..1, 0..*).
- Foreign-key generation requires mapping rules for the
  referencing and referenced columns.
- Entities do not have to participate in Associations.

### 4.4 UML Property and Type Mappings

These mappings use existing UML features rather than
creating additional stereotypes or dependencies.

| ID | UML Element / Feature | Target Concept | Mapping Rule |
|---|---|---|---|
| PROP-01 | `«Entity»::ownedAttribute` | SQL column | Each owned Property generates a database column |
| PROP-02 | `Property::isID = true` | SQL primary key | Identifying Properties generate primary-key constraints |
| PROP-03 | `Property::type` | SQL column type | UML types are translated to PostgreSQL-compatible types |
| PROP-04 | `«Route».method` typed by `HttpMethod` | HTTP method | Enumeration value determines the endpoint HTTP method |

**Primary-key constraints:**
- Each Entity must have at least one identifying Property.
- Multiple identifying Properties may form a composite key.
- SQL column types must have valid PostgreSQL mappings.

### 4.5 Stereotype Generalization

These relationships exist in the UML Profile, rather
than between individual application-model instances.

| Child Stereotype | UML Relationship | Parent Stereotype | Meaning |
|---|---|---|---|
| `«ApplicationService»` | `Generalization` | `«ContainerizedService»` | Inherits common container properties |
| `«DatabaseService»` | `Generalization` | `«ContainerizedService»` | Inherits common container properties |

---

## 5. Constraints and Validation

Keep all OCL/validation rules here instead of repeating them in every element mapping.

| ID | Candidate constraint | Status |
|---|---|---|



## 6. Open Modeling Decisions

- **Completeness:** Add any missing permissible UML relationships before declaring Section 4 an exhaustive allowlist.

## 7. Completion Checklist

- [x] All 9 listed concepts have unique mapping IDs.
- [x] Each mapping records source, UML metaclass, stereotype, and mapping rule.
- [x] Specified tagged values and HTTP literals are preserved.
- [x] Existing relationship candidates are collected in one section.
- [ ] Verify stereotype names, tag definitions, and enumeration literals against the actual Papyrus profile.
- [ ] Resolve open relationship semantics and define permitted multiplicities.
- [ ] Implement and test approved constraints and model-to-code transformations.

