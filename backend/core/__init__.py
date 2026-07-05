# FILE_PATH: core/__init__.py
import time

import pyotp


def verify_totp_code(otp: str, secret: str) -> bool:
    totp = pyotp.TOTP(secret)
    # Using 'valid_window=1' allows for a grace period of one step before or after the current time.
    # This helps mitigate minor clock drifts between the client generating the OTP and the server verifying it.
    is_valid = totp.verify(otp, valid_window=1)
    return is_valid

def check_totp(otp: str, secret: str) -> bool:
    return verify_totp_code(otp, secret)
