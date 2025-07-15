from ..errors import InstallerError


class StoppedByUserError(InstallerError):
  def __init__(self, reason=""):
    super().__init__(msg=reason)


class InvalidTargetDirectoryError(InstallerError):
  def __init__(self, path, *args: object) -> None:
    super().__init__(f"target directory '{path}' exists and is not empty", *args)
