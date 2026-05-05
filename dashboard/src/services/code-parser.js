/**
 * Code Parser Service
 * Uses Tree-sitter WASM for parsing code in browser
 * Falls back to Acorn (JS/TS) and regex heuristics
 */

// Tree-sitter will be loaded dynamically
let treeSitter = null;
let loadedLanguages = {};

// Language mappings
const LANGUAGE_GRAMMARS = {
  javascript: 'tree-sitter-javascript',
  typescript: 'tree-sitter-typescript',
  python: 'tree-sitter-python',
  go: 'tree-sitter-go',
  rust: 'tree-sitter-rust',
  java: 'tree-sitter-java',
  ruby: 'tree-sitter-ruby',
  c: 'tree-sitter-c',
  cpp: 'tree-sitter-cpp',
};

/**
 * Initialize Tree-sitter
 */
export async function initTreeSitter() {
  if (treeSitter) return true;

  try {
    // In production, this would load from jsdelivr CDN
    // For now, we'll simulate the initialization
    console.log('Initializing Tree-sitter WASM...');
    
    // Simulate async loading
    await new Promise(resolve => setTimeout(resolve, 100));
    
    treeSitter = {
      init: async () => {},
      Parser: class Parser {
        constructor() {
          this._parser = null;
        }
        
        setLanguage(lang) {
          this.language = lang;
        }
        
        parse(content) {
          // Simulate parsing - in production would use actual Tree-sitter
          return this._simulateParse(content);
        }
        
        _simulateParse(content) {
          return {
            rootNode: {
              type: 'program',
              children: this._extractNodes(content),
              walk: function* () {
                yield* this.children || [];
              },
            },
          };
        }
        
        _extractNodes(content) {
          const nodes = [];
          const lines = content.split('\n');
          
          lines.forEach((line, idx) => {
            // Detect functions
            const funcMatch = line.match(/function\s+(\w+)|(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s+)?(?:\([^)]*\)|\w+)\s*=>/);
            if (funcMatch) {
              nodes.push({
                type: 'function_declaration',
                text: line.trim(),
                startPosition: { row: idx, column: 0 },
                endPosition: { row: idx, column: line.length },
                namedChildren: [],
              });
            }
            
            // Detect classes
            const classMatch = line.match(/class\s+(\w+)/);
            if (classMatch) {
              nodes.push({
                type: 'class_declaration',
                text: line.trim(),
                startPosition: { row: idx, column: 0 },
                endPosition: { row: idx, column: line.length },
                namedChildren: [],
              });
            }
          });
          
          return nodes;
        }
      },
    };
    
    await treeSitter.init();
    console.log('Tree-sitter initialized');
    return true;
  } catch (error) {
    console.warn('Tree-sitter initialization failed:', error);
    return false;
  }
}

/**
 * Load language grammar
 */
export async function loadLanguage(language) {
  if (loadedLanguages[language]) {
    return loadedLanguages[language];
  }

  if (!treeSitter) {
    await initTreeSitter();
  }

  try {
    // In production, would load from CDN:
    // const langModule = await import(`https://cdn.jsdelivr.net/npm/${LANGUAGE_GRAMMARS[language]}`);
    // loadedLanguages[language] = langModule;
    
    // Simulate loading
    loadedLanguages[language] = { name: language };
    return loadedLanguages[language];
  } catch (error) {
    console.warn(`Failed to load language: ${language}`, error);
    return null;
  }
}

/**
 * Parse code with Tree-sitter
 */
export async function parseWithTreeSitter(content, language) {
  if (!treeSitter) {
    await initTreeSitter();
  }

  const lang = await loadLanguage(language);
  if (!lang) {
    throw new Error(`Language not supported: ${language}`);
  }

  const parser = new treeSitter.Parser();
  parser.setLanguage(lang);
  
  const tree = parser.parse(content);
  return extractCodeStructure(tree, content, language);
}

/**
 * Extract code structure from Tree-sitter tree
 */
