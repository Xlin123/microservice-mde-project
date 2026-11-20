# microservice-mde-project

## Running the EVL validation

The canonical rules in `validation/microservices.evl` implement CON-01 through
CON-55 in Section 5 of `documentation/mappings.md`. Each message starts with its
`CON-xx` id. CON-05 and CON-16 are warnings; all other rules report errors.

Prerequisites: Eclipse with Papyrus and Epsilon installed (Help, Install New Software,
`https://download.eclipse.org/epsilon/updates/`, pick "Epsilon Core" and "Epsilon EMF
Integration"). Import the profile project and the project containing your application model.

1. Open `validation/microservices.evl` and choose Run As, EVL Validation.
2. On the Models tab add a model of type **UML model**:
   - Name: `Model`
   - Model file: the `.uml` application model to validate, with `MicroservicesProfile` applied
   - Tick "Read on load", untick "Store on disposal".
3. Run. Failures appear in the Validation view; `constraint` rows are errors, `critique`
   rows are warnings.

Save the launch configuration as `validation/microservices.launch` (Common tab, Shared file)
so the team gets the same settings.

The validator follows method dependencies through service and utility branches,
checks router scopes independently through class dependencies, and terminates on
cycles. Inherited container roles are recognized. Profile definitions, stereotype
extensions and canonical UML libraries are excluded from application validation.
Standard primitive types are checked by library identity, not just by name.

## Running the validation tests without Eclipse

With Python 3, a JDK, and Papyrus/Epsilon jars installed locally, run:

```bash
python3 validation/run_tests.py --plugins /path/to/Papyrus/plugins
```

Alternatively set `EPSILON_PLUGINS`. The runner also recognizes
`~/Applications/papyrus-7.1/Papyrus/plugins` by default. It compiles the test harness
into a temporary directory and runs the actual EVL engine. It does not download
dependencies or load or modify any repository application model.

The tests create isolated in-memory models from the shared profile and standard
primitive library. They cover valid direct and service-mediated calls, utility
cycles, association-owned and class-owned ends, optional and collection returns,
all 55 constraint failures, and malformed or unresolved model elements.

## Validation policies

CON-45 currently compares quoted, case-preserving PostgreSQL identifiers after
the standard 63-byte UTF-8 identifier limit, including multibyte truncation
collisions. The generator must use the same policy, or this comparison must be
updated when the identifier-generation decision in mappings.md is settled.

Python names use NFKC normalization and the Java runtime's Unicode identifier
tables, with Python reserved keywords excluded. Non-ASCII naming requires a Java
runtime whose Unicode repertoire covers the target Python identifiers.

These checks validate the declared model. Argument binding, handwritten method
behavior, API serialization, and SQL/YAML/code generation remain separate work.
