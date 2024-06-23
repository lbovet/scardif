package ch.swisspost.scardif.stack;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

@NoArgsConstructor
@AllArgsConstructor
public abstract class DefaultGraph implements Graph {
    private Repository repository = new SailRepository(new MemoryStore());

    @Override
    public Graph addLayer(Layer layer) {
        return null;
    }
}
