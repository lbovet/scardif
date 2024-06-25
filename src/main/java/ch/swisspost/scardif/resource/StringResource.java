package ch.swisspost.scardif.resource;

public record StringResource(String iri, String content) implements Resource {
    @Override
    public String readString() {
        return content;
    }
}
