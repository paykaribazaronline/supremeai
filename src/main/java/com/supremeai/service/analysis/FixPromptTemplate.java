package com.supremeai.service.analysis;

public class FixPromptTemplate {
    private String templateName;
    private String template;
    private String outputFormat;

    public FixPromptTemplate() {}

    public FixPromptTemplate(String templateName, String template, String outputFormat) {
        this.templateName = templateName;
        this.template = template;
        this.outputFormat = outputFormat;
    }

    public static FixPromptTemplateBuilder builder() {
        return new FixPromptTemplateBuilder();
    }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public String render(FixContext context) {
        return template
            .replace("{filePath}", context.getFilePath())
            .replace("{lineNumber}", String.valueOf(context.getLineNumber()))
            .replace("{findingMessage}", context.getFindingMessage())
            .replace("{suggestion}", context.getSuggestion())
            .replace("{codeSnippet}", context.getCodeSnippet())
            .replace("{severity}", context.getSeverity())
            .replace("{category}", context.getCategory())
            .replace("{language}", context.getLanguage());
    }

    public static class FixPromptTemplateBuilder {
        private String templateName;
        private String template;
        private String outputFormat;

        public FixPromptTemplateBuilder templateName(String templateName) { this.templateName = templateName; return this; }
        public FixPromptTemplateBuilder template(String template) { this.template = template; return this; }
        public FixPromptTemplateBuilder outputFormat(String outputFormat) { this.outputFormat = outputFormat; return this; }

        public FixPromptTemplate build() {
            return new FixPromptTemplate(templateName, template, outputFormat);
        }
    }

    public static class FixContext {
        private String filePath;
        private int lineNumber;
        private String findingMessage;
        private String suggestion;
        private String codeSnippet;
        private String severity;
        private String category;
        private String language;

        public FixContext() {}

        public FixContext(String filePath, int lineNumber, String findingMessage, String suggestion, String codeSnippet, String severity, String category, String language) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.findingMessage = findingMessage;
            this.suggestion = suggestion;
            this.codeSnippet = codeSnippet;
            this.severity = severity;
            this.category = category;
            this.language = language;
        }

        public static FixContextBuilder builder() {
            return new FixContextBuilder();
        }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

        public String getFindingMessage() { return findingMessage; }
        public void setFindingMessage(String findingMessage) { this.findingMessage = findingMessage; }

        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

        public String getCodeSnippet() { return codeSnippet; }
        public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public static class FixContextBuilder {
            private String filePath;
            private int lineNumber;
            private String findingMessage;
            private String suggestion;
            private String codeSnippet;
            private String severity;
            private String category;
            private String language;

            public FixContextBuilder filePath(String filePath) { this.filePath = filePath; return this; }
            public FixContextBuilder lineNumber(int lineNumber) { this.lineNumber = lineNumber; return this; }
            public FixContextBuilder findingMessage(String findingMessage) { this.findingMessage = findingMessage; return this; }
            public FixContextBuilder suggestion(String suggestion) { this.suggestion = suggestion; return this; }
            public FixContextBuilder codeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; return this; }
            public FixContextBuilder severity(String severity) { this.severity = severity; return this; }
            public FixContextBuilder category(String category) { this.category = category; return this; }
            public FixContextBuilder language(String language) { this.language = language; return this; }

            public FixContext build() {
                return new FixContext(filePath, lineNumber, findingMessage, suggestion, codeSnippet, severity, category, language);
            }
        }
    }
}
