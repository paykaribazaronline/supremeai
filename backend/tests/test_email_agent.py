import pytest
from tools.email_agent import EmailAgent

def test_email_agent_connection():
    agent = EmailAgent()
    assert agent.connect_gmail_oauth("gmail", ["scope1"]) is True
    assert agent.connect_imap("imap.gmail.com", 993, "test@example.com", "pass") is True

def test_otp_extraction():
    agent = EmailAgent()
    body = "Your verification code is 482910."
    assert agent.extract_otp(body) == "482910"
    
    body_no_otp = "Welcome to SupremeAI!"
    assert agent.extract_otp(body_no_otp) == ""

def test_signup_flow():
    agent = EmailAgent()
    res = agent.signup_flow("example.com")
    assert res["status"] == "success"
    assert "otp" in res
