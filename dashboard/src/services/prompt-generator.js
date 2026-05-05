
/**
 * Prompt Generator Service
 * Converts code analysis into AI-ready prompts
 * Generates 3 prompt types: BUILD, ANALYZE, ASK
 */

class PromptGenerator {
  constructor() {
    this.templates = {
      build: this.generateBuildPrompt.bind(this),
      analyze: this.generateAnalyzePrompt.bind(this),
      ask: this.generateAskPrompt.bind(this),
    };
  }

  /**
   * Generate all prompt types
   */
  generate(repoMeta, fileTree, parsedFiles, analysis) {
    return {
      build: this.generateBuildPrompt(repoMeta, fileTree, parsedFiles, analysis),
      analyze: this.generateAnalyzePrompt(repoMeta, fileTree, parsedFiles, analysis),
      ask: this.generateAskPrompt(repoMeta, fileTree, parsedFiles, analysis),
    };
  }

  /**
   * Generate BUILD prompt (recreate this project)
   */
  generateBuildPrompt(repoMeta, fileTree, parsedFiles, analysis) {
    const keyComponents = this.summarizeKeyComponents(parsedFiles);
    const architecture = this.summarizeArchitecture(parsedFiles, analysis);
    const dependencies = this.formatDependencies(parsedFiles);
    const patterns = analysis.patterns || [];
    const security = analysis.securityIssues || [];

    return `You are an expert software architect and developer. Based on the following repository analysis, generate a complete implementation plan and starter code.

REPOSITORY: ${repoMeta.full_name}
DESCRIPTION: ${repoMeta.description || 'No description provided'}
LANGUAGE: ${repoMeta.language || 'Multiple'}
STARS: ${repoMeta.stars || 'N/A'}
TOPICS: ${(repoMeta.topics || []).join(', ') || 'N/A'}

ARCHITECTURE OVERVIEW:
${architecture}

KEY COMPONENTS:
${keyComponents}

DEPENDENCY GRAPH:
${dependencies}

DESIGN PATTERNS DETECTED:
${patterns.length > 0 ? patterns.map(p => `- ${p.patternType}: ${p.description}`).join('\n') : 'No clear patterns detected'}

SECURITY CONSIDERATIONS:
${security.length > 0 ? security.map(s => `- ${s.type} (${s.severity}): ${s.description} [${s.file}:${s.line}]`).join('\n') : 'No security issues detected'}

TASK:
1. Create the project structure matching the original
2. Implement all core functions with proper types and documentation
3. Set up the dependency injection/module pattern as detected
4. Include comprehensive error handling and logging
5. Add basic tests for critical paths
6. Ensure security best practices are followed
7. Document the architecture and key decisions

DELIVERABLES:
- Complete source code implementation
- Architecture decision record (ADR)
- Test suite with >80% coverage
- Security audit report
- Setup and deployment instructions

Start with the main entry point and work outward. Prioritize:
1. Core business logic
2. Data layer and persistence
3. API/controllers
4. Security and validation
5. Tests and documentation

Be thorough and production-ready in your implementation.`;
  }

  /**
   * Generate ANALYZE prompt (explain architecture)
   */
  generateAnalyzePrompt(repoMeta, fileTree, parsedFiles, analysis) {
    const keyFiles = this.formatKeyFiles(parsedFiles);
    const healthInfo = analysis.healthScore ? `\nHEALTH SCORE: ${analysis.healthScore}/100 (${analysis.healthGrade})` : '';
    const circularDeps = analysis.circularDependencies || [];
    const complexity = analysis.complexity || {};

    return `Analyze the following codebase and provide a comprehensive technical analysis:

REPOSITORY: ${repoMeta.full_name}
DESCRIPTION: ${repoMeta.description || 'N/A'}
LANGUAGE: ${repoMeta.language || 'Unknown'}
FILES ANALYZED: ${parsedFiles.length}
TOTAL FUNCTIONS: ${analysis.totalFunctions || 0}
TOTAL CLASSES: ${analysis.totalClasses || 0}
CIRCULAR DEPENDENCIES: ${circularDeps.length}${healthInfo}

${complexity.estimatedTime ? `\nESTIMATED ANALYSIS TIME: ${complexity.estimatedTime}` : ''}

KEY FILES:
${keyFiles}

ARCHITECTURE ANALYSIS:
${this.generateArchitectureAnalysis(parsedFiles, analysis)}

DEPENDENCY ANALYSIS:
${this.generateDependencyAnalysis(parsedFiles, analysis)}

SECURITY ANALYSIS:
${this.generateSecurityAnalysis(analysis)}

CODE QUALITY ASSESSMENT:
${this.generateQualityAssessment(parsedFiles, analysis)}

REFACTORING SUGGESTIONS:
${this.generateRefactoringSuggestions(parsedFiles, analysis)}

PERFORMANCE CONSIDERATIONS:
${this.generatePerformanceAnalysis(parsedFiles, analysis)}

Please provide:
1. Architecture pattern identification (MVC, Microservices, Layered, etc.)
2. Data flow diagram (text description)
3. Critical paths and potential bottlenecks
4. Detailed refactoring recommendations
5. Security audit findings with remediation steps
6. Testing strategy recommendations
7. Documentation gaps and improvements
8. Scalability assessment

Be detailed and actionable in your analysis.`;
  }

