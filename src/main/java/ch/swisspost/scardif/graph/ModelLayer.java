package ch.swisspost.scardif.graph;

import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.repository.Repository;

@RequiredArgsConstructor
public class ModelLayer implements Layer {

    private final ModelBuilder modelBuilder;

    @Override
    public void apply(Graph graph) {
        try( var connection = graph.getRepository().getConnection()) {
            connection.add(modelBuilder.build());
        }
    }
}
