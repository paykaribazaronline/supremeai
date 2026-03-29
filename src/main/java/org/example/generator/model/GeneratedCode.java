package org.example.generator.model;

public class GeneratedCode {
    // This class will hold the generated code data
    private String code;
    private String language;

    public GeneratedCode(String code, String language) {
        this.code = code;
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "GeneratedCode{code='" + code + '\'
                + ", language='" + language + '\''
                + '}';
    }
}