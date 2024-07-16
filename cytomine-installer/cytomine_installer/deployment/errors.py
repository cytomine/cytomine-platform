from .enums import ConfigSectionEnum
from ..errors import InstallerError


class KeyAlreadyExistsError(InstallerError):
  def __init__(self, ns, key, *args: object) -> None:
    super().__init__(f"key '{key}' already exists in namespace '{ns}'", *args)


class UnknownValueTypeError(InstallerError):
  def __init__(self, value_type, *args: object) -> None:
    super().__init__(f"unknown value type: {value_type}", *args)


class InvalidServerConfigurationError(InstallerError):
  pass


class MissingConfigFileError(InvalidServerConfigurationError):
  def __init__(self, path, filename="cytomine.yml", *args: object) -> None:
    super().__init__(f"missing cytomine configuration '{filename}' in '{path}'", *args)


class NoDockerComposeYamlFileError(InvalidServerConfigurationError):
  def __init__(self, path, *args: object) -> None:
    super().__init__(f"no docker-compose.yml file found in '{path}'", *args)


class MissingConfigFileError(InvalidServerConfigurationError):
  def __init__(self, path, filename="cytomine.yml", *args: object) -> None:
    super().__init__(f"missing cytomine configuration '{filename}' in '{path}'", *args)


class UnknownConfigSection(InstallerError):
  def __init__(self, section, *args: object) -> None:
    available_values = ", ".join(list(map(lambda v: v.value, ConfigSectionEnum)))
    super().__init__(f"unknown section '{section}', expects one of {{{available_values}}}", *args)


class UnknownServiceError(InstallerError):
  def __init__(self, service, *args: object) -> None:
    super().__init__(f"unknown service '{service}'", *args)


class InvalidGlobalValue(InstallerError):
  def __init__(self, ns, key, value, *args: object) -> None:
    super().__init__(f"invalid value '{value}' for '{key}' in namespace '{ns}', expecting a global variable reference like '$GLOBAL_NAMESPACE.$VAR_NAME'", *args)
