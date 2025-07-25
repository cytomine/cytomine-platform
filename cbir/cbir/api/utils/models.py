"""DTO Models"""

from pydantic import BaseModel


class Storage(BaseModel):
    """
    Storage model.
    """

    name: str
