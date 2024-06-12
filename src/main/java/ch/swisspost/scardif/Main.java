package ch.swisspost.scardif;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import com.networknt.schema.walk.JsonSchemaWalkListener;
import com.networknt.schema.walk.WalkEvent;
import com.networknt.schema.walk.WalkFlow;
import org.eclipse.rdf4j.model.util.ModelBuilder;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        var schemaSource = Main.class.getClassLoader().getResourceAsStream("person-schema.json");
        var document = new Scanner(Main.class.getClassLoader().getResourceAsStream("john.json"), "UTF-8").useDelimiter("\\A").next();

        SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
        ModelBuilder modelBuilder = new ModelBuilder();
        schemaValidatorsConfig.addKeywordWalkListener(new RdfKeywordListener("https://data.example.org/persons/john", modelBuilder));

        final JsonSchemaFactory schemaFactory = JsonSchemaFactory
                .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)).addMetaSchema(JsonMetaSchema.getV202012())
                .build();
        var schema = schemaFactory.getSchema(schemaSource, schemaValidatorsConfig);

        schema.walk(document, InputFormat.JSON, true);
    }

    private static class RdfKeywordListener implements JsonSchemaWalkListener {

        String instanceIri;
        ModelBuilder modelBuilder;
        Set<String> processedLinks = new HashSet<>();

        Set<String> getProcessedSchemaLocations = new HashSet<>();
        public RdfKeywordListener(String instanceIri, ModelBuilder modelBuilder) {
            this.instanceIri = instanceIri;
            this.modelBuilder = modelBuilder;
        }

        @Override
        public WalkFlow onWalkStart(WalkEvent event) {
            var instance = event.getInstanceLocation().toString().replace("$", "").replace(".", "/");
            var instanceParent = instance.replaceAll("/[^/]*$", "");
            var absoluteInstance = instanceIri + instance.replace("[", "/").replace("]", "");
            var absoluteInstanceParent = instanceIri + instanceParent.replace("[", "/").replace("]", "");
            var keyword = event.getKeyword();
            var schemaParentLocation = event.getSchemaLocation().toString().replaceAll("/"+keyword+"$", "").replace("/$ref", "");
            if (schemaParentLocation.endsWith("/items") && !schemaParentLocation.endsWith("/properties/items")) {
                schemaParentLocation = schemaParentLocation.replaceAll("/items$", "");
            }

            if(List.of("type", "$ref").contains(keyword)) {
                var instanceNode = event.getNode();
                if(keyword.equals("$ref") || "object".equals(event.getSchemaNode().get("type").asText()) || "array".equals(event.getSchemaNode().get("type").asText())) {
                    if("object".equals(event.getSchemaNode().get(keyword).asText())) {
                        System.out.println(absoluteInstance+ " " + "type" + " " + schemaParentLocation);
                    } else {
                        if (schemaParentLocation.endsWith("/items") && !schemaParentLocation.endsWith("/properties/items")) {
                            System.out.println(absoluteInstance.replaceAll("/[^/]*$", "") + " " + "item" + " " + absoluteInstance);
                        } else {
                            if (processedLinks.add(instance)) {
                                System.out.println(absoluteInstanceParent + " " + schemaParentLocation + " " + absoluteInstance);
                            }
                        }
                    }
                } else {
                    if (schemaParentLocation.endsWith("/items") && !schemaParentLocation.endsWith("/properties/items")) {
                        System.out.println(absoluteInstanceParent + " " + schemaParentLocation.replaceAll("/[^/]*$", "")+ " " + absoluteInstance);
                    } else  {
                        System.out.println(absoluteInstanceParent + " " + schemaParentLocation + " " + absoluteInstance);
                    }
                    System.out.println(absoluteInstance+ " value " + instanceNode);
                }
            }

            if (!List.of("$id", "properties", "items", "$ref").contains(keyword) && event.getSchemaNode().get(keyword).isValueNode()) {
                System.out.println(schemaParentLocation + " https://json-schema.org/"+keyword + " " + event.getSchemaNode().get(keyword));
            }

            System.out.println();
            return WalkFlow.CONTINUE;
        }

        @Override
        public void onWalkEnd(WalkEvent keywordWalkEvent, Set<ValidationMessage> validationMessages) {

        }
    }
}