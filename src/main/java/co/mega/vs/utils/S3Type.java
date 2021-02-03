package co.mega.vs.utils;

public enum S3Type {

    AWS("aws"), HW("hw");

    String name;

    S3Type(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}