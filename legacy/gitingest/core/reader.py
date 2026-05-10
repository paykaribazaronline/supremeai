"""
File reader with multi-encoding fallback and Jupyter notebook support
"""
from pathlib import Path
from typing import Optional, Tuple
import chardet


def read_file(filepath: Path, max_size: int = None) -> Tuple[str, str]:
    """
    Read a file with encoding fallback.
    
    Args:
        filepath: Path to file
        max_size: Maximum file size to read (bytes), None for no limit
        
    Returns:
        Tuple of (content, encoding_used)
        
    Raises:
        UnicodeDecodeError: If file cannot be decoded
        IOError: If file cannot be read
    """
    # Check file size
    if max_size and filepath.stat().st_size > max_size:
        raise IOError(f"File too large: {filepath.stat().st_size} bytes")
    
    # Try UTF-8 first (most common)
    try:
        content = filepath.read_text(encoding='utf-8')
        return content, 'utf-8'
    except UnicodeDecodeError:
        pass
    
    # Try UTF-16 (with BOM detection)
    for enc in ['utf-16-le', 'utf-16-be', 'utf-16']:
        try:
            content = filepath.read_text(encoding=enc)
            return content, enc
        except (UnicodeDecodeError, UnicodeError):
            pass
    
    # Try Latin-1 (should always work)
    try:
        content = filepath.read_text(encoding='latin-1')
        return content, 'latin-1'
    except Exception:
        pass
    
    # Last resort: detect encoding with chardet
    try:
        raw = filepath.read_bytes()
        detected = chardet.detect(raw)
        if detected and detected.get('encoding'):
            encoding = detected['encoding']
            content = raw.decode(encoding)
            return content, encoding
    except Exception:
        pass
    
    raise UnicodeDecodeError(f"Could not decode file: {filepath}")


def read_file_safe(filepath: Path, max_size: int = None) -> Tuple[str, str]:
    """
    Read file with fallback, returning empty string on error.
    
    Args:
        filepath: Path to file
        max_size: Maximum file size
        
    Returns:
        Tuple of (content, encoding_used or error message)
    """
    try:
        return read_file(filepath, max_size)
    except UnicodeDecodeError:
        return "", "decode-error"
    except Exception as e:
        return "", f"error: {str(e)}"


def read_binary_file(filepath: Path) -> bytes:
    """
    Read file as binary data.
    
    Args:
        filepath: Path to file
        
    Returns:
        File contents as bytes
    """
    return filepath.read_bytes()


def process_jupyter_notebook(filepath: Path) -> str:
    """
    Extract code cells from Jupyter notebook.
    
    Args:
        filepath: Path to .ipynb file
        
    Returns:
        Concatenated code cells as string
    """
    import json
    
    try:
        content, _ = read_file(filepath)
        notebook = json.loads(content)
        
        code_cells = []
        for cell in notebook.get('cells', []):
            if cell.get('cell_type') == 'code':
                source = cell.get('source', [])
                if isinstance(source, list):
                    code = ''.join(source)
                else:
                    code = str(source)
                
                if code.strip():
                    code_cells.append(code)
        
        return '\n\n# --- Cell ---\n\n'.join(code_cells)
    
    except Exception as e:
        return f"# Error processing Jupyter notebook: {str(e)}"


def is_binary_file(filepath: Path) -> bool:
    """
    Check if a file appears to be binary.
    
    Args:
        filepath: Path to file
        
    Returns:
        True if file appears binary
    """
    try:
        with open(filepath, 'rb') as f:
            chunk = f.read(8192)
            if b'\x00' in chunk:
                return True
            # Check for high ratio of non-text characters
            if chunk:
                non_text = sum(1 for b in chunk if b < 0x09 or (0x0D < b < 0x20) and b != 0x1B)
                if len(chunk) > 0 and non_text / len(chunk) > 0.30:
                    return True
    except Exception:
        return True
    return False


def get_file_info(filepath: Path) -> dict:
    """
    Get file information.
    
    Args:
        filepath: Path to file
        
    Returns:
        Dictionary with file info
    """
    import os
    
    stat = filepath.stat()
    return {
        'path': str(filepath),
        'name': filepath.name,
        'size': stat.st_size,
        'extension': filepath.suffix,
        'is_symlink': filepath.is_symlink(),
        'is_binary': is_binary_file(filepath) if not filepath.is_symlink() else False,
    }
