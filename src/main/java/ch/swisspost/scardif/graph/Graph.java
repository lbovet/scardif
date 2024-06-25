package ch.swisspost.scardif.graph;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class Graph {
    private Repository repository = new SailRepository(new MemoryStore());

    private List<Layer> layers = new ArrayList<>();

    public Graph addLayer(Layer layer) {
        layers.add(layer);
        return this;
    }

    private void applyLayers() {
        layers.forEach(layer -> layer.apply(this));
        layers.clear();
    }

    public void write(RDFFormat format, OutputStream output) throws FileNotFoundException {
        applyLayers();
        try(var connection = repository.getConnection()) {
            RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, new FileOutputStream("target/out.trig"));
            writer.startRDF();
            connection.export(writer);
            writer.endRDF();
        }
    }

    Repository getRepository() {
        return repository;
    }
}
