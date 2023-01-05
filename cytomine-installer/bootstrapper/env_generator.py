from abc import ABC, abstractmethod
from io import StringIO
import subprocess
import uuid



class EnvValueGenerator(ABC):
  """A generic class for auto-generating env values"""
  @property
  @abstractmethod
  def method_key(self):
    pass

  @property
  @abstractmethod
  def accept_string_field(self):
    pass

  @abstractmethod
  def _resolve(self, field):
    """
    Parameters
    ----------
    field: Any
      The content of a {value} field as parsed by the yaml parser. 

    Returns
    -------
    value: str
      The auto-generated value
    """
    pass

  def resolve(self, field):
    """
    Parameters
    ----------
    field: Any
      The content of a {value} field as parsed by the yaml parser. 

    Returns
    -------
    value: str
      The auto-generated value

    Raises
    ------
    InvalidAutoGenerationData: 
      When the content of the field is invalid for the selected
      auto-generation method 
    """
    self.validate(field)
    return self._resolve(field)

  def validate(self, field):
    """
    Returns
    -------
    generator: EnvValueGenerator
      The current generator
    
    Raises
    ------
    InvalidAutoGenerationData: 
      When the generation method has been recognized but the content of the field
      cannot be used for generating the value with the given method
    UnrecognizedGenerationField:
      When the content of the field does not match the generation method
    """
    standardized_field = dict()
    if isinstance(field, str) and self.accept_string_field:
      standardized_field["type"] = field
    elif isinstance(field, dict) and "type" in field:
      if "freeze" in field and not isinstance(field["freeze"], bool):
        raise InvalidAutoGenerationData(self, "invalid type for 'freeze' parameter")
      standardized_field = field
    else:
      raise UnrecognizedGenerationField(self)
    if standardized_field["type"] != self.method_key:
      raise UnrecognizedGenerationField(self)
    return self._validate(standardized_field)

  def _validate(self, field: dict):
    # to implement custom checks on the content of a field 
    return self


class InvalidAutoGenerationData(ValueError):
  def __init__(self, generator: EnvValueGenerator, desc, *args):
    super().__init__(f"invalid field content for generation method '{generator.method_key}': {desc}", *args)


class UnrecognizedGenerationField(ValueError):
  def __init__(self, generator: EnvValueGenerator, *args) -> None:
    super().__init__(f"field does not match generation method '{generator.method_key}' convention ", *args)


class RandomUUIDGenerator(EnvValueGenerator):
  def _resolve(self, field):
    return str(uuid.uuid4())

  @property
  def accept_string_field(self):
    return True

  @property
  def method_key(self):
    return "random_uuid"


class OpenSSLGenerator(EnvValueGenerator):
  LENGTH_FIELD = "length"
  BASE64_FIELD = "base64"

  def _resolve(self, field):
    command = ["openssl", "rand"]
    if self._is_base64(field):
      command.append("-base64")
    command.append(f"{self._get_length(field)}")
    return subprocess.check_output(command).decode("utf8").strip()

  @property
  def method_key(self):
    return "openssl"
  
  @property
  def accept_string_field(self):
    return False

  def _is_base64(self, field):
    return field.get("base64", False) 

  def _get_length(self, field):
    return field.get("length", 0)

  def _validate(self, field):
    base64 = field.get("base64")
    if base64 is not None and not isinstance(base64, bool):
      raise InvalidAutoGenerationData(self, "base64 should be a boolean")
    
    length = field.get("length")
    if length is not None and not (isinstance(length, int) and length > 0):
      raise InvalidAutoGenerationData(self, "length should be an integer > 0")
    
    return self


class EnvValueGeneratorFactory(object):
  def make_generator(self, field):
    """
    """
    generators = [
      RandomUUIDGenerator(),
      OpenSSLGenerator()
    ]

    for generator in generators:
      try:
        return generator.validate(field)
      except UnrecognizedGenerationField:
        continue
      except InvalidAutoGenerationData:
        # invalid data exception will be handled by the resolve method
        return generator
    
    raise ValueError("impossible to identify the generation method")