from bootstrapper.errors import BootstrapperError


class StoppedByUserError(BootstrapperError):
  def __init__(self, reason=""):
    super().__init__(msg=reason)