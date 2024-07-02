package ch.swisspost.scardif.graph;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.Reader;

@RequiredArgsConstructor
public class RdfLayer implements Layer {
    private final Reader reader;
    private final RDFFormat format;

    @Override
    @SneakyThrows
    public void apply(Graph graph) {
        try(var connection = new SailRepository(graph.getTopSail()).getConnection()) {
            connection.add(reader, RDFFormat.TURTLE);
        }
    }
}
