package ch.swisspost.scardif;

import com.networknt.schema.*;
import com.networknt.schema.walk.JsonSchemaWalkListener;
import com.networknt.schema.walk.WalkEvent;
import com.networknt.schema.walk.WalkFlow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.util.*;

public class Main {

    public final static String jsonSchemaNs = "https://www.w3.org/2019/wot/json-schema#";

    public static void main(String[] args) {

        var schemaSource = Main.class.getClassLoader().getResourceAsStream("person-schema.json");
        var document = new Scanner(Main.class.getClassLoader().getResourceAsStream("john.json"), "UTF-8").useDelimiter("\\A").next();

        var graphNs = "https://graph.example.org/";
        var instanceNs = "https://data.example.org/";
        var instanceName = "john";
        var rootIri = instanceNs+instanceName+"#";

        SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
        RdfKeywordListener listener = new RdfKeywordListener();
        schemaValidatorsConfig.addKeywordWalkListener(listener);

        final JsonSchemaFactory schemaFactory = JsonSchemaFactory
                .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)).addMetaSchema(JsonMetaSchema.getV202012())
                .build();
        var schema = schemaFactory.getSchema(schemaSource, schemaValidatorsConfig);

        schema.walk(document, InputFormat.JSON, true);

        listener.getInstances().values().forEach(System.out::println);

        Instance root = listener.instances.get("$");

        ModelBuilder modelBuilder = new ModelBuilder().namedGraph(graphNs+instanceName);

        listener.getInstances().values().forEach(instance -> {
            var instanceIri = dataIri(rootIri, instance.location);
            var link = Optional.ofNullable(instance.property).orElse(instance.schema.iri);
            if(instance.parent != null) {
                modelBuilder.add(dataIri(rootIri, instance.parent.location), schemaIri(link), instanceIri);
            }
            if("object".equals(instance.schema.properties.get("type"))) {
                modelBuilder.add(instanceIri, RDF.TYPE, schemaIri(instance.schema.iri));
            }
        });

        RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, System.out);
        writer.startRDF();
        var model = modelBuilder.build();
        model.getNamespaces().forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));
        for (var statement : model) {
            writer.handleStatement(statement);
        }
        writer.endRDF();
    }

    static IRI dataIri(String rootIri, String location) {
        return Values.iri(rootIri, location.replace("$.", "").replace("$","").replace(".", "/").replace("[", "/").replace("]", ""));
    }

    static IRI schemaIri(String schemaIri) {
        return Values.iri(schemaIri);
    }

    @RequiredArgsConstructor

    static class Instance {
        public final String location;

        public Instance parent;

        public String property;

        public Schema schema;

        public String toString() {
            return "location="+location+"\n"+
                    (parent != null ? ("parent="+parent.location+"\n"): "") +
                    (property != null ? ("property="+ property +"\n"): "") +
                    (schema != null ? ("schema=\n"+schema+"\n"): "");

        }
    }

    @RequiredArgsConstructor
    static class Schema {
        public final String iri;
        public Map<String, String> properties = new HashMap<>();

        public String toString() {
            return "  iri=" + iri + "\n" +
                    properties.entrySet().stream().map(entry -> "  " + entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + "\n" + b).orElse("");
        }
    }

    private static class RdfKeywordListener implements JsonSchemaWalkListener {

        @Getter
        private Map<String, Instance> instances = new HashMap<>();

        @Getter
        private Map<String, Schema> schemas = new HashMap<>();


        @Override
        public WalkFlow onWalkStart(WalkEvent event) {
            var instanceLocation = event.getInstanceLocation().toString();
            Instance instance = instances.computeIfAbsent(instanceLocation, k -> new Instance(instanceLocation));

            var schemaLocation = event.getSchemaLocation().toString();
            var keyword = event.getKeyword();
            var schemaIri = schemaLocation.replaceAll("/"+keyword+"$", "");

            Schema schema = schemas.computeIfAbsent(schemaIri, k -> new Schema(schemaIri));

            var node = event.getSchemaNode().get(keyword);
            if(node.isValueNode()) {
                schema.properties.put(keyword, node.asText());
            }

            if(schemaIri.endsWith("/$ref")) {
                instance.property = schemaIri.replaceAll("/\\$ref$", "");
            }

            var parent = event.getInstanceLocation().getParent();
            if(parent != null) {
                instance.parent = instances.get(parent.toString());
            }

            instance.schema = schema;
            return WalkFlow.CONTINUE;
        }

        @Override
        public void onWalkEnd(WalkEvent keywordWalkEvent, Set<ValidationMessage> validationMessages) {

        }
    }
}