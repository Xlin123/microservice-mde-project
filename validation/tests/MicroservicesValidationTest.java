import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

/** Behavioral tests built only from the profile and the canonical primitive library. */
public class MicroservicesValidationTest {
    static Path root;
    static Profile profile;
    static Model primitives;
    static ResourceSetImpl resources;
    static int serial = 0, cases = 0;
    static Set<Integer> negativeCoverage = new TreeSet<>();
    static boolean selected(String name) {
        String filter = System.getProperty("evl.filter");
        return filter == null || name.contains(filter);
    }

    static class Fixture {
        final Resource resource;
        final Model model;
        final Component app, db;
        final Class router, service, utility, repo, customer, order;
        final Operation route, action, save, calculate;
        final Parameter input;
        final Property customerKey, orderKey, customerEnd, ordersEnd;
        final Association association;
        final Dependency appDb, routerService, serviceRepo, serviceUtility;
        final Dependency routeAction, actionSave, actionCalculate;
        Fixture() {
            resource = resources.createResource(URI.createURI("memory:/case" + (++serial) + ".uml"));
            model = UMLFactory.eINSTANCE.createModel(); model.setName("Fixture");
            resource.getContents().add(model); model.applyProfile(profile);
            app = component("Application", "ApplicationService", 8000);
            db = component("Database", "DatabaseService", 5433);
            router = clazz(app, "OrderRouter", "Router"); tag(router, "Router", "prefix", "/orders");
            service = clazz(app, "OrderService", "Service");
            utility = clazz(app, "PricingService", "Service");
            repo = clazz(app, "OrderRepository", "Repository");
            customer = clazz(db, "Customer", "Entity"); order = clazz(db, "Order", "Entity");
            customerKey = attribute(customer, "id", "Integer", 1, 1); customerKey.setIsID(true);
            orderKey = attribute(order, "id", "Integer", 1, 1); orderKey.setIsID(true);
            attribute(order, "total", "Real", 1, 1);
            attribute(customer, "name", "String", 0, 1);
            attribute(customer, "active", "Boolean", 1, 1);
            route = operation(router, "createOrder"); role(route, "Route");
            tag(route, "Route", "method", ((Enumeration) profile.getOwnedType("HttpMethod")).getOwnedLiteral("POST"));
            tag(route, "Route", "path", "/{id}");
            input = parameter(route, "id", primitive("Integer"), 1, 1, ParameterDirectionKind.IN_LITERAL);
            parameter(route, null, order, 1, 1, ParameterDirectionKind.RETURN_LITERAL);
            action = operation(service, "createOrder");
            parameter(action, "id", primitive("Integer"), 1, 1, ParameterDirectionKind.IN_LITERAL);
            parameter(action, null, order, 1, 1, ParameterDirectionKind.RETURN_LITERAL);
            save = operation(repo, "save");
            parameter(save, "id", primitive("Integer"), 1, 1, ParameterDirectionKind.IN_LITERAL);
            parameter(save, null, order, 1, 1, ParameterDirectionKind.RETURN_LITERAL);
            calculate = operation(utility, "calculateTotal");
            parameter(calculate, "quantity", primitive("Integer"), 1, 1, ParameterDirectionKind.IN_LITERAL);
            parameter(calculate, null, primitive("Real"), 1, 1, ParameterDirectionKind.RETURN_LITERAL);
            appDb = dependency(app, db); routerService = dependency(router, service);
            serviceRepo = dependency(service, repo); serviceUtility = dependency(service, utility);
            routeAction = dependency(route, action); actionSave = dependency(action, save);
            actionCalculate = dependency(action, calculate); dependency(repo, order); dependency(repo, customer);
            association = (Association) db.createPackagedElement("CustomerOrders", UMLPackage.Literals.ASSOCIATION);
            customerEnd = order.createOwnedAttribute("customer", customer, 1, 1);
            ordersEnd = customer.createOwnedAttribute("orders", order, 0, -1);
            association.getMemberEnds().add(customerEnd); association.getMemberEnds().add(ordersEnd);
        }
        Component component(String name, String role, int hostPort) {
            Component c = (Component) model.createPackagedElement(name, UMLPackage.Literals.COMPONENT);
            role(c, role); tag(c, role, "hostPort", hostPort);
            tag(c, role, "containerPort", role.equals("DatabaseService") ? 5432 : 8080);
            if (role.equals("DatabaseService")) {
                tag(c, role, "databaseName", "fixture"); tag(c, role, "databaseVolume", "fixture-data");
            }
            return c;
        }
        Class clazz(Component owner, String name, String role) {
            Class c = (Class) owner.createPackagedElement(name, UMLPackage.Literals.CLASS);
            role(c, role); return c;
        }
        Dependency dependency(NamedElement from, NamedElement to) {
            Usage d = (Usage) model.createPackagedElement("uses" + serial + "_" + model.getPackagedElements().size(), UMLPackage.Literals.USAGE);
            d.getClients().add(from); d.getSuppliers().add(to); return d;
        }
        void remove(Element e) { e.destroy(); }
        Operation extraRoute(Class owner, String name, String path) {
            Operation op = operation(owner, name); role(op, "Route");
            tag(op, "Route", "method", ((Enumeration) profile.getOwnedType("HttpMethod")).getOwnedLiteral("POST"));
            tag(op, "Route", "path", path);
            var matcher = java.util.regex.Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)\\}").matcher(path);
            while (matcher.find()) parameter(op, matcher.group(1), primitive("Integer"), 1, 1, ParameterDirectionKind.IN_LITERAL);
            return op;
        }
        Operation extraScope(Component owner, String name, String prefix, String path) {
            Class r = clazz(owner, name + "Router", "Router"); tag(r, "Router", "prefix", prefix);
            Class repository = clazz(owner, name + "Repository", "Repository");
            Operation op = extraRoute(r, "fetch", path), target = operation(repository, "find");
            dependency(r, repository); dependency(op, target); dependency(repository, order);
            return op;
        }
    }
    static PrimitiveType primitive(String name) { return (PrimitiveType) primitives.getOwnedType(name); }
    static void role(Element e, String name) { e.applyStereotype(profile.getOwnedStereotype(name)); }
    static void tag(Element e, String role, String name, Object value) { e.setValue(profile.getOwnedStereotype(role), name, value); }
    static Operation operation(Class owner, String name) { return owner.createOwnedOperation(name, null, null); }
    static Property attribute(Class owner, String name, String type, int lower, int upper) {
        return owner.createOwnedAttribute(name, primitive(type), lower, upper);
    }
    static Parameter parameter(Operation owner, String name, Type type, int lower, int upper, ParameterDirectionKind direction) {
        Parameter p = owner.createOwnedParameter(name, type); p.setDirection(direction); p.setLower(lower); p.setUpper(upper); return p;
    }
    static Set<Integer> validate(Fixture f) throws Exception {
        EvlModule module = new EvlModule(); module.parse(root.resolve("validation/microservices.evl").toFile());
        if (!module.getParseProblems().isEmpty()) throw new AssertionError(module.getParseProblems().toString());
        InMemoryEmfModel model = new InMemoryEmfModel("Model", f.resource, UMLPackage.eINSTANCE);
        model.setExpand(true); model.setStoredOnDisposal(false);
        module.getContext().getModelRepository().addModel(model);
        Set<Integer> failures = new TreeSet<>();
        for (UnsatisfiedConstraint failure : module.execute()) {
            String message = failure.getMessage();
            if (!message.matches("CON-\\d{2} .*")) throw new AssertionError("Missing constraint ID: " + message);
            failures.add(Integer.parseInt(message.substring(4, 6)));
            int id = Integer.parseInt(message.substring(4, 6));
            if (failure.getConstraint().isCritique() != (id == 5 || id == 16))
                throw new AssertionError("Incorrect error/warning severity: " + message);
            if (Boolean.getBoolean("evl.verbose")) System.out.println(message);
        }
        return failures;
    }
    static void good(String name, Consumer<Fixture> change) throws Exception {
        if (!selected(name)) return;
        Fixture f = new Fixture(); change.accept(f);
        Set<Integer> failures = validate(f);
        if (!failures.isEmpty()) throw new AssertionError(name + " unexpectedly failed " + failures);
        cases++; resources.getResources().remove(f.resource);
        System.out.println("PASS " + name);
    }
    static void bad(String name, int expected, Consumer<Fixture> change) throws Exception {
        if (!selected(name)) return;
        Fixture f = new Fixture(); change.accept(f);
        Set<Integer> failures = validate(f);
        if (!failures.contains(expected)) throw new AssertionError(name + " expected CON-" + expected + " but got " + failures);
        negativeCoverage.add(expected); cases++; resources.getResources().remove(f.resource);
        System.out.println("PASS " + name + " detects CON-" + String.format("%02d", expected));
    }
    static void onlyBad(String name, int expected, Consumer<Fixture> change) throws Exception {
        if (!selected(name)) return;
        Fixture f = new Fixture(); change.accept(f);
        Set<Integer> failures = validate(f);
        if (!failures.equals(Set.of(expected))) throw new AssertionError(name + " expected only CON-" + expected + " but got " + failures);
        negativeCoverage.add(expected); cases++; resources.getResources().remove(f.resource);
        System.out.println("PASS " + name + " detects only CON-" + String.format("%02d", expected));
    }
    public static void main(String[] args) throws Exception {
        root = Path.of(args[0]);
        resources = new ResourceSetImpl(); UMLResourcesUtil.init(resources);
        profile = (Profile) resources.getResource(URI.createFileURI(root.resolve("MicroservicesProfile/MicroservicesProfile.profile.uml").toString()), true).getContents().get(0);
        primitives = (Model) resources.getResource(URI.createURI("pathmap://UML_LIBRARIES/UMLPrimitiveTypes.library.uml"), true).getContents().get(0);
        for (String role : List.of("ContainerizedService", "ApplicationService", "DatabaseService", "Entity", "Router", "Route", "Service", "Repository")) {
            if (profile.getOwnedStereotype(role) == null) throw new AssertionError("Missing profile stereotype " + role);
        }
        Set<String> methods = new HashSet<>();
        ((Enumeration) profile.getOwnedType("HttpMethod")).getOwnedLiterals().forEach(value -> methods.add(value.getName()));
        if (!methods.equals(Set.of("GET", "POST", "PUT", "PATCH", "DELETE"))) throw new AssertionError("Unexpected HttpMethod literals " + methods);
        good("valid service chain, utility branch, entity returns and class-owned association", f -> {});
        good("direct router/repository chain", f -> {
            f.remove(f.routerService); f.remove(f.routeAction);
            f.dependency(f.router, f.repo); f.dependency(f.route, f.save);
        });
        good("service and operation cycles terminate", f -> {
            f.dependency(f.utility, f.service); f.dependency(f.calculate, f.action);
            f.dependency(f.action, f.action);
        });
        good("optional entity and primitive collections are valid returns", f -> {
            f.route.getOwnedParameters().get(1).setLower(0);
            f.action.getOwnedParameters().get(1).setLower(0); f.action.getOwnedParameters().get(1).setUpper(-1);
            f.calculate.getOwnedParameters().get(1).setLower(0); f.calculate.getOwnedParameters().get(1).setUpper(-1);
        });
        good("association-owned ends", f -> {
            f.association.getOwnedEnds().add(f.customerEnd); f.association.getOwnedEnds().add(f.ordersEnd);
        });
        good("optional reference and string primary key", f -> {
            f.customerEnd.setLower(0); f.customerKey.setType(primitive("String"));
        });
        good("multiple routers may access the same entities through separate repositories", f -> {
            f.extraScope(f.app, "Other", "/other", "/{id}");
        });
        good("different applications may expose the same endpoint", f -> {
            Component other = f.component("OtherApplication", "ApplicationService", 8001);
            f.dependency(other, f.db); f.extraScope(other, "Other", "/orders", "/{id}");
        });
        good("unused repository and service are allowed", f -> {
            f.clazz(f.app, "UnusedService", "Service"); f.clazz(f.app, "UnusedRepository", "Repository");
        });
        good("routes on one router may share services and a repository", f -> {
            f.dependency(f.extraRoute(f.router, "other", "/other/{id}"), f.action);
        });
        good("utility persistence may use the same repository", f -> {
            f.dependency(f.utility, f.repo); f.dependency(f.calculate, f.save);
        });
        good("Python Unicode and soft keywords are valid names", f -> {
            f.service.setName("Échange"); f.utility.setName("match"); f.save.setName("type");
        });
        good("valid primitive and optional entity defaults", f -> {
            parameter(f.action, "label", primitive("String"), 0, 1, ParameterDirectionKind.IN_LITERAL)
                .createDefaultValue(null, null, UMLPackage.Literals.LITERAL_STRING);
            parameter(f.action, "quantity", primitive("Integer"), 1, 1, ParameterDirectionKind.IN_LITERAL)
                .createDefaultValue(null, null, UMLPackage.Literals.LITERAL_INTEGER);
            parameter(f.action, "active", primitive("Boolean"), 0, 1, ParameterDirectionKind.IN_LITERAL)
                .createDefaultValue(null, null, UMLPackage.Literals.LITERAL_BOOLEAN);
            parameter(f.action, "amount", primitive("Real"), 1, 1, ParameterDirectionKind.IN_LITERAL)
                .createDefaultValue(null, null, UMLPackage.Literals.LITERAL_REAL);
            parameter(f.action, "order", f.order, 0, 1, ParameterDirectionKind.IN_LITERAL)
                .createDefaultValue(null, null, UMLPackage.Literals.LITERAL_NULL);
        });

        bad("router ownership", 1, f -> f.model.getPackagedElements().add(f.router));
        bad("root-only prefix", 2, f -> tag(f.router, "Router", "prefix", "/"));
        bad("router helper lacks Route", 3, f -> operation(f.router, "helper"));
        bad("duplicate method/path", 4, f -> f.dependency(f.extraRoute(f.router, "duplicate", "/{id}"), f.action));
        bad("empty router warning", 5, f -> f.remove(f.route));
        bad("Route owned by service", 6, f -> f.service.getOwnedOperations().add(f.route));
        bad("empty path", 7, f -> tag(f.route, "Route", "path", ""));
        bad("missing placeholder input", 8, f -> f.route.getOwnedParameters().remove(f.input));
        bad("service ownership", 9, f -> f.db.getPackagedElements().add(f.service));
        bad("entity ownership", 10, f -> f.app.getPackagedElements().add(f.order));
        bad("missing primary key", 11, f -> f.orderKey.setIsID(false));
        bad("same-named custom primitive is not standard", 12, f -> {
            PrimitiveType fake = (PrimitiveType) f.model.createPackagedElement("Integer", UMLPackage.Literals.PRIMITIVE_TYPE);
            f.orderKey.setType(fake);
        });
        bad("inherited container port range", 13, f -> tag(f.app, "ApplicationService", "hostPort", 0));
        bad("duplicate inherited host port", 14, f -> tag(f.db, "DatabaseService", "hostPort", 8000));
        bad("empty database settings", 15, f -> tag(f.db, "DatabaseService", "databaseName", ""));
        bad("application database dependency warning", 16, f -> f.remove(f.appDb));
        bad("unsupported dependency direction", 17, f -> f.dependency(f.repo, f.router));
        bad("non-Entity association end", 18, f -> f.customerEnd.setType(f.service));
        bad("unsupported one-to-one association", 19, f -> { f.ordersEnd.setLower(0); f.ordersEnd.setUpper(1); });
        bad("multiple class roles", 20, f -> role(f.service, "Repository"));
        bad("route has no repository call", 21, f -> f.remove(f.actionSave));
        bad("class dependency has no method witness", 22, f -> f.remove(f.actionCalculate));
        bad("method dependency lacks class dependency", 23, f -> f.remove(f.routerService));
        bad("router reaches a second repository", 24, f -> {
            Class other = f.clazz(f.app, "OtherRepository", "Repository");
            f.dependency(f.service, other); f.dependency(f.action, operation(other, "save"));
        });
        onlyBad("two routers share a repository", 25, f -> {
            Class other = f.clazz(f.app, "OtherRouter", "Router"); tag(other, "Router", "prefix", "/other");
            f.dependency(other, f.repo); f.dependency(f.extraRoute(other, "fetch", "/{id}"), f.save);
        });
        bad("two routers share a service", 26, f -> {
            Class other = f.clazz(f.app, "OtherRouter", "Router"); tag(other, "Router", "prefix", "/other");
            f.dependency(other, f.service); f.dependency(f.extraRoute(other, "fetch", "/{id}"), f.action);
        });
        bad("scope crosses applications", 27, f -> {
            Component other = f.component("OtherApplication", "ApplicationService", 8001);
            f.dependency(other, f.db); other.getPackagedElements().add(f.utility);
        });
        bad("repository application lacks accessed database", 28, f -> f.remove(f.appDb));
        onlyBad("equivalent URL patterns across routers", 29, f -> f.extraScope(f.app, "Other", "/orders", "/{order_id}"));
        bad("missing modeled name", 30, f -> f.service.setName(null));
        onlyBad("duplicate normalized names", 31, f -> {
            f.service.setName("Kelvin"); f.utility.setName("Kelvin");
        });
        bad("Python keyword", 32, f -> f.save.setName("class"));
        bad("incompatible container roles", 33, f -> {
            role(f.app, "DatabaseService"); tag(f.app, "DatabaseService", "databaseName", "other");
            tag(f.app, "DatabaseService", "databaseVolume", "other-data");
        });
        bad("parameterized prefix", 34, f -> tag(f.router, "Router", "prefix", "/shops/{shop_id}"));
        bad("path contains whitespace", 35, f -> tag(f.route, "Route", "path", "/bad path"));
        bad("malformed placeholder", 36, f -> tag(f.route, "Route", "path", "/{id"));
        bad("repeated placeholders", 37, f -> tag(f.route, "Route", "path", "/{id}/{id}"));
        bad("optional path input", 38, f -> f.input.setLower(0));
        bad("invalid prefix/path composition", 39, f -> tag(f.router, "Router", "prefix", "/orders/"));
        bad("optional primary key", 40, f -> f.orderKey.setLower(0));
        bad("Real primary key", 41, f -> f.orderKey.setType(primitive("Real")));
        bad("association property owned by wrong entity", 42, f -> f.customer.getOwnedAttributes().add(f.customerEnd));
        bad("association crosses databases", 43, f -> {
            Component other = f.component("OtherDatabase", "DatabaseService", 5434);
            f.dependency(f.app, other); other.getPackagedElements().add(f.customer);
        });
        bad("association role collides with scalar attribute", 44, f -> f.customerEnd.setName("total"));
        bad("generated FK collides with column", 45, f -> attribute(f.order, "customer_id", "Integer", 1, 1));
        bad("entity attribute lacks association", 46, f -> f.order.createOwnedAttribute("other", f.customer));
        bad("referenced key has no valid mapping", 47, f -> f.customerKey.setType(primitive("Boolean")));
        bad("collection-valued scalar attribute", 48, f -> f.orderKey.setUpper(-1));
        bad("unmapped method parameter type", 49, f -> f.input.setType(f.service));
        bad("unsupported parameter direction", 50, f -> f.input.setDirection(ParameterDirectionKind.OUT_LITERAL));
        bad("multiple return parameters", 51, f -> parameter(f.action, null, f.order, 1, 1, ParameterDirectionKind.RETURN_LITERAL));
        bad("collection-valued input", 52, f -> f.input.setUpper(-1));
        bad("mismatched input default", 53, f -> f.input.createDefaultValue(null, null, UMLPackage.Literals.LITERAL_STRING));
        bad("multiple method callers on one dependency", 54, f -> f.routeAction.getClients().add(f.calculate));
        bad("called method has invalid signature", 55, f -> f.save.getOwnedParameters().get(0).setType(f.service));

        onlyBad("different routes on one router cannot select different repositories", 24, f -> {
            Class other = f.clazz(f.app, "OtherRepository", "Repository");
            f.dependency(f.router, other);
            f.dependency(f.extraRoute(f.router, "other", "/other/{id}"), operation(other, "find"));
        });
        onlyBad("utility-only service cannot be shared by two router scopes", 26, f -> {
            Operation other = f.extraScope(f.app, "Other", "/other", "/{id}");
            f.dependency((Class) other.getOwner(), f.utility); f.dependency(other, f.calculate);
        });
        bad("utility branch cannot introduce a second repository", 21, f -> {
            Class other = f.clazz(f.app, "OtherRepository", "Repository");
            f.dependency(f.utility, other); f.dependency(f.calculate, operation(other, "find"));
        });

        bad("missing prefix reports failures without aborting", 2, f -> tag(f.router, "Router", "prefix", null));
        bad("missing path reports failures without aborting", 7, f -> tag(f.route, "Route", "path", null));
        bad("missing primary-key type reports failures without aborting", 12, f -> f.customerKey.setType(null));
        bad("composite key is rejected", 11, f -> attribute(f.order, "secondKey", "Integer", 1, 1).setIsID(true));
        bad("association end cannot be an identifying attribute", 40, f -> f.customerEnd.setIsID(true));
        bad("n-ary association is rejected", 18, f -> f.association.createOwnedEnd("extra", f.order));
        bad("empty association reports failures without aborting", 18, f -> f.association.getMemberEnds().clear());
        bad("untyped association end reports failures without aborting", 18, f -> f.customerEnd.setType(null));
        bad("missing role name reports failures without aborting", 44, f -> f.customerEnd.setName(null));
        bad("required reference to optional input type is checked", 38, f -> f.input.setType(f.order));
        bad("duplicate placeholder inputs", 8, f -> parameter(f.route, "id", primitive("Integer"), 1, 1, ParameterDirectionKind.IN_LITERAL));
        bad("path default is rejected", 38, f -> f.input.createDefaultValue(null, null, UMLPackage.Literals.LITERAL_INTEGER));
        bad("null default on required input", 53, f -> f.input.createDefaultValue(null, null, UMLPackage.Literals.LITERAL_NULL));
        bad("return default is rejected", 53, f -> f.action.getOwnedParameters().get(1).createDefaultValue(null, null, UMLPackage.Literals.LITERAL_NULL));
        bad("missing parameter type", 49, f -> f.input.setType(null));
        bad("application filename cannot bypass validation", 49, f -> {
            f.resource.setURI(URI.createURI("memory:/UML.metamodel.uml")); f.input.setType(null);
        });
        bad("unmapped UnlimitedNatural parameter", 49, f -> f.input.setType(primitive("UnlimitedNatural")));
        bad("missing method target", 54, f -> f.routeAction.getSuppliers().clear());
        bad("unresolved method target", 55, f -> {
            Operation proxy = UMLFactory.eINSTANCE.createOperation();
            ((org.eclipse.emf.ecore.InternalEObject) proxy).eSetProxyURI(f.resource.getURI().appendFragment("missing-operation"));
            f.routeAction.getSuppliers().clear(); f.routeAction.getSuppliers().add(proxy);
        });
        bad("non-ASCII surrounding whitespace", 30, f -> f.app.setName("\u00a0Application"));
        bad("invisible identifier character", 32, f -> f.service.setName("Order\u200cService"));
        bad("foreign-key UTF-8 identifier truncation collision", 45, f -> {
            String prefix = "é".repeat(31) + "a";
            f.customerEnd.setName(prefix); attribute(f.order, prefix + "b", "Integer", 1, 1);
        });
        for (String path : List.of("/bad\\path", "/bad\npath", "/bad\u00a0path", "//id", "/./id", "/../id", "/id?query=1", "/id#fragment")) {
            bad("invalid path content " + path.replace('\n', ' '), 35, f -> tag(f.route, "Route", "path", path));
        }
        for (String path : List.of("/{}", "/{{id}}", "/item-{id}", "/{id:int}")) {
            bad("invalid placeholder " + path, 36, f -> tag(f.route, "Route", "path", path));
        }

        Set<Integer> expected = new TreeSet<>(); for (int id = 1; id <= 55; id++) expected.add(id);
        if (System.getProperty("evl.filter") == null) {
            if (!negativeCoverage.equals(expected)) throw new AssertionError("Missing constraint coverage: " + negativeCoverage);
            System.out.println("Passed " + cases + " behavioral cases; all 55 constraints have negative coverage and a valid baseline.");
        } else {
            if (cases == 0) throw new AssertionError("No cases match the filter");
            System.out.println("Passed " + cases + " selected behavioral cases.");
        }
    }
}