  /**
   * Generate ASK prompt (template for questions)
   */
  generateAskPrompt(repoMeta, fileTree, parsedFiles, analysis) {
    const context = this.summarizeForQA(parsedFiles, analysis);
    const keyComponents = this.extractKeyComponents(parsedFiles);

    return `I have analyzed the repository ${repoMeta.full_name}.

REPOSITORY CONTEXT:
${context}

KEY COMPONENTS:
${keyComponents}

You can ask me questions like:
- "Explain how the ${keyComponents.split('\n')[1] || 'authentication'} flow works"
- "What does the [functionName] function do?"
- "How do I add a new feature to [module]?"
- "Find potential bugs in [file]"
- "Refactor [function] to use async/await"
- "Explain the dependency between [moduleA] and [moduleB]"
- "What are the security implications of [code section]?"
- "How would I optimize [function] for performance?"

CONTEXT AVAILABLE:
- File structure and organization
- Function signatures and purposes
- Class hierarchies and relationships
- Import/export dependencies
- Design patterns detected
- Security considerations
- Code complexity metrics

USER QUESTION: [PASTE YOUR QUESTION HERE]

Please provide a detailed, technical answer based on the repository context above.`;
  }

  /**
   * Summarize key components
   */
  summarizeKeyComponents(parsedFiles) {
    if (!parsedFiles || parsedFiles.length === 0) {
      return 'No components identified';
    }

    const components = [];
    const functions = [];
    const classes = [];

    parsedFiles.forEach(file => {
      if (file.functions) {
        functions.push(...file.functions.map(f => `  - ${f.name}() in ${file.path}`));
      }
      if (file.classes) {
        classes.push(...file.classes.map(c => `  - ${c.name} in ${file.path}`));
      }
    });

    if (functions.length > 0) {
      components.push(`Functions (${functions.length}):\n${functions.slice(0, 20).join('\n')}`);
    }
    if (classes.length > 0) {
      components.push(`Classes (${classes.length}):\n${classes.slice(0, 15).join('\n')}`);
    }

    return components.join('\n\n') || 'No components identified';
  }

  /**
   * Summarize architecture
   */
  summarizeArchitecture(parsedFiles, analysis) {
    const patterns = analysis.patterns || [];
    const hasMVC = patterns.some(p => 
      p.patternType.includes('CONTROLLER') || 
      p.patternType.includes('MODEL') ||
      p.patternType.includes('VIEW')
    );
    const hasLayered = patterns.some(p => p.patternType.includes('LAYERED'));
    const hasDI = patterns.some(p => p.patternType.includes('DEPENDENCY_INJECTION'));

    let architecture = [];
    
    if (hasMVC) architecture.push('- MVC (Model-View-Controller) pattern detected');
    if (hasLayered) architecture.push('- Layered architecture (presentation, business, data)');
    if (hasDI) architecture.push('- Dependency injection pattern');
    
    if (architecture.length === 0) {
      architecture.push('- Modular monolith structure');
      architecture.push('- Component-based organization');
    }

    const fileCount = parsedFiles.length || 0;
    const estimatedModules = Math.ceil(fileCount / 10);
    
    architecture.push(`- Approximately ${estimatedModules} logical modules`);
    architecture.push(`- ${fileCount} files across the codebase`);

    return architecture.join('\n');
  }

  /**
   * Format dependencies
   */
  formatDependencies(parsedFiles) {
    if (!parsedFiles || parsedFiles.length === 0) {
      return 'No dependency information available';
    }

    const deps = [];
    parsedFiles.forEach(file => {
      if (file.callReferences && file.callReferences.length > 0) {
        const uniqueDeps = [...new Set(file.callReferences.map(r => r.toFunction))];
        if (uniqueDeps.length > 0) {
          deps.push(`  ${file.path}:`);
          deps.push(`    → Calls: ${uniqueDeps.slice(0, 5).join(', ')}${uniqueDeps.length > 5 ? '...' : ''}`);
        }
      }
    });

    return deps.length > 0 ? deps.join('\n') : 'No inter-file dependencies detected';
  }

