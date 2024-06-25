package ch.swisspost.scardif.model;

import org.eclipse.rdf4j.model.util.ModelBuilder;

public interface RdfSource {
    public void generate(ModelBuilder modelBuilder);
}
