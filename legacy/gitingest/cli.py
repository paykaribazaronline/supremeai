"""
Click CLI for Gitingest
"""
import sys
from pathlib import Path
from typing import Optional

import click

from ..config import get_settings
from ..models import IngestRequest, IngestResponse
from ..core import ingest, ingest_local


@click.command()
@click.argument("source", required=False, default=".")
@click.option("-o", "--output", 
              help="Output file path (- for stdout)")
@click.option("-i", "--include", multiple=True, 
              help="Include patterns (can be specified multiple times)")
@click.option("-e", "--exclude", multiple=True,
              help="Exclude patterns (can be specified multiple times)")
@click.option("-t", "--token", 
              help="GitHub Personal Access Token")
@click.option("--max-file-size", type=int, default=None,
              help="Maximum file size in KB")
@click.option("--include-submodules", is_flag=True,
              help="Include git submodules")
@click.option("--include-gitignored", is_flag=True,
              help="Ignore .gitignore patterns")
@click.option("--pattern-type", type=click.Choice(["include", "exclude"]),
              default="exclude",
              help="Pattern matching type")
@click.version_option(version="1.0.0", prog_name="gitingest")
def main(
    source: str,
    output: Optional[str],
    include: tuple,
    exclude: tuple,
    token: Optional[str],
    max_file_size: Optional[int],
    include_submodules: bool,
    include_gitignored: bool,
    pattern_type: str,
):
    """
    Gitingest: Convert Git repositories into LLM-friendly text digests.
    
    SOURCE can be a GitHub URL, git URL, or local directory path.
    Defaults to current directory.
    
    Examples:
    
        gitingest .
        
        gitingest https://github.com/fastapi/fastapi
        
        gitingest https://github.com/user/repo -o digest.txt
        
        gitingest https://github.com/user/repo -e "*.md" -e "docs/*"
        
        gitingest https://github.com/user/repo -o -  # stdout
    """
    try:
        # Build patterns string
        patterns_list = list(include if pattern_type == "include" else exclude)
        patterns = ",".join(patterns_list) if patterns_list else None
        
        # Create request
        request = IngestRequest(
            url=source,
            pattern_type=pattern_type,
            patterns=patterns,
            max_file_size_kb=max_file_size,
            github_pat=token,
        )
        
        # Run ingestion
        click.echo("Processing repository...", err=True)
        
        # Check if local path
        source_path = Path(source)
        if source_path.exists() and source_path.is_dir():
            result = ingest_local(
                path=source,
                include_patterns=list(include) if pattern_type == "include" else None,
                exclude_patterns=list(exclude) if pattern_type == "exclude" else None,
                max_file_size_kb=max_file_size,
            )
        else:
            result = ingest(request)
        
        # Output result
        output_content = _format_output(result)
        
        if output == "-":
            # Stdout
            click.echo(output_content)
        else:
            output_path = Path(output or "digest.txt")
            output_path.write_text(output_content, encoding='utf-8')
            click.echo(f"Digest written to {output_path}", err=True)
            click.echo(f"Summary: {result.summary[:100]}...", err=True)
            click.echo(f"Files: {result.file_count}", err=True)
            click.echo(f"Estimated tokens: {result.estimated_tokens}", err=True)
            
    except Exception as e:
        click.echo(f"Error: {e}", err=True)
        sys.exit(1)


def _format_output(result: IngestResponse) -> str:
    """Format the complete output"""
    parts = []
    
    # Add summary section
    parts.append("=" * 80)
    parts.append("SUMMARY")
    parts.append("=" * 80)
    parts.append(result.summary)
    parts.append("")
    
    # Add tree section
    parts.append("=" * 80)
    parts.append("DIRECTORY TREE")
    parts.append("=" * 80)
    parts.append(result.tree)
    parts.append("")
    
    # Add content
    parts.append(result.content)
    
    return "\n".join(parts)


if __name__ == "__main__":
    main()
