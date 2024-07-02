package ch.swisspost.scardif.graph;

import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;

@RequiredArgsConstructor
public class ConstructLayer implements Layer {

    private final String query;

    @Override
    public void apply(Graph graph) {
        graph.setTopSail(new CustomGraphQueryInferencer(graph.getTopSail(), QueryLanguage.SPARQL, query, ""));
    }
}
