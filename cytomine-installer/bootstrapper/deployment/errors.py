

from .enums import CytomineEnvSectionEnum
from ..errors import BootstrapperError


class KeyAlreadyExistsError(BootstrapperError):
  def __init__(self, ns, key, *args: object) -> None:
    super().__init__(f"key '{key}' already exists in namespace '{ns}'", *args)


class UnknownValueTypeError(BootstrapperError):
  def __init__(self, value_type, *args: object) -> None:
    super().__init__(f"unknown value type: {value_type}", *args)


class InvalidServerConfigurationError(BootstrapperError):
  pass 


class MissingCytomineYamlFileError(InvalidServerConfigurationError):
  def __init__(self, path, filename="cytomine.yml", *args: object) -> None:
    super().__init__(f"missing cytomine configuration '{filename}' in '{path}'", *args)



class NoDockerComposeYamlFileError(InvalidServerConfigurationError):
  def __init__(self, path, *args: object) -> None:
    super().__init__(f"no docker-compose.yml file found in '{path}'", *args)



class MissingCytomineYamlFileError(InvalidServerConfigurationError):
  def __init__(self, path, filename="cytomine.yml", *args: object) -> None:
    super().__init__(f"missing cytomine configuration '{filename}' in '{path}'", *args)


class UnknownCytomineEnvSection(BootstrapperError):
  def __init__(self, section, *args: object) -> None:
    available_values = ', '.join(list(map(lambda v: v.value, CytomineEnvSectionEnum)))
    super().__init__(f"unknown section '{section}', expects one of {{{available_values}}}", *args)


class UnknownServiceError(BootstrapperError):
  def __init__(self, service, *args: object) -> None:
    super().__init__(f"unknown service '{service}'", *args)
