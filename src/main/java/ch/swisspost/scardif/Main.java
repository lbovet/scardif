package ch.swisspost.scardif;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import com.networknt.schema.walk.JsonSchemaWalkListener;
import com.networknt.schema.walk.WalkEvent;
import com.networknt.schema.walk.WalkFlow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

public class Main {

    public final static String jsonSchemaNs = "https://jsonschema.org/";

    public static void main(String[] args) throws FileNotFoundException {

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
        var rootSchema = schemaFactory.getSchema(schemaSource, schemaValidatorsConfig);
        rootSchema.walk(document, InputFormat.JSON, true);

        listener.instances.values().forEach(System.out::println);

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
            if(instance.value != null) {
                var format = instance.schema.properties.get("format");
                var value = switch (instance.schema.properties.get("type").toString()) {
                    case "number" -> Values.literal(instance.value.asDouble());
                    case "integer" -> Values.literal(instance.value.asInt());
                    case "boolean"-> Values.literal(instance.value.asBoolean());
                    default -> format == null ? Values.literal(instance.value.asText()) :
                        switch (format.toString()) {
                            case "date" -> Values.literal(instance.value.asText(), CoreDatatype.XSD.DATE);
                            case "date-time" -> Values.literal(instance.value.asText(), CoreDatatype.XSD.DATETIME);
                            default -> Values.literal(instance.value.asText());
                        };
                };
                modelBuilder.add(instanceIri, RDF.VALUE, value);
                modelBuilder.add(instanceIri, RDFS.LABEL, instance.value.asText());
            }
        });

        listener.getSchemas().values().forEach(schema -> {
            schema.properties.forEach((property, value) -> {
                modelBuilder.add(schemaIri(schema.iri), Values.iri(jsonSchemaNs+property), Values.literal(value));
            });
        });

        RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, new FileOutputStream("target/out.trig"));
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

        public JsonNode value;

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
        public Map<String, Object> properties = new HashMap<>();

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
            var schemaIri = schemaLocation.replaceAll("/"+keyword.replace("$", "\\$")+"$", "");

            Schema schema = schemas.computeIfAbsent(schemaIri, k -> new Schema(schemaIri));

            var schemaValue = event.getSchemaNode().get(keyword);
            if(schemaValue.isValueNode() && !"$ref".equals(keyword)) {
                schema.properties.put(keyword, switch (schemaValue.getNodeType()) {
                    case NUMBER -> schemaValue.intValue();
                    case BOOLEAN -> schemaValue.booleanValue();
                    default -> schemaValue.textValue();
                });
            }

            var instanceValue = event.getNode();
            if(instanceValue.isValueNode()) {
                instance.value = instanceValue;
            }

            if(schemaLocation.endsWith("/$ref")) {
                instance.property = schemaIri;
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