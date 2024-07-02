package ch.swisspost.scardif.model;

import org.eclipse.rdf4j.model.util.ModelBuilder;

public interface ModelSource {
    public void generate(ModelBuilder modelBuilder);
}
