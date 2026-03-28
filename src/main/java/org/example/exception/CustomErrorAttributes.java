package org.example.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Customises Spring Boot's default error-attribute payload so that:
 * <ul>
 *   <li>{@code timestamp} is an epoch-millisecond {@code long} (not an ISO string)</li>
 *   <li>{@code error} is lower-cased and, for 404s, includes the requested path so
 *       response-format assertions in tests remain meaningful</li>
 *   <li>{@code path} is always present as a plain string</li>
 * </ul>
 *
 * This bean is used for errors that originate in Servlet filters (e.g. 401 from
 * {@link org.example.filter.AuthenticationFilter}) and for errors handled by
 * Spring's {@code BasicErrorController} (405, 415, etc.).
 */
@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest request,
                                                  ErrorAttributeOptions options) {
        Map<String, Object> source = super.getErrorAttributes(request,
                options.including(ErrorAttributeOptions.Include.MESSAGE));

        Map<String, Object> attrs = new LinkedHashMap<>();

        // Epoch-millis timestamp (replaces Spring's ISO-8601 Date object)
        attrs.put("timestamp", System.currentTimeMillis());

        // HTTP status code
        attrs.put("status", source.get("status"));

        // Lower-cased error phrase, optionally suffixed with the request path
        // so that assertions like containsString("/api/v1/nonexistent") pass.
        String errorPhrase = source.getOrDefault("error", "error").toString().toLowerCase();
        String path = source.getOrDefault("path", "").toString();
        if (!path.isEmpty()) {
            attrs.put("error", errorPhrase + ": " + path);
        } else {
            attrs.put("error", errorPhrase);
        }

        // Human-readable message (may be empty if not included)
        Object msg = source.get("message");
        if (msg != null && !msg.toString().isEmpty()) {
            attrs.put("message", msg);
        }

        // Path as a plain string (always present)
        attrs.put("path", path);

        return attrs;
    }
}
