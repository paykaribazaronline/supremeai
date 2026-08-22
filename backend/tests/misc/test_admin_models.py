from models.admin import (
    AdminEasyLoginRequest,
    AdminFirebaseLoginRequest,
    AdminFirebaseTotpSetupRequest,
    AdminFirebaseTotpVerifyRequest,
)


def test_admin_firebase_login_request():
    req = AdminFirebaseLoginRequest(id_token="token")
    assert req.id_token == "token"


def test_admin_firebase_totp_setup_request():
    req = AdminFirebaseTotpSetupRequest(id_token="token")
    assert req.id_token == "token"


def test_admin_firebase_totp_verify_request():
    req = AdminFirebaseTotpVerifyRequest(id_token="token", otp="789012")
    assert req.id_token == "token"
    assert req.otp == "789012"


def test_admin_easy_login_request():
    req = AdminEasyLoginRequest(code="easy-code")
    assert req.code == "easy-code"
