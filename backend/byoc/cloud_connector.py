class CloudStatus:
    connected: bool = False
    provider: str = "gcp"
    region: str | None = None


class CloudResource:
    id: str
    type: str
    name: str
    status: str


async def ping() -> CloudStatus:
    return CloudStatus()


async def list_resources() -> list[CloudResource]:
    return []
