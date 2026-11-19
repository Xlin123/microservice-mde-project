# microservice-mde-project

## Running the EVL validation

The rules in `validation/microservices.evl` implement Section 5 of
`documentation/mappings.md`. Each message starts with its `CON-xx` id.

Prerequisites: Eclipse with Papyrus and Epsilon installed (Help, Install New Software,
`https://download.eclipse.org/epsilon/updates/`, pick "Epsilon Core" and "Epsilon EMF
Integration"). Import `MicroservicesProfile` and `MicroServiceModels` as existing projects.

1. Open `validation/microservices.evl` and choose Run As, EVL Validation.
2. On the Models tab add a model of type **UML model**:
   - Name: `Model`
   - Model file: `/MicroServiceModels/MicroServiceModels.uml`
   - Tick "Read on load", untick "Store on disposal".
3. Run. Failures appear in the Validation view; `constraint` rows are errors, `critique`
   rows are warnings.

Save the launch configuration as `validation/microservices.launch` (Common tab, Shared file)
so the team gets the same settings.

Known failures on the booking reference model:

- CON-10, because the entities are owned by `booking-service` rather than `booking-db`.
  See mappings.md Section 6.
- CON-21 on all four routes, because `BookingRepository` has no operations yet and no
  Route has a Dependency to one.
