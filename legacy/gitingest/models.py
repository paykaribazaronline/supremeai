"""
Pydantic models for Gitingest request/response schemas
"""
from typing import Optional, List
from pydantic import BaseModel, Field, validator


class IngestRequest(BaseModel):
    """Request model for repository ingestion"""
    url: str = Field(..., description="Repository URL or local path")
    pattern_type: str = Field("exclude", description="Pattern type: include or exclude")
    patterns: Optional[str] = Field(None, description="Comma-separated patterns")
    max_file_size_kb: Optional[int] = Field(None, description="Max file size in KB")
    github_pat: Optional[str] = Field(None, description="GitHub Personal Access Token")
    
    @validator('pattern_type')
    def validate_pattern_type(cls, v):
        if v not in ('include', 'exclude'):
            raise ValueError('pattern_type must be "include" or "exclude"')
        return v
    
    @validator('github_pat')
    def validate_github_pat(cls, v):
        if v is None:
            return v
        # Validate GitHub PAT format
        import re
        pat_patterns = [
            r'^ghp_[a-zA-Z0-9]{36,}$',  # Fine-grained PAT
            r'^gho_[a-zA-Z0-9]{36,}$',  # OAuth access token
            r'^ghu_[a-zA-Z0-9]{36,}$',  # GitHub App user-to-server
            r'^ghs_[a-zA-Z0-9]{36,}$',  # GitHub App server-to-server
            r'^ghr_[a-zA-Z0-9]{36,}$',  # Refresh token
            r'^github_pat_[a-zA-Z0-9_]{82,}$',  # New format PAT
        ]
        if not any(re.match(pattern, v) for pattern in pat_patterns):
            raise ValueError('Invalid GitHub PAT format')
        return v


class IngestResponse(BaseModel):
    """Response model for repository ingestion"""
    summary: str = Field(..., description="Summary of the repository")
    tree: str = Field(..., description="Directory tree structure")
    content: str = Field(..., description="File contents")
    file_count: int = Field(..., description="Number of files processed")
    estimated_tokens: int = Field(..., description="Estimated token count")
    repo_name: str = Field(..., description="Repository name")
    branch: Optional[str] = Field(None, description="Branch name")
    cached: bool = Field(False, description="Whether result was cached")


class RepoInfo(BaseModel):
    """Repository information extracted from URL"""
    host: str = Field(..., description="Git host (github, gitlab, etc.)")
    user: str = Field(..., description="Repository owner")
    repo: str = Field(..., description="Repository name")
    branch: Optional[str] = Field(None, description="Branch name")
    subpath: Optional[str] = Field(None, description="Subpath within repo")
    original_url: Optional[str] = Field(None, description="Original URL")
    
    @property
    def full_name(self) -> str:
        return f"{self.user}/{self.repo}"
    
    @property
    def clone_url(self) -> str:
        if self.host == "github":
            return f"https://github.com/{self.user}/{self.repo}.git"
        elif self.host == "gitlab":
            return f"https://gitlab.com/{self.user}/{self.repo}.git"
        elif self.host == "bitbucket":
            return f"https://bitbucket.org/{self.user}/{self.repo}.git"
        else:
            # Generic HTTPS
            return f"https://{self.host}/{self.user}/{self.repo}.git"


class ErrorResponse(BaseModel):
    """Error response model"""
    error: str
    detail: Optional[str] = None
    code: Optional[int] = None
