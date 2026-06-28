"""
SupremeAI 2.0 — Admin Pydantic Models
বাংলা মন্তব্য: অ্যাডমিন অথেন্টিকেশন ও ম্যানেজমেন্ট রাউটগুলোর জন্য ইনপুট ভ্যালিডেশন স্কিমা
"""

from pydantic import BaseModel
from pydantic import Field


class AdminLoginRequest(BaseModel):
    password: str = Field(..., description="Admin password")


class AdminVerifyRequest(BaseModel):
    password: str = Field(..., description="Admin password")
    otp: str = Field(..., description="TOTP MFA OTP code")


class AdminFirebaseLoginRequest(BaseModel):
    id_token: str = Field(..., description="Firebase ID token")


class AdminFirebaseTotpSetupRequest(BaseModel):
    id_token: str = Field(..., description="Firebase ID token")


class AdminFirebaseTotpVerifyRequest(BaseModel):
    id_token: str = Field(..., description="Firebase ID token")
    otp: str = Field(..., description="TOTP MFA OTP code")


class AdminEasyLoginRequest(BaseModel):
    code: str = Field(..., description="Easy login authentication code")
