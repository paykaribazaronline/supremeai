package com.supremeai.codeflow.analyzer;

import com.supremeai.codeflow.model.CodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Design pattern and code smell detector
 */
@Component
public class PatternDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(PatternDetector.class);
    
    /**
     * Detect design patterns in code
     */
    public List<CodeRepository.PatternDetection> detectPatterns(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.PatternDetection> patterns = new ArrayList<>();
        
        for (CodeRepository.CodeFile file : files) {
            patterns.addAll(detectPatternsInFile(file));
        }
        
        // Detect cross-file patterns
        patterns.addAll(detectCrossFilePatterns(files));
        
        logger.info("Detected {} patterns across {} files", patterns.size(), files.size());
        return patterns;
    }
    
    /**
     * Detect patterns within a single file
     */
    private List<CodeRepository.PatternDetection> detectPatternsInFile(CodeRepository.CodeFile file) {
        List<CodeRepository.PatternDetection> patterns = new ArrayList<>();
        
        if (file.getClasses() == null || file.getClasses().isEmpty()) {
            return patterns;
        }
        
        for (CodeRepository.ClassInfo clazz : file.getClasses()) {
            // Singleton pattern
            if (isSingleton(clazz, file)) {
                patterns.add(createPattern("SINGLETON", 
                    "Singleton pattern detected", file, clazz.getLine(), 95));
            }
            
            // Factory pattern
            if (isFactory(clazz, file)) {
                patterns.add(createPattern("FACTORY",
                    "Factory pattern detected", file, clazz.getLine(), 90));
            }
            
            // Observer pattern
            if (isObserver(clazz, file)) {
                patterns.add(createPattern("OBSERVER",
                    "Observer pattern detected", file, clazz.getLine(), 85));
            }
            
            // Strategy pattern
            if (isStrategy(clazz, file)) {
                patterns.add(createPattern("STRATEGY",
                    "Strategy pattern detected", file, clazz.getLine(), 85));
            }
            
            // Decorator pattern
            if (isDecorator(clazz, file)) {
                patterns.add(createPattern("DECORATOR",
                    "Decorator pattern detected", file, clazz.getLine(), 80));
            }
            
            // Builder pattern
            if (isBuilder(clazz, file)) {
                patterns.add(createPattern("BUILDER",
                    "Builder pattern detected", file, clazz.getLine(), 90));
            }
            
            // Repository pattern
            if (isRepository(clazz, file)) {
                patterns.add(createPattern("REPOSITORY",
                    "Repository pattern detected", file, clazz.getLine(), 95));
            }
            
            // Service pattern
            if (isService(clazz, file)) {
                patterns.add(createPattern("SERVICE",
                    "Service pattern detected", file, clazz.getLine(), 90));
            }
            
            // Controller pattern
            if (isController(clazz, file)) {
                patterns.add(createPattern("CONTROLLER",
                    "Controller pattern detected", file, clazz.getLine(), 95));
            }
        }
        
        // Detect React hooks pattern
        if (file.getLanguage().equals("javascript") || file.getLanguage().equals("typescript")) {
            patterns.addAll(detectReactHooks(file));
        }
        
        // Detect God object (anti-pattern)
        for (CodeRepository.ClassInfo clazz : file.getClasses()) {
            if (isGodObject(clazz)) {
                patterns.add(createPattern("GOD_OBJECT",
                    "God object anti-pattern detected (class with too many responsibilities)",
                    file, clazz.getLine(), 95));
            }
        }
        
        return patterns;
    }
    
    /**
     * Detect cross-file patterns
     */
    private List<CodeRepository.PatternDetection> detectCrossFilePatterns(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.PatternDetection> patterns = new ArrayList<>();
        
        // Detect layered architecture
        patterns.addAll(detectLayeredArchitecture(files));
        
        // Detect dependency injection
        patterns.addAll(detectDependencyInjection(files));
        
        // Detect module boundaries
        patterns.addAll(detectModuleBoundaries(files));
        
        return patterns;
    }
    
    /**
     * Detect Singleton pattern
     */
    private boolean isSingleton(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        // Check for private constructor and static instance
        boolean hasPrivateConstructor = file.getFunctions() != null && file.getFunctions().stream()
            .anyMatch(f -> f.getName().equals(clazz.getName()) && f.getIsPrivate() != null && f.getIsPrivate());
        
        boolean hasStaticInstance = file.getContent() != null &&
            file.getContent().contains("static") &&
            file.getContent().contains(clazz.getName());
        
        return hasPrivateConstructor && hasStaticInstance;
    }
    
    /**
     * Detect Factory pattern
     */
    private boolean isFactory(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        // Check for create/instance methods returning different types
        boolean hasCreateMethods = clazz.getMethods() != null && clazz.getMethods().stream()
            .anyMatch(m -> m.getName() != null && m.getName().matches("(create|make|build|newInstance).*") &&
                m.getReturnType() != null && !m.getReturnType().equals("void"));
        
        // Check for interface/abstract class return types
        boolean returnsAbstraction = clazz.getMethods() != null && clazz.getMethods().stream()
            .anyMatch(m -> m.getReturnType() != null &&
                (m.getReturnType().contains("Interface") ||
                 m.getReturnType().contains("Abstract")));
        
        return hasCreateMethods || returnsAbstraction;
    }
    
    /**
     * Detect Observer pattern
     */
    private boolean isObserver(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        boolean hasSubscribe = clazz.getMethods() != null && clazz.getMethods().stream()
            .anyMatch(m -> m.getName() != null && m.getName().matches("(subscribe|register|attach|addListener).*"));
        
        boolean hasNotify = clazz.getMethods() != null && clazz.getMethods().stream()
            .anyMatch(m -> m.getName() != null && m.getName().matches("(notify|publish|emit|broadcast).*"));
        
        return hasSubscribe && hasNotify;
    }
    
    /**
     * Detect Strategy pattern
     */
    private boolean isStrategy(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        // Check for interchangeable algorithms
        boolean hasAlgorithmMethods = clazz.getMethods().stream()
            .anyMatch(m -> m.getName().matches("(execute|run|process|calculate|algorithm).*") &&
                !m.getReturnType().equals("void"));
        
        // Check for context that uses strategy
        boolean hasContext = file.getClasses().stream()
            .anyMatch(c -> c.getName().contains("Context") || c.getName().contains("Strategy"));
        
        return hasAlgorithmMethods || hasContext;
    }
    
    /**
     * Detect Decorator pattern
     */
    private boolean isDecorator(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        // Check for wrapper that implements same interface
        boolean hasWrapper = clazz.getImplementsInterfaces().stream()
            .anyMatch(i -> file.getClasses().stream()
                .anyMatch(c -> c.getImplementsInterfaces().contains(i)));
        
        // Check for component reference
        boolean hasComponent = clazz.getFields().stream()
            .anyMatch(f -> file.getClasses().stream()
                .anyMatch(c -> c.getImplementsInterfaces().contains(f)));
        
        return hasWrapper || hasComponent;
    }
    
    /**
     * Detect Builder pattern
     */
    private boolean isBuilder(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        // Check for fluent interface
        boolean hasFluentMethods = clazz.getMethods().stream()
            .filter(m -> !m.getReturnType().equals("void"))
            .anyMatch(m -> m.getReturnType().contains(clazz.getName()));
        
        // Check for build method
        boolean hasBuildMethod = clazz.getMethods().stream()
            .anyMatch(m -> m.getName().equals("build"));
        
        return hasFluentMethods && hasBuildMethod;
    }
    
    /**
     * Detect Repository pattern
     */
    private boolean isRepository(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        boolean hasRepoName = clazz.getName() != null &&
            (clazz.getName().toLowerCase().contains("repository") ||
            clazz.getName().toLowerCase().contains("dao"));
        
        boolean hasCrudMethods = clazz.getMethods() != null && clazz.getMethods().stream()
            .anyMatch(m -> m.getName() != null && m.getName().matches("(save|find|delete|update|create|get|list).*"));
        
        return hasRepoName || hasCrudMethods;
    }
    
    /**
     * Detect Service pattern
     */
    private boolean isService(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        boolean hasServiceName = clazz.getName().toLowerCase().contains("service");
        boolean hasServiceAnnotation = file.getContent() != null && 
            file.getContent().contains("@Service");
        
        return hasServiceName || hasServiceAnnotation;
    }
    
    /**
     * Detect Controller pattern
     */
    private boolean isController(CodeRepository.ClassInfo clazz, CodeRepository.CodeFile file) {
        boolean hasControllerName = clazz.getName().toLowerCase().contains("controller");
        boolean hasControllerAnnotation = file.getContent() != null && 
            (file.getContent().contains("@Controller") || 
             file.getContent().contains("@RestController"));
        
        return hasControllerName || hasControllerAnnotation;
    }
    
    /**
     * Detect God object anti-pattern
     */
    private boolean isGodObject(CodeRepository.ClassInfo clazz) {
        int methodCount = clazz.getMethods() != null ? clazz.getMethods().size() : 0;
        int fieldCount = clazz.getFields() != null ? clazz.getFields().size() : 0;
        int totalComplexity = clazz.getComplexity() != null ? clazz.getComplexity() : 0;
        
        // God object has too many methods, fields, and high complexity
        return methodCount > 20 || fieldCount > 20 || totalComplexity > 50;
    }
    
    /**
     * Detect React hooks pattern
     */
    private List<CodeRepository.PatternDetection> detectReactHooks(CodeRepository.CodeFile file) {
        List<CodeRepository.PatternDetection> patterns = new ArrayList<>();
        String content = file.getContent();
        
        if (content == null) {
            return patterns;
        }
        
        String[] hooks = {"useState", "useEffect", "useContext", "useReducer", 
                         "useCallback", "useMemo", "useRef", "useLayoutEffect"};
        
        for (String hook : hooks) {
            if (content.contains(hook + "(")) {
                patterns.add(createPattern("REACT_" + hook.toUpperCase(),
                    "React " + hook + " hook detected", file, 0, 95));
            }
        }
        
        return patterns;
    }
    
    /**
     * Detect layered architecture
     */
    private List<CodeRepository.PatternDetection> detectLayeredArchitecture(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.PatternDetection> patterns = new ArrayList<>();
        
        boolean hasController = files.stream()
            .anyMatch(f -> f.getClasses() != null && f.getClasses().stream()
                .anyMatch(c -> isController(c, f)));
        
        boolean hasService = files.stream()
            .anyMatch(f -> f.getClasses() != null && f.getClasses().stream()
                .anyMatch(c -> isService(c, f)));
        
        boolean hasRepository = files.stream()
            .anyMatch(f -> f.getClasses() != null && f.getClasses().stream()
                .anyMatch(c -> isRepository(c, f)));
        
        if (hasController && hasService && hasRepository) {
            patterns.add(createPattern("LAYERED_ARCHITECTURE",
                "Layered architecture (Controller-Service-Repository) detected",
                null, 0, 100));
        }
        
        return patterns;
    }
    
    /**
     * Detect dependency injection
     */
    private List<CodeRepository.PatternDetection> detectDependencyInjection(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.PatternDetection> patterns = new ArrayList<>();
        
        boolean hasAutowired = files.stream()
            .anyMatch(f -> f.getContent() != null && 
                f.getContent().contains("@Autowired"));
        
        boolean hasInject = files.stream()
            .anyMatch(f -> f.getContent() != null && 
                (f.getContent().contains("@Inject") || 
                 f.getContent().contains("constructor injection")));
        
        if (hasAutowired || hasInject) {
            patterns.add(createPattern("DEPENDENCY_INJECTION",
                "Dependency injection pattern detected", null, 0, 95));
        }
        
        return patterns;
    }
    
    /**
     * Detect module boundaries
     */
    private List<CodeRepository.PatternDetection> detectModuleBoundaries(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.PatternDetection> patterns = new ArrayList<>();
        
        // Group files by directory
        Map<String, List<CodeRepository.CodeFile>> modules = files.stream()
            .collect(Collectors.groupingBy(f -> {
                String path = f.getPath();
                int lastSlash = path.lastIndexOf('/');
                return (lastSlash > 0) ? path.substring(0, lastSlash) : "root";
            }));
        
        if (modules.size() > 3) {
            patterns.add(createPattern("MODULE_BOUNDARIES",
                "Clear module boundaries detected (" + modules.size() + " modules)",
                null, 0, 85));
        }
        
        return patterns;
    }
    
    /**
     * Create pattern detection object
     */
    private CodeRepository.PatternDetection createPattern(
            String type, String description, CodeRepository.CodeFile file, 
            int line, int confidence) {
        return CodeRepository.PatternDetection.builder()
            .patternType(type)
            .description(description)
            .file(file != null ? file.getPath() : "cross-file")
            .line(line)
            .confidence(confidence)
            .details(new HashMap<>())
            .build();
    }
}