  /**
   * Format key files
   */
  formatKeyFiles(parsedFiles) {
    if (!parsedFiles || parsedFiles.length === 0) {
      return 'No files to display';
    }

    const keyFiles = parsedFiles
      .filter(f => f.functions && f.functions.length > 0)
      .sort((a, b) => (b.functions?.length || 0) - (a.functions?.length || 0))
      .slice(0, 10);

    return keyFiles.map(f => 
      `  ${f.path}\n    Functions: ${f.functions?.length || 0}, Lines: ${f.linesOfCode || 0}, Complexity: ${f.complexity || 0}`
    ).join('\n') || 'No key files identified';
  }

  /**
   * Summarize for Q&A
   */
  summarizeForQA(parsedFiles, analysis) {
    const totalFunctions = parsedFiles.reduce((sum, f) => sum + (f.functions?.length || 0), 0);
    const totalClasses = parsedFiles.reduce((sum, f) => sum + (f.classes?.length || 0), 0);
    const languages = [...new Set(parsedFiles.map(f => f.language).filter(Boolean))];

    let summary = [];
    summary.push(`- Repository contains ${parsedFiles.length} files`);
    summary.push(`- Total functions: ${totalFunctions}`);
    summary.push(`- Total classes: ${totalClasses}`);
    summary.push(`- Languages: ${languages.join(', ') || 'Unknown'}`);
    
    if (analysis.healthScore) {
      summary.push(`- Health score: ${analysis.healthScore}/100 (${analysis.healthGrade})`);
    }
    
    if (analysis.patterns && analysis.patterns.length > 0) {
      summary.push(`- Design patterns: ${analysis.patterns.map(p => p.patternType).join(', ')}`);
    }
    
    if (analysis.securityIssues && analysis.securityIssues.length > 0) {
      const criticalCount = analysis.securityIssues.filter(s => s.severity === 'CRITICAL').length;
      summary.push(`- Security issues: ${analysis.securityIssues.length} (${criticalCount} critical)`);
    }

    return summary.join('\n');
  }

  /**
   * Extract key components
   */
  extractKeyComponents(parsedFiles) {
    const components = [];
    parsedFiles.forEach(file => {
      if (file.functions) {
        file.functions.slice(0, 3).forEach(f => {
          components.push(`  - ${f.name}() in ${file.path}`);
        });
      }
    });
    return components.slice(0, 10).join('\n') || 'No components identified';
  }

  /**
   * Generate architecture analysis
   */
  generateArchitectureAnalysis(parsedFiles, analysis) {
    const patterns = analysis.patterns || [];
    const layers = this.identifyLayers(parsedFiles);
    
    let analysisText = [];
    
    if (layers.length > 0) {
      analysisText.push(`Identified layers: ${layers.join(', ')}`);
    }
    
    if (patterns.length > 0) {
      analysisText.push(`Design patterns: ${patterns.map(p => p.patternType).join(', ')}`);
    }
    
    analysisText.push(`Modularity: ${this.assessModularity(parsedFiles)}`);
    analysisText.push(`Coupling: ${this.assessCoupling(parsedFiles)}`);
    analysisText.push(`Cohesion: ${this.assessCohesion(parsedFiles)}`);
    
    return analysisText.join('\n');
  }

  /**
   * Generate dependency analysis
   */
  generateDependencyAnalysis(parsedFiles, analysis) {
    const graph = analysis.dependencyGraph;
    let analysisText = [];
    
    if (graph && graph.blastRadius) {
      analysisText.push(`Maximum blast radius: ${graph.blastRadius} files`);
    }
    
    if (graph && graph.criticalPath && graph.criticalPath.length > 0) {
      analysisText.push(`Critical path length: ${graph.criticalPath.length} files`);
    }
    
    const circularDeps = analysis.circularDependencies || [];
    if (circularDeps.length > 0) {
      analysisText.push(`Circular dependencies found: ${circularDeps.length}`);
    }
    
    return analysisText.join('\n') || 'No dependency analysis available';
  }

  /**
   * Generate security analysis
   */
  generateSecurityAnalysis(analysis) {
    const issues = analysis.securityIssues || [];
    if (issues.length === 0) {
      return 'No security issues detected';
    }
    
    const bySeverity = {
      CRITICAL: issues.filter(i => i.severity === 'CRITICAL').length,
      HIGH: issues.filter(i => i.severity === 'HIGH').length,
      MEDIUM: issues.filter(i => i.severity === 'MEDIUM').length,
      LOW: issues.filter(i => i.severity === 'LOW').length,
    };
    
    return `Critical: ${bySeverity.CRITICAL}, High: ${bySeverity.HIGH}, Medium: ${bySeverity.MEDIUM}, Low: ${bySeverity.LOW}`;
  }

