public interface TemplateEngine {
    String render(String templateName, Map<String, Object> context);
}