package ch.swisspost.scardif.graph;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class Graph {

    private NotifyingSail topSail = new MemoryStore();
    private SailRepository repository;
    private List<Layer> layers = new ArrayList<>();

    public Graph addLayer(Layer layer) {
        layers.add(layer);
        return this;
    }

    private void applyLayers() {
        layers.forEach(layer -> layer.apply(this));
        layers.clear();
    }

    public void write(RDFFormat format, OutputStream output) {
        applyLayers();
        try(var connection = new SailRepository(topSail).getConnection()) {
            connection.add(new ModelBuilder()
                .setNamespace("", "https://example.org/")
                .add(":u", ":v", ":w").build());
            connection.export(Rio.createWriter(format, output));
        }
    }

    NotifyingSail getTopSail() {
        return topSail;
    }

    void setTopSail(NotifyingSail sail) {
        this.topSail = sail;
    }
}
