package ch.swisspost.scardif;

import org.eclipse.rdf4j.model.util.ModelBuilder;

public interface RdfSource {
    public void generate(ModelBuilder modelBuilder);
}
