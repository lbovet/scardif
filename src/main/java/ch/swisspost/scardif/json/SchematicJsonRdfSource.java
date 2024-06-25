package ch.swisspost.scardif.json;

import ch.swisspost.scardif.model.RdfSource;
import ch.swisspost.scardif.resource.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import com.networknt.schema.walk.JsonSchemaWalkListener;
import com.networknt.schema.walk.WalkEvent;
import com.networknt.schema.walk.WalkFlow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.*;
import java.util.stream.Stream;

/**
 * Generate a graph from a JSON document and an ontology derived from a JSON schema.
 */
@RequiredArgsConstructor
@Slf4j
public class SchematicJsonRdfSource implements RdfSource {
    public static final String JSON_SCHEMA_NS = "https://jsonschema.org#";

    private final Resource schema;
    private final Stream<Resource> documents;

    @Override
    public void generate(ModelBuilder modelBuilder) {
        documents.forEach(document -> {
            var rootIri = document.iri();
            var schemaSource = schema.readString();

            SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
            RdfKeywordListener listener = new RdfKeywordListener();
            schemaValidatorsConfig.addKeywordWalkListener(listener);

            final JsonSchemaFactory schemaFactory = JsonSchemaFactory
                    .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)).addMetaSchema(JsonMetaSchema.getV202012())
                    .build();

            schemaFactory.getSchema(schemaSource, schemaValidatorsConfig).walk(document.readString(), InputFormat.JSON, true);

            listener.getInstances().values().forEach(instance -> addInstance(modelBuilder, instance, rootIri));
            listener.getSchemas().values().forEach(schemaElement -> addSchema(modelBuilder, schemaElement));
        });
    }

    private void addInstance(ModelBuilder modelBuilder, Instance instance, String rootIri) {
        log.debug("Adding instance:\n"+instance);
        var instanceIri = dataIri(rootIri, instance.location);
        var link = Optional.ofNullable(instance.property).orElse(instance.schema.iri);
        if (instance.parent != null) {
            modelBuilder.add(dataIri(rootIri, instance.parent.location), schemaIri(link), instanceIri);
            modelBuilder.add(schemaIri(link), RDF.TYPE, RDF.PROPERTY);
            modelBuilder.add(schemaIri(link), RDFS.LABEL, shortName(link));
        }
        if (instance.value != null) {
            var format = instance.schema.properties.get("format");
            var value = switch (instance.schema.properties.get("type").toString()) {
                case "number" -> Values.literal(instance.value.asDouble());
                case "integer" -> Values.literal(instance.value.asInt());
                case "boolean" -> Values.literal(instance.value.asBoolean());
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
        Optional.ofNullable(instance.schema.properties.get("type")).ifPresent(type -> {
            modelBuilder.add(instanceIri, RDF.TYPE, Values.iri(JSON_SCHEMA_NS + type));
            modelBuilder.add(Values.iri(JSON_SCHEMA_NS + type), RDF.TYPE, RDFS.CLASS);
        });
        modelBuilder.add(instanceIri, RDF.TYPE, schemaIri(instance.schema.iri));
        var shortName = shortName(instance.schema.iri);
        if (!shortName.isBlank()) {
            modelBuilder.add(schemaIri(instance.schema.iri), RDFS.LABEL, shortName);
        }
        modelBuilder.add(schemaIri(instance.schema.iri), RDF.TYPE, RDFS.CLASS);
    }

    private void addSchema(ModelBuilder modelBuilder, Schema schema) {
        log.debug("Adding schema:\n"+schema);
        modelBuilder.add(schemaIri(schema.iri), RDF.TYPE, RDFS.CLASS);
        schema.properties.forEach((property, value) -> {
            switch (property) {
                case "type" -> {
                    modelBuilder.add(schemaIri(schema.iri), RDFS.SUBCLASSOF, Values.iri(JSON_SCHEMA_NS + value));
                    modelBuilder.add(Values.iri(JSON_SCHEMA_NS + value), RDF.TYPE, RDFS.CLASS);
                }
                case "description" -> {
                    modelBuilder.add(schemaIri(schema.iri), RDFS.COMMENT, Values.literal(value));
                }
            }
            modelBuilder.add(schemaIri(schema.iri), Values.iri(JSON_SCHEMA_NS + property), Values.literal(value));
        });
    }

    private IRI dataIri(String rootIri, String location) {
        return Values.iri(rootIri, location.replace("$.", "").replace("$","").replace(".", "/").replace("[", "/").replace("]", "").replace("/", "."));
    }

    private IRI schemaIri(String schemaIri) {
        var segments = schemaIri.split("#");
        return Values.iri(segments[0]+"#", segments.length > 1 ? segments[1].replaceFirst("^/", "").replace("/", "."): "");
    }

    private String shortName(String iri) {
        if(iri.contains("#")) {
            String result = iri.substring(iri.lastIndexOf("#")+1);
            result = result.substring(result.lastIndexOf("/")+1);
            return result;
        } else {
            return iri;
        }
    }

    @RequiredArgsConstructor

    private static class Instance {
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
    private static class Schema {
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
