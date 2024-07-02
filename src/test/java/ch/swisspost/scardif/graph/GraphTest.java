package ch.swisspost.scardif.graph;

import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

class GraphTest {
    @Test
    public void testLayers() throws IOException {
        var output = new ByteArrayOutputStream();
        new Graph()
                .addLayer(
                        new ConstructLayer("""
                                PREFIX : <http://foo.org/bar#>
                                CONSTRUCT {
                                	?uncle :uncleOf ?nephew
                                } WHERE {
                                	?uncle :brotherOf/:parentOf ?nephew
                                }
                                """)
                )
                .addLayer(new RdfLayer(new StringReader("""
                    @prefix : <http://foo.org/bar#> .
                    :John01 :parentOf  :John02, :Peter .
                    :Jason  :brotherOf :John01 .
                    :Matt   :brotherOf :John01 .
                    """), RDFFormat.TURTLE)
                )
                .write(RDFFormat.TRIG, System.out);
        /*
        assertEquals("""
                @prefix : <https://example.org/> .

                {
                  :s :p :o .
                  :s :p :x .
                }
                """, output.toString());*/
    }
}