  /**
   * Generate quality assessment
   */
  generateQualityAssessment(parsedFiles, analysis) {
    const avgComplexity = parsedFiles.length > 0
      ? Math.round(parsedFiles.reduce((sum, f) => sum + (f.complexity || 0), 0) / parsedFiles.length)
      : 0;
    
    const deadCode = analysis.deadCode || [];
    const patterns = analysis.patterns || [];
    
    return `Average complexity: ${avgComplexity}\n` +
           `Dead code instances: ${deadCode.length}\n` +
           `Design patterns: ${patterns.length}\n` +
           `Code quality: ${this.assessOverallQuality(analysis)}`;
  }

  /**
   * Generate refactoring suggestions
   */
  generateRefactoringSuggestions(parsedFiles, analysis) {
    const suggestions = [];
    
    const circularDeps = analysis.circularDependencies || [];
    if (circularDeps.length > 0) {
      suggestions.push('Break circular dependencies using interfaces or dependency injection');
    }
    
    const deadCode = analysis.deadCode || [];
    if (deadCode.length > 5) {
      suggestions.push('Remove unused code to reduce maintenance burden');
    }
    
    const highComplexity = parsedFiles.filter(f => (f.complexity || 0) > 20);
    if (highComplexity.length > 0) {
      suggestions.push('Refactor high-complexity functions into smaller units');
    }
    
    const securityIssues = (analysis.securityIssues || []).filter(s => 
      s.severity === 'CRITICAL' || s.severity === 'HIGH'
    );
    if (securityIssues.length > 0) {
      suggestions.push('Address security vulnerabilities immediately');
    }
    
    return suggestions.length > 0 
      ? suggestions.map((s, i) => `${i + 1}. ${s}`).join('\n')
      : 'No critical refactoring needed';
  }

  /**
   * Generate performance analysis
   */
  generatePerformanceAnalysis(parsedFiles, analysis) {
    const suggestions = [];
    
    const graph = analysis.dependencyGraph;
    if (graph && graph.blastRadius > 20) {
      suggestions.push('High blast radius indicates tight coupling - consider modularization');
    }
    
    const highComplexity = parsedFiles.filter(f => (f.complexity || 0) > 30);
    if (highComplexity.length > 0) {
      suggestions.push('High complexity functions may impact performance - profile and optimize');
    }
    
    return suggestions.length > 0 
      ? suggestions.join('\n')
      : 'No major performance concerns identified';
  }

  /**
   * Identify architectural layers
   */
  identifyLayers(parsedFiles) {
    const layers = new Set();
    
    parsedFiles.forEach(file => {
      const path = file.path.toLowerCase();
      if (path.includes('controller') || path.includes('api')) layers.add('Presentation');
      if (path.includes('service') || path.includes('business')) layers.add('Business Logic');
      if (path.includes('model') || path.includes('entity')) layers.add('Domain');
      if (path.includes('repository') || path.includes('dao')) layers.add('Data Access');
      if (path.includes('config')) layers.add('Configuration');
    });
    
    return Array.from(layers);
  }

  /**
   * Assess modularity
   */
  assessModularity(parsedFiles) {
    const fileCount = parsedFiles.length;
    if (fileCount === 0) return 'Unknown';
    
    const avgFileSize = parsedFiles.reduce((sum, f) => sum + (f.linesOfCode || 0), 0) / fileCount;
    
    if (avgFileSize < 200) return 'High';
    if (avgFileSize < 500) return 'Medium';
    return 'Low';
  }

  /**
   * Assess coupling
   */
  assessCoupling(parsedFiles) {
    const totalDeps = parsedFiles.reduce((sum, f) => 
      sum + (f.callReferences?.length || 0), 0);
    const avgDeps = totalDeps / Math.max(parsedFiles.length, 1);
    
    if (avgDeps < 3) return 'Low';
    if (avgDeps < 8) return 'Medium';
    return 'High';
  }

  /**
   * Assess cohesion
   */
  assessCohesion(parsedFiles) {
    const filesWithSinglePurpose = parsedFiles.filter(f => {
      const funcCount = f.functions?.length || 0;
      return funcCount > 0 && funcCount <= 5;
    }).length;
    
    const ratio = funcCount / Math.max(parsedFiles.length, 1);
    
    if (ratio > 0.7) return 'High';
    if (ratio > 0.4) return 'Medium';
    return 'Low';
  }

  /**
   * Assess overall quality
   */
  assessOverallQuality(analysis) {
    const healthScore = analysis.healthScore || 0;
    
    if (healthScore >= 80) return 'Excellent';
    if (healthScore >= 60) return 'Good';
    if (healthScore >= 40) return 'Fair';
    return 'Needs Improvement';
  }
}

export default new PromptGenerator();