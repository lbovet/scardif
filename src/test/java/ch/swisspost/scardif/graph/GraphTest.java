package ch.swisspost.scardif.graph;

import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

class GraphTest {
    @Test
    public void testLayers() {
        var output = new ByteArrayOutputStream();
        new Graph()
                .addLayer(
                        new ModelLayer(new ModelBuilder()
                                .setNamespace("", "https://example.org/")
                                .add(":s", ":p", ":o")))
                .addLayer(
                        new ConstructLayer("""
                                prefix : <https://example.org/>
                                
                                construct {
                                    
                                    :a :b :c .
                                } where {
                                    
                                }
                                """)
                )
                .write(RDFFormat.TRIG, System.out);

        assertEquals("""
                @prefix : <https://example.org/> .

                {
                  :s :p :o .
                  :s :p :x .
                }
                """, output.toString());
    }
}