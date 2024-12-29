package io.github.plantaest.citron.enumeration;

public enum Model {
    VIWIKI_MODEL_V1("viwiki_model_v1", 1);

    private final String id;
    private final int number;

    Model(String id, int number) {
        this.id = id;
        this.number = number;
    }

    public String getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public static Model getDefaultModel() {
        return Model.VIWIKI_MODEL_V1;
    }
}
