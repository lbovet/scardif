package ch.swisspost.scardif.json;

import ch.swisspost.scardif.resource.StringResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.stream.Stream;

class SchematicJsonRdfSourceTest {
    @Test
    public void testJohn() throws IOException {
        var schemaContent = new Scanner(SchematicJsonRdfSourceTest.class.getClassLoader().getResourceAsStream("person-schema.json"), StandardCharsets.UTF_8).useDelimiter("\\A").next();
        var documentContent = new Scanner(SchematicJsonRdfSourceTest.class.getClassLoader().getResourceAsStream("john.json"), StandardCharsets.UTF_8).useDelimiter("\\A").next();

        var schema = new StringResource(new ObjectMapper().readTree(schemaContent).get("$id").asText(), schemaContent);
        var document = new StringResource("https://data.example.org/john#", documentContent);

        var modelBuilder = new ModelBuilder().namedGraph("https://graph.example.org/people");
        new SchematicJsonModelSource(schema, Stream.of(document)).generate(modelBuilder);

        RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, new FileOutputStream("target/out.trig"));
        writer.startRDF();
        var model = modelBuilder.build();
        model.getNamespaces().forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));
        for (var statement : model) {
            writer.handleStatement(statement);
        }
        writer.endRDF();
    }
}