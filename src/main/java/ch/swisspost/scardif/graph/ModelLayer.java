package ch.swisspost.scardif.graph;

import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.repository.sail.SailRepository;

@RequiredArgsConstructor
public class ModelLayer implements Layer {

    private final ModelBuilder modelBuilder;

    @Override
    public void apply(Graph graph) {
        try( var connection = new SailRepository(graph.getTopSail()).getConnection()) {
            connection.add(modelBuilder.build());
        }
    }
}
