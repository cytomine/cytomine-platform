from abc import ABC, abstractmethod
import subprocess
import uuid
import string
import secrets


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
        raise InvalidAutoGenerationData(
          self, "invalid type for 'freeze' parameter"
        )
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
    super().__init__(
      f"invalid field content for generation method '{generator.method_key}': {desc}",
      *args,
    )


class UnrecognizedGenerationField(ValueError):
  def __init__(self, generator: EnvValueGenerator, *args) -> None:
    super().__init__(
      f"field does not match generation method '{generator.method_key}' convention ",
      *args,
    )


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
  FIELD_LENGTH = "length"

  def _resolve(self, field):
    command = ["openssl", "rand", "-base64"]
    command.append(f"{self._get_length(field)}")
    return subprocess.check_output(command).decode("utf8").strip()

  @property
  def method_key(self):
    return "openssl"

  @property
  def accept_string_field(self):
    return False

  def _get_length(self, field):
    return field.get(self.FIELD_LENGTH, 32)

  def _validate(self, field):
    length = field.get(self.FIELD_LENGTH)
    if length is not None and not (isinstance(length, int) and length > 0):
      raise InvalidAutoGenerationData(self, "length should be an integer > 0")

    return self


class SecretGenerator(EnvValueGenerator):
  FIELD_LENGTH = "length"
  FIELD_WHITELIST = "whitelist"
  FIELD_BLACKLIST = "blacklist"
  """ Allow a subset of string.punctuation in order to avoid issues with bash env files """
  ALLOWED_PUNCTUATION = "!#%()*+, -.=?^_"

  def __init__(self) -> None:
    super().__init__()
    self._base_alphabet = (
      string.ascii_letters + string.digits + self.ALLOWED_PUNCTUATION
    )

  def _resolve(self, field):
    length = field.get(self.FIELD_LENGTH, 0)
    return "".join(secrets.choice(self._get_alphabet(field)) for i in range(length))

  def _get_alphabet(self, field):
    whitelist = field.get(self.FIELD_WHITELIST)
    if whitelist is not None:
      return whitelist
    alphabet = {*self._base_alphabet}
    alphabet = "".join(alphabet.difference(*field.get(self.FIELD_BLACKLIST, "")))
    return alphabet

  def _validate(self, field):
    length = field.get(self.FIELD_LENGTH)
    if length is not None and not (isinstance(length, int) and length > 0):
      raise InvalidAutoGenerationData(self, "length should be an integer >= 0")
    whitelist = field.get(self.FIELD_WHITELIST)
    if whitelist is not None and not (isinstance(whitelist, str) and length > 0):
      raise InvalidAutoGenerationData(
        self,
        "characters whitelist must be a string with one or more characters",
      )
    blacklist = field.get(self.FIELD_BLACKLIST)
    if blacklist is not None and not (isinstance(blacklist, str) and length > 0):
      raise InvalidAutoGenerationData(
        self,
        "characters blacklist must be a string with one or more characters",
      )
    if blacklist is not None and whitelist is not None:
      raise InvalidAutoGenerationData(
        self,
        "characters blacklist and whitelist cannot be set at the same time",
      )
    return self

  @property
  def accept_string_field(self):
    return True

  @property
  def method_key(self):
    return "secret"

  @property
  def base_alphabet(self):
    return self._base_alphabet


class EnvValueGeneratorFactory(object):
  def make_generator(self, field):
    """ """
    generators = [RandomUUIDGenerator(), OpenSSLGenerator(), SecretGenerator()]

    for generator in generators:
      try:
        return generator.validate(field)
      except UnrecognizedGenerationField:
        continue
      except InvalidAutoGenerationData:
        # invalid data exception will be handled by the resolve method
        return generator

    raise ValueError("impossible to identify the generation method")
