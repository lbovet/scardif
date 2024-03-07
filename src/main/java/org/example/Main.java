package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import com.networknt.schema.walk.JsonSchemaWalkListener;
import com.networknt.schema.walk.WalkEvent;
import com.networknt.schema.walk.WalkFlow;

import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
        schemaValidatorsConfig.addKeywordWalkListener(new RdfKeywordListener());

        final JsonSchemaFactory schemaFactory = JsonSchemaFactory
                .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)).addMetaSchema(JsonMetaSchema.getV202012())
                .build();

        var schemaSource = Main.class.getClassLoader().getResourceAsStream("person-schema.json");
        var document = new Scanner(Main.class.getClassLoader().getResourceAsStream("john.json"), "UTF-8").useDelimiter("\\A").next();
        var schema = schemaFactory.getSchema(schemaSource, schemaValidatorsConfig);

        schema.walk(document, InputFormat.JSON, true);
    }

    private static class RdfKeywordListener implements JsonSchemaWalkListener {
        @Override
        public WalkFlow onWalkStart(WalkEvent event) {
            JsonNode schemaNode = event.getSchemaNode();
            System.out.println(event.getSchemaLocation());
            System.out.println(event.getNode());
            System.out.println();
            return WalkFlow.CONTINUE;
        }

        @Override
        public void onWalkEnd(WalkEvent keywordWalkEvent, Set<ValidationMessage> validationMessages) {

        }
    }
}