package be.everesst.socialriskdeclaration;

public enum Type {
    FILE(0),
    FOLDER(1);

    private final int value;

    Type(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
