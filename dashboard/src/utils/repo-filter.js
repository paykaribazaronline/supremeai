/**
 * Repo Filter - "Copy Required Part" Logic
 * Auto-filters based on repo language and structure
 * User can override with custom path patterns
 */

// Smart filters for different languages
const SMART_FILTERS = {
  javascript: {
    include: ['src/', 'lib/', 'components/', 'hooks/', 'utils/', 'services/', 'api/'],
    exclude: ['node_modules/', 'dist/', 'build/', 'coverage/', '*.test.*', '*.spec.*', '*.snap'],
    maxFiles: 50,
    maxSizePerFile: 100000, // 100KB
  },
  typescript: {
    include: ['src/', 'lib/', 'components/', 'hooks/', 'utils/', 'services/', 'api/'],
    exclude: ['node_modules/', 'dist/', 'build/', 'coverage/', '*.test.*', '*.spec.*'],
    maxFiles: 50,
    maxSizePerFile: 100000,
  },
  python: {
    include: ['src/', 'app/', 'core/', 'models/', 'views/', 'utils/', 'services/'],
    exclude: ['venv/', '__pycache__/', '*.pyc', 'tests/', 'test_', '.env'],
    maxFiles: 50,
    maxSizePerFile: 50000,
  },
  go: {
    include: ['cmd/', 'internal/', 'pkg/', 'api/', 'handlers/'],
    exclude: ['vendor/', 'test/', '*_test.go'],
    maxFiles: 50,
    maxSizePerFile: 50000,
  },
  rust: {
    include: ['src/', 'lib/', 'bin/', 'examples/'],
    exclude: ['target/', '*.rs.bk'],
    maxFiles: 50,
    maxSizePerFile: 50000,
  },
  java: {
    include: ['src/main/', 'src/java/', 'app/'],
    exclude: ['src/test/', 'target/', '*.class'],
    maxFiles: 50,
    maxSizePerFile: 100000,
  },
  ruby: {
    include: ['app/', 'lib/', 'config/'],
    exclude: ['vendor/', 'test/', 'spec/'],
    maxFiles: 50,
    maxSizePerFile: 50000,
  },
  cpp: {
    include: ['src/', 'include/', 'lib/'],
    exclude: ['build/', 'cmake-build-*/', '*.o', '*.a'],
    maxFiles: 50,
    maxSizePerFile: 100000,
  },
};

/**
 * Filter files based on smart filters and user preferences
 * @param {Array} files - Array of file objects from GitHub API
 * @param {Object} filterConfig - Language-specific filter config
 * @param {string} customFocus - User-specified path to focus on
 * @returns {Array} Filtered files
 */
export function filterFiles(files, filterConfig, customFocus = null) {
  if (!files || !Array.isArray(files)) {
    return [];
  }

  // If user specified a custom focus path, filter to only those files
  if (customFocus) {
    return files.filter(f => 
      f.path.includes(customFocus) && 
      !isExcluded(f.path, filterConfig.exclude)
    ).slice(0, filterConfig.maxFiles);
  }

  // Apply smart filtering
  const filtered = files.filter(file => {
    // Check if file matches include patterns
    const isIncluded = filterConfig.include.some(pattern => 
      file.path.startsWith(pattern) || file.path.includes('/' + pattern)
    );

    // Check if file is excluded
    const isExcludedFile = isExcluded(file.path, filterConfig.exclude);

    // Check file size
    const isWithinSizeLimit = file.size <= filterConfig.maxSizePerFile;

    return isIncluded && !isExcludedFile && isWithinSizeLimit;
  });

  // Limit total number of files
  return filtered.slice(0, filterConfig.maxFiles);
}

/**
 * Check if a file path matches any exclusion pattern
 * @private
 */
function isExcluded(path, excludePatterns) {
  return excludePatterns.some(pattern => {
    if (pattern.endsWith('/')) {
      return path.includes(pattern) || path.startsWith(pattern);
    }
    if (pattern.startsWith('*')) {
      return path.endsWith(pattern.substring(1));
    }
    return path.includes(pattern);
  });
}

/**
 * Detect the primary language of a repository based on file tree
 * @param {Array} files - Array of file objects
 * @returns {string} Detected language
 */
export function detectLanguage(files) {
  const langScores = {};

  files.forEach(file => {
    const ext = file.name.split('.').pop().toLowerCase();
    const lang = getLanguageFromExtension(ext);
    
    if (lang) {
      langScores[lang] = (langScores[lang] || 0) + 1;
    }
  });

  // Return language with highest score
  return Object.entries(langScores)
    .sort((a, b) => b[1] - a[1])[0]?.[0] || 'unknown';
}

/**
 * Get language from file extension
 * @private
 */
function getLanguageFromExtension(ext) {
  const langMap = {
    js: 'javascript',
    jsx: 'javascript',
    ts: 'typescript',
    tsx: 'typescript',
    py: 'python',
    go: 'go',
    rs: 'rust',
    java: 'java',
    rb: 'ruby',
    c: 'cpp',
    cpp: 'cpp',
    cc: 'cpp',
    h: 'cpp',
  };

  return langMap[ext];
}

/**
 * Get smart filter for a specific language
 * @param {string} language - Language identifier
 * @returns {Object} Filter configuration
 */
export function getSmartFilter(language) {
  return SMART_FILTERS[language] || SMART_FILTERS.javascript;
}

/**
 * Prioritize important files
 * Sorts files by importance (entry points, configs, main files first)
 * @param {Array} files - Array of file objects
 * @returns {Array} Sorted files
 */
export function prioritizeFiles(files) {
  const priorityPatterns = [
    'index.',
    'main.',
    'app.',
    'server.',
    'config.',
    'package.json',
    'pyproject.toml',
    'go.mod',
    'Cargo.toml',
    'pom.xml',
    'build.gradle',
  ];

  return files.sort((a, b) => {
    const aPriority = getPriorityScore(a.path, priorityPatterns);
    const bPriority = getPriorityScore(b.path, priorityPatterns);
    return bPriority - aPriority;
  });
}

/**
 * Calculate priority score for a file path
 * @private
 */
function getPriorityScore(path, patterns) {
  let score = 0;
  
  patterns.forEach(pattern => {
    if (path.includes(pattern)) {
      score += 10;
    }
  });

  // Higher priority for root-level files
  if (!path.includes('/')) {
    score += 5;
  }

  // Lower priority for test files
  if (path.includes('test') || path.includes('spec')) {
    score -= 5;
  }

  return score;
}

/**
 * Group files by directory
 * @param {Array} files - Array of file objects
 * @returns {Object} Files grouped by directory
 */
export function groupFilesByDirectory(files) {
  return files.reduce((groups, file) => {
    const dir = file.path.substring(0, file.path.lastIndexOf('/')) || 'root';
    if (!groups[dir]) {
      groups[dir] = [];
    }
    groups[dir].push(file);
    return groups;
  }, {});
}

/**
 * Estimate analysis complexity
 * @param {Array} files - Array of file objects
 * @returns {Object} Complexity estimate
 */
export function estimateComplexity(files) {
  const totalSize = files.reduce((sum, f) => sum + (f.size || 0), 0);
  const totalFiles = files.length;
  
  let complexity = 'low';
  if (totalFiles > 30 || totalSize > 5000000) {
    complexity = 'high';
  } else if (totalFiles > 15 || totalSize > 1000000) {
    complexity = 'medium';
  }

  return {
    totalFiles,
    totalSize,
    complexity,
    estimatedTime: complexity === 'high' ? '30-60s' : 
                    complexity === 'medium' ? '15-30s' : '5-15s',
  };
}