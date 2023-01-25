from bootstrapper.errors import BootstrapperError


class StoppedByUserError(BootstrapperError):
  def __init__(self, reason=""):
    super().__init__(msg=reason)


class SameDirectoryError(BootstrapperError):
  def __init__(self, path, *args: object) -> None:
    super().__init__(f"target directory '{path}' is not empty", *args)