import xml.etree.ElementTree as ET
from typing import Dict, Any, List, Optional
from urllib.parse import urlparse, parse_qs
from loguru import logger

try:
    from jose import JWTError, jwt as jose_jwt
    _JOSE_AVAILABLE = True
except ImportError:
    _JOSE_AVAILABLE = False
    JWTError = Exception  # type: ignore[misc,assignment]
    jose_jwt = None  # type: ignore[assignment]


class SSOIntegrator:
    def __init__(self, saml_settings: Optional[Dict[str, Any]] = None):
        self.saml_settings = saml_settings or {}
        self.onelogin = self._load_onelogin()
        logger.info(f"Initialized SSOIntegrator (python-saml={'loaded' if self.onelogin else 'fallback'})")

    def _load_onelogin(self):
        try:
            from onelogin.saml2.auth import OneLogin_Saml2_Auth
            from onelogin.saml2.settings import OneLogin_Saml2_Settings
            self._OneLogin_Saml2_Auth = OneLogin_Saml2_Auth
            self._OneLogin_Saml2_Settings = OneLogin_Saml2_Settings
            return True
        except ImportError:
            return False

    def _prepare_request(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        if self.onelogin:
            return {
                "https": "on" if self.saml_settings.get("sp_entity_id", "").startswith("https") else "off",
                "http_host": self.saml_settings.get("sp_entity_id", "") or "localhost",
                "script_name": self.saml_settings.get("acs_url", ""),
                "get_data": parse_qs(urlparse(self.saml_settings.get("query_string", "")).query),
                "post_data": request_data.get("post_data", {}),
            }
        return request_data

    def get_metadata(self) -> Dict[str, Any]:
        if self.onelogin:
            try:
                settings_obj = self._build_settings()
                metadata = settings_obj.get_sp_metadata()
                return {"status": "success", "content_type": "application/xml", "body": metadata}
            except Exception as exc:
                logger.error(f"Metadata generation failed: {exc}")
        xml = self._fallback_metadata()
        return {"status": "fallback", "content_type": "application/xml", "body": xml}

    def get_sso_url(self, relay_state: Optional[str] = None) -> str:
        if self.onelogin:
            try:
                settings_obj = self._build_settings()
                req = self._prepare_request({})
                auth = self._OneLogin_Saml2_Auth(req, old_settings=settings_obj)
                sso_url = auth.login(return_to=relay_state)
                return sso_url
            except Exception as exc:
                logger.error(f"SSO URL generation failed: {exc}")
        return self.saml_settings.get("idp_sso_url", "")

    async def process_sso_response(self, post_data: Dict[str, Any], relay_state: Optional[str] = None) -> Dict[str, Any]:
        if self.onelogin:
            try:
                settings_obj = self._build_settings()
                req = self._prepare_request({"post_data": post_data})
                auth = self._OneLogin_Saml2_Auth(req, old_settings=settings_obj)
                auth.process_response()
                errors = auth.get_errors()
                if errors:
                    return {"status": "error", "message": ", ".join(errors)}
                if not auth.is_authenticated():
                    return {"status": "error", "message": "Not authenticated"}
                attributes = auth.get_attributes()
                name_id = auth.get_nameid()
                session_index = auth.get_session_index()
                user_id = attributes.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier", [name_id])[0]
                email = attributes.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress", [""])[0]
                groups = attributes.get("http://schemas.microsoft.com/ws/2008/06/identity/claims/groups", []) or \
                         attributes.get("groups", []) or \
                         [v for k, v in attributes.items() if "group" in k.lower() for v in (v if isinstance(v, list) else [v])]
                roles = self.map_roles(groups)
                return {
                    "status": "success",
                    "user_id": user_id,
                    "email": email,
                    "name_id": name_id,
                    "session_index": session_index,
                    "groups": groups,
                    "roles": roles,
                    "method": "python-saml",
                }
            except Exception as exc:
                logger.error(f"SAML response processing failed: {exc}")
        try:
            root = ET.fromstring(post_data.get("SAMLResponse", ""))
            logger.info("SAML XML parsed successfully.")
            user_id = root.findtext(".//{urn:oasis:names:tc:SAML:2.0:assertion}Subject/{urn:oasis:names:tc:SAML:2.0:assertion}NameID", default="")
            groups_el = root.findall(".//{urn:oasis:names:tc:SAML:2.0:assertion}AttributeStatement//{urn:oasis:names:tc:SAML:2.0:assertion}Attribute[@Name='groups']/{urn:oasis:names:tc:SAML:2.0:assertion}AttributeValue")
            groups = [el.text for el in groups_el if el.text]
            email_el = root.find(".//{urn:oasis:names:tc:SAML:2.0:assertion}AttributeStatement//{urn:oasis:names:tc:SAML:2.0:assertion}Attribute[@Name='email']/{urn:oasis:names:tc:SAML:2.0:assertion}AttributeValue")
            email = email_el.text if email_el is not None else ""
            return {
                "status": "success",
                "user_id": user_id,
                "email": email,
                "groups": groups,
                "roles": self.map_roles(groups),
                "method": "xml_fallback",
            }
        except ET.ParseError as exc:
            logger.error(f"Fallback SAML parsing failed: {exc}")
            return {"status": "error", "message": "Invalid SAML response"}

    def map_roles(self, sso_groups: List[str]) -> List[str]:
        internal_roles: List[str] = []
        mapping = {
            "Admin": "owner",
            "Administrators": "owner",
            "Developers": "editor",
            "Editors": "editor",
            "Viewers": "viewer",
            "Operators": "operator",
        }
        for group in sso_groups:
            role = mapping.get(group)
            if role:
                internal_roles.append(role)
        return internal_roles or ["viewer"]

    def get_logout_url(self, request: Optional[Dict[str, Any]] = None, relay_state: Optional[str] = None) -> str:
        if self.onelogin:
            try:
                settings_obj = self._build_settings()
                req = self._prepare_request(request or {})
                auth = self._OneLogin_Saml2_Auth(req, old_settings=settings_obj)
                return auth.logout(return_to=relay_state)
            except Exception as exc:
                logger.error(f"Logout URL generation failed: {exc}")
        return self.saml_settings.get("idp_slo_url", "")

    async def process_slo_response(self, post_data: Dict[str, Any]) -> Dict[str, Any]:
        if self.onelogin:
            try:
                settings_obj = self._build_settings()
                req = self._prepare_request({"post_data": post_data})
                auth = self._OneLogin_Saml2_Auth(req, old_settings=settings_obj)
                auth.process_slo(delete_session_callback=lambda: None)
                return {"status": "success", "method": "python-saml"}
            except Exception as exc:
                logger.error(f"SLO processing failed: {exc}")
        return {"status": "success", "method": "mock_fallback"}

    def _build_settings(self) -> Any:
        settings_dict = {
            "strict": False,
            "debug": True,
            "sp": {
                "entityId": self.saml_settings.get("sp_entity_id", "https://supremeai.com/metadata"),
                "assertionConsumerService": {
                    "url": self.saml_settings.get("acs_url", "https://supremeai.com/acs"),
                },
                "singleLogoutService": {
                    "url": self.saml_settings.get("sls_url", "https://supremeai.com/sls"),
                },
                "NameIDFormat": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                "x509cert": self.saml_settings.get("sp_x509_cert", ""),
                "privateKey": self.saml_settings.get("sp_private_key", ""),
            },
            "idp": {
                "entityId": self.saml_settings.get("idp_entity_id", ""),
                "singleSignOnServiceUrl": self.saml_settings.get("idp_sso_url", ""),
                "singleLogoutServiceUrl": self.saml_settings.get("idp_slo_url", ""),
                "x509cert": self.saml_settings.get("idp_x509_cert", ""),
            },
            "security": {
                "authnRequestsSigned": False,
                "logoutRequestSigned": False,
                "logoutResponseSigned": False,
                "signMetadata": False,
            },
        }
        if self.onelogin:
            return self._OneLogin_Saml2_Settings(settings=settings_dict, security=self.saml_settings.get("security", {}))
        return settings_dict

    def _fallback_metadata(self) -> str:
        sp_entity_id = self.saml_settings.get("sp_entity_id", "https://supremeai.com")
        acs_url = self.saml_settings.get("acs_url", f"{sp_entity_id}/acs")
        return f"""<?xml version="1.0"?>
<EntityDescriptor xmlns="urn:oasis:names:tc:SAML:2.0:metadata" entityID="{sp_entity_id}">
  <SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
    <AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="{acs_url}" index="1"/>
  </SPSSODescriptor>
</EntityDescriptor>"""

    # ── OIDC helpers ───────────────────────────────────────────────

    OIDC_PROVIDERS = {
        "okta": {
            "authorization_endpoint": "https://{domain}/oauth2/default/v1/authorize",
            "token_endpoint": "https://{domain}/oauth2/default/v1/token",
            "userinfo_endpoint": "https://{domain}/oauth2/default/v1/userinfo",
            "jwks_uri": "https://{domain}/oauth2/default/v1/keys",
        },
        "azure": {
            "authorization_endpoint": "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize",
            "token_endpoint": "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token",
            "userinfo_endpoint": "https://graph.microsoft.com/oidc/userinfo",
            "jwks_uri": "https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys",
        },
        "google": {
            "authorization_endpoint": "https://accounts.google.com/o/oauth2/v2/auth",
            "token_endpoint": "https://oauth2.googleapis.com/token",
            "userinfo_endpoint": "https://openidconnect.googleapis.com/v1/userinfo",
            "jwks_uri": "https://www.googleapis.com/oauth2/v3/certs",
        },
    }

    def get_oidc_auth_url(
        self,
        provider: str,
        client_id: str,
        redirect_uri: str,
        state: str,
        scope: str = "openid profile email",
        extra_params: Optional[Dict[str, str]] = None,
    ) -> str:
        cfg = self.OIDC_PROVIDERS.get(provider.lower())
        if not cfg:
            raise ValueError(f"Unsupported OIDC provider: {provider}")
        base = cfg["authorization_endpoint"].format(
            domain=self.saml_settings.get("oidc_domain", ""),
            tenant=self.saml_settings.get("oidc_tenant", ""),
        )
        params = {
            "client_id": client_id,
            "redirect_uri": redirect_uri,
            "response_type": "code",
            "scope": scope,
            "state": state,
        }
        if extra_params:
            params.update(extra_params)
        qs = "&".join(f"{k}={v}" for k, v in params.items())
        return f"{base}?{qs}"

    async def exchange_oidc_code(
        self,
        provider: str,
        code: str,
        redirect_uri: str,
        client_id: str,
        client_secret: str,
    ) -> Dict[str, Any]:
        cfg = self.OIDC_PROVIDERS.get(provider.lower())
        if not cfg:
            return {"status": "error", "message": f"Unsupported OIDC provider: {provider}"}
        token_url = cfg["token_endpoint"].format(
            domain=self.saml_settings.get("oidc_domain", ""),
            tenant=self.saml_settings.get("oidc_tenant", ""),
        )
        try:
            import httpx
            payload = {
                "grant_type": "authorization_code",
                "code": code,
                "redirect_uri": redirect_uri,
                "client_id": client_id,
                "client_secret": client_secret,
            }
            async with httpx.AsyncClient() as client:
                resp = await client.post(token_url, data=payload, timeout=10.0)
                resp.raise_for_status()
                tokens = resp.json()
            id_token = tokens.get("id_token", "")
            if id_token and _JOSE_AVAILABLE and jose_jwt is not None:
                # Verify basic structure; real apps should verify signature via jwks_uri
                header = jose_jwt.get_unverified_header(id_token)
                payload = jose_jwt.get_unverified_claims(id_token)
                return {
                    "status": "success",
                    "id_token": id_token,
                    "access_token": tokens.get("access_token", ""),
                    "claims": payload,
                    "header": header,
                }
            return {"status": "success", "tokens": tokens}
        except Exception as exc:
            logger.error(f"OIDC code exchange failed: {exc}")
            return {"status": "error", "message": str(exc)}

    async def process_oidc_response(self, provider: str, code: str, state: str) -> Dict[str, Any]:
        """Convenience wrapper: exchange code, fetch userinfo, map roles."""
        client_id = self.saml_settings.get("oidc_client_id", "")
        client_secret = self.saml_settings.get("oidc_client_secret", "")
        redirect_uri = self.saml_settings.get("oidc_redirect_uri", "")
        exchange = await self.exchange_oidc_code(
            provider=provider,
            code=code,
            redirect_uri=redirect_uri,
            client_id=client_id,
            client_secret=client_secret,
        )
        if exchange.get("status") != "success":
            return exchange

        claims = exchange.get("claims", {})
        user_id = claims.get("sub") or claims.get("email", "unknown")
        email = claims.get("email", "")
        # Provider-specific groups extraction
        groups: List[str] = []
        if provider == "okta":
            groups = claims.get("groups", [])
        elif provider == "azure":
            groups = claims.get("groups", [])
        elif provider == "google":
            groups = claims.get("hd", "").split(",") if claims.get("hd") else []
        roles = self.map_roles(groups)
        return {
            "status": "success",
            "user_id": user_id,
            "email": email,
            "groups": groups,
            "roles": roles,
            "method": f"oidc:{provider}",
            "tokens": exchange.get("tokens", {}),
        }