function extractCodeStructure(tree, content, language) {
  const functions = [];
  const classes = [];
  const imports = [];
  const lines = content.split('\n');

  function walk(node, depth = 0) {
    if (depth > 10) return; // Prevent infinite recursion
    
    // Extract based on node type
    switch (node.type) {
      case 'function_declaration':
      case 'method_definition':
      case 'arrow_function':
        const funcInfo = extractFunctionInfo(node, lines, language);
        if (funcInfo) functions.push(funcInfo);
        break;
        
      case 'class_declaration':
      case 'class_definition':
        const classInfo = extractClassInfo(node, lines, language);
        if (classInfo) classes.push(classInfo);
        break;
        
      case 'import_statement':
      case 'import_declaration':
      case 'require_call':
        const importInfo = extractImportInfo(node, lines, language);
        if (importInfo) imports.push(importInfo);
        break;
    }
    
    // Walk children
    if (node.children) {
      node.children.forEach(child => walk(child, depth + 1));
    } else if (node.namedChildren) {
      node.namedChildren.forEach(child => walk(child, depth + 1));
    }
  }

  walk(tree.rootNode);
  
  return { functions, classes, imports };
}

/**
 * Extract function information
 */
function extractFunctionInfo(node, lines, language) {
  const startLine = node.startPosition.row;
  const endLine = node.endPosition.row;
  const text = lines.slice(startLine, endLine + 1).join('\n');
  
  // Extract name
  let name = 'anonymous';
  const nameMatch = text.match(/function\s+(\w+)|(?:const|let|var)\s+(\w+)\s*=|(\w+)\s*\(/);
  if (nameMatch) {
    name = nameMatch[1] || nameMatch[2] || nameMatch[3] || 'anonymous';
  }
  
  // Extract parameters
  const params = [];
  const paramMatch = text.match(/\(([^)]*)\)/);
  if (paramMatch && paramMatch[1]) {
    paramMatch[1].split(',').forEach(p => {
      const param = p.trim().split('=')[0].trim();
      if (param) params.push(param);
    });
  }
  
  // Extract return type (TypeScript)
  let returnType = 'any';
  if (language === 'typescript') {
    const returnMatch = text.match(/:\s*([\w<>\[\]]+)(?=\s*{)/);
    if (returnMatch) returnType = returnMatch[1];
  }
  
  return {
    name,
    line: startLine + 1,
    endLine: endLine + 1,
    parameters: params,
    returnType,
    complexity: calculateComplexity(text),
    cyclomaticComplexity: calculateCyclomaticComplexity(text),
    cognitiveComplexity: calculateCognitiveComplexity(text),
    calledFunctions: extractCalledFunctions(text),
    isPublic: !text.includes('private'),
    isStatic: text.includes('static'),
    isAsync: text.includes('async'),
  };
}

/**
 * Extract class information
 */
function extractClassInfo(node, lines, language) {
  const startLine = node.startPosition.row;
  const endLine = node.endPosition.row;
  const text = lines.slice(startLine, endLine + 1).join('\n');
  
  // Extract name
  let name = 'AnonymousClass';
  const nameMatch = text.match(/class\s+(\w+)/);
  if (nameMatch) name = nameMatch[1];
  
  // Extract extends
  const extendsClasses = [];
  const extendsMatch = text.match(/extends\s+(\w+)/);
  if (extendsMatch) extendsClasses.push(extendsMatch[1]);
  
  // Extract implements
  const implementsInterfaces = [];
  const implementsMatch = text.match(/implements\s+([^{]+)/);
  if (implementsMatch) {
    implementsInterfaces.push(...implementsMatch[1].split(',').map(i => i.trim()));
  }
  
  return {
    name,
    line: startLine + 1,
    type: text.includes('interface') ? 'INTERFACE' : 
          text.includes('abstract') ? 'ABSTRACT_CLASS' : 'CLASS',
    extendsClasses,
    implementsInterfaces,
    methods: [], // Would be populated by walking children
    fields: extractFields(text),
    complexity: calculateComplexity(text),
    isAbstract: text.includes('abstract') || text.includes('interface'),
    isFinal: text.includes('final') || text.includes('sealed'),
  };
}

/**
 * Extract import information
 */
function extractImportInfo(node, lines, language) {
  const line = lines[node.startPosition.row];
  
  let module = '';
  let alias = '';
  
  if (language === 'javascript' || language === 'typescript') {
    const importMatch = line.match(/from\s+['"]([^'"]+)['"]/);
    if (importMatch) module = importMatch[1];
    
    const aliasMatch = line.match(/import\s+(\w+)\s+from/);
    if (aliasMatch) alias = aliasMatch[1];
  } else if (language === 'python') {
    const importMatch = line.match(/import\s+(\w+)/);
    if (importMatch) module = importMatch[1];
  }
  
  return {
    module,
    alias,
    isUsed: true, // Would be checked during analysis
    line: node.startPosition.row + 1,
  };
}

/**
 * Extract fields from class text
 */
function extractFields(text) {
  const fields = [];
  const fieldRegex = /(?:private|public|protected)?\s*(?:static)?\s*(\w+)\s+(\w+)\s*[=;]/g;
  let match;
  
  while ((match = fieldRegex.exec(text)) !== null) {
    fields.push(match[2]);
  }
  
  return fields;
}

/**
 * Extract called functions from text
 */
function extractCalledFunctions(text) {
  const functions = [];
  const funcRegex = /(\w+)\s*\(/g;
  let match;
  
  while ((match = funcRegex.exec(text)) !== null) {
    // Skip keywords and common non-function calls
    const word = match[1];
    if (!['if', 'for', 'while', 'switch', 'catch', 'function', 'return', 'new', 'typeof'].includes(word)) {
      functions.push(word);
    }
  }
  
  return [...new Set(functions)]; // Remove duplicates
}

/**
 * Calculate cyclomatic complexity
 */
function calculateCyclomaticComplexity(text) {
  const keywords = ['if', 'else if', 'for', 'while', 'do', 'case', 'catch', '&&', '||', '?'];
  let complexity = 1;
  
  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b${keyword}\\b`, 'g');
    const matches = text.match(regex);
    if (matches) complexity += matches.length;
  });
  
  return complexity;
}

/**
 * Calculate cognitive complexity
 */
function calculateCognitiveComplexity(text) {
  let complexity = 0;
  const lines = text.split('\n');
  
  lines.forEach(line => {
    const trimmed = line.trim();
    
    // Nested structures increase complexity more
    if (trimmed.includes('if') || trimmed.includes('for') || trimmed.includes('while')) {
      const indent = line.search(/\S/);
      complexity += 1 + Math.floor(indent / 2);
    }
    
    // Boolean operators
    complexity += (line.match(/&&/g) || []).length;
    complexity += (line.match(/\|\|/g) || []).length;
  });
  
  return complexity;
}

/**
 * Calculate general complexity score
 */
function calculateComplexity(text) {
  const cyclomatic = calculateCyclomaticComplexity(text);
  const cognitive = calculateCognitiveComplexity(text);
  const lines = text.split('\n').length;
  
  // Weighted formula
  return Math.min(100, Math.round(
    (cyclomatic * 2) + 
    (cognitive * 1.5) + 
    (lines * 0.1)
  ));
}

/**
 * Parse with Acorn (for JavaScript/TypeScript)
 */
export async function parseWithAcorn(content) {
  // In production, would use actual Acorn parser
  // For now, simulate with regex-based parsing
  console.log('Using Acorn parser (simulated)');
  
  return parseWithRegex(content, 'javascript');
}

/**
 * Parse with regex heuristics (fallback)
 */
export function parseWithRegex(content, language) {
  const functions = [];
  const classes = [];
  const imports = [];
  const lines = content.split('\n');

  // Get patterns for language
  const funcPattern = getFunctionPattern(language);
  const classPattern = getClassPattern(language);
  const importPattern = getImportPattern(language);

  // Extract imports
  lines.forEach((line, idx) => {
    if (importPattern.test(line)) {
      imports.push({
        module: extractModuleName(line, language),
        alias: extractAlias(line, language),
        isUsed: true,
        line: idx + 1,
      });
    }
  });

  // Extract functions
  lines.forEach((line, idx) => {
    const funcMatch = line.match(funcPattern);
    if (funcMatch) {
      const name = extractFunctionName(line, language);
      if (name) {
        functions.push({
          name,
          line: idx + 1,
          endLine: idx + 10, // Estimate
          parameters: extractParameters(line, language),
          returnType: extractReturnType(line, language),
          complexity: 1,
          cyclomaticComplexity: 1,
          cognitiveComplexity: 1,
          calledFunctions: [],
          isPublic: !line.includes('private'),
          isStatic: line.includes('static'),
          isAsync: line.includes('async'),
        });
      }
    }
  });

  // Extract classes
  lines.forEach((line, idx) => {
    const classMatch = line.match(classPattern);
    if (classMatch) {
      const name = extractClassName(line, language);
      if (name) {
        classes.push({
          name,
          line: idx + 1,
          type: 'CLASS',
          extendsClasses: extractExtends(line, language),
          implementsInterfaces: extractImplements(line, language),
          methods: [],
          fields: [],
          complexity: 1,
          isAbstract: line.includes('abstract') || line.includes('interface'),
          isFinal: line.includes('final'),
        });
      }
    }
  });

  return { functions, classes, imports };
}

/**
 * Get import pattern for language
 */
function getImportPattern(language) {
  switch (language) {
    case 'python':
      return /^\s*(import\s+[\w.]+|from\s+[\w.]+\s+import)/;
    case 'javascript':
    case 'typescript':
      return /import\s+.*?from\s+['"][^'"]+['"]|require\s*\(/;
    case 'java':
      return /^\s*import\s+[\w.*]+;/;
    case 'go':
      return /^\s*import\s+\(/;
    default:
      return /./;
  }
}

/**
 * Get function pattern for language
 */
function getFunctionPattern(language) {
  switch (language) {
    case 'python':
      return /^\s*def\s+(\w+)\s*\(/;
    case 'javascript':
    case 'typescript':
      return /(?:function\s+(\w+)|const\s+(\w+)\s*=\s*(?:async\s+)?\([^)]*\)\s*=>|async\s+function\s+(\w+))/;
    case 'java':
      return /(public|private|protected)?\s*(static)?\s*\w+\s+(\w+)\s*\(/;
    case 'go':
      return /^\s*func\s+(\w+)\s*\(/;
    default:
      return /./;
  }
}

/**
 * Get class pattern for language
 */
function getClassPattern(language) {
  switch (language) {
    case 'python':
      return /^\s*class\s+(\w+)/;
    case 'javascript':
    case 'typescript':
      return /class\s+(\w+)/;
    case 'java':
      return /(public|private|protected)?\s*(abstract|final)?\s*class\s+(\w+)/;
    case 'go':
      return /type\s+(\w+)\s+struct/;
    default:
      return /./;
  }
}

/**
 * Extract module name from import line
 */
function extractModuleName(line, language) {
  switch (language) {
    case 'python':
      const pyMatch = line.match(/import\s+([\w.]+)/);
      return pyMatch ? pyMatch[1] : '';
    case 'javascript':
    case 'typescript':
      const jsMatch = line.match(/from\s+['"]([^'"]+)['"]/);
      return jsMatch ? jsMatch[1] : '';
    default:
      return '';
  }
}

/**
 * Extract alias from import line
 */
function extractAlias(line, language) {
  const aliasMatch = line.match(/import\s+(\w+)\s+from/);
  return aliasMatch ? aliasMatch[1] : null;
}

/**
 * Extract function name
 */
function extractFunctionName(line, language) {
  const patterns = [
    /function\s+(\w+)/,
    /const\s+(\w+)\s*=/,
    /(\w+)\s*\(/,
  ];
  
  for (const pattern of patterns) {
    const match = line.match(pattern);
    if (match) return match[1];
  }
  
  return null;
}

/**
 * Extract class name
 */
function extractClassName(line, language) {
  const match = line.match(/class\s+(\w+)/);
  return match ? match[1] : null;
}

/**
 * Extract parameters
 */
function extractParameters(line, language) {
  const match = line.match(/\(([^)]*)\)/);
  if (!match || !match[1]) return [];
  
  return match[1].split(',').map(p => p.trim().split('=')[0].trim()).filter(Boolean);
}

/**
 * Extract return type
 */
function extractReturnType(line, language) {
  if (language !== 'typescript') return 'void';
  
  const match = line.match(/:\s*([\w<>\[\]]+)(?=\s*{)/);
  return match ? match[1] : 'any';
}

/**
 * Extract extends
 */
function extractExtends(line, language) {
  const match = line.match(/extends\s+(\w+)/);
  return match ? [match[1]] : [];
}

/**
 * Extract implements
 */
function extractImplements(line, language) {
  const match = line.match(/implements\s+([^{]+)/);
  if (!match) return [];
  
  return match[1].split(',').map(i => i.trim());
}