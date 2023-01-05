from abc import ABC, abstractmethod
import enum
from collections import defaultdict

from bootstrapper.env_generator import EnvValueGeneratorFactory


class EnvValueTypeEnum(enum.Enum):
  CONSTANT = "constant"
  GLOBAL = "global"
  AUTOGENERATE = "auto"


class KeyAlreadyExistsError(KeyError):
  def __init__(self, ns, key, *args: object) -> None:
    super().__init__(f"key '{key}' already exists in namespace '{ns}'", *args)


class BaseEnvStore(ABC):
  @abstractmethod
  def get_value(namespace: str, key: str):
    """Get a value stored in the EnvStore"""
    pass


class EnvStore(BaseEnvStore):
  def __init__(self) -> None:
    self._store = defaultdict(dict)
    self._store_type = defaultdict(dict)
  
  def _add_env(self, ns, key, value_fn, _type):
    if key in self._store[ns]:
      raise KeyAlreadyExistsError(ns, key)
    self._store[ns][key] = value_fn()
    self._store_type[ns][key] = _type

  def add_namespace(self, ns: str, entries: dict, store=None):
    if ns in self._store:
      raise ValueError(f"namespace '{ns}' already exists in env store")
    
    for k, v in entries.get(EnvValueTypeEnum.CONSTANT.value, {}).items():
      self._add_env(ns, k, lambda: v, EnvValueTypeEnum.CONSTANT)
    
    if EnvValueTypeEnum.GLOBAL.value in entries and len(entries[EnvValueTypeEnum.GLOBAL.value]) > 0 and store is None:
      raise ValueError("'global' is not supported in this section")

    for k, v in entries.get(EnvValueTypeEnum.GLOBAL.value, {}).items():
      other_ns, other_key = v.split(".")
      self._add_env(ns, k, lambda: store.get_value(other_ns, other_key), EnvValueTypeEnum.GLOBAL)

    gen_factory = EnvValueGeneratorFactory()
    for k, v in entries.get(EnvValueTypeEnum.AUTOGENERATE.value, {}).items():
      generator = gen_factory.make_generator(v)
      self._add_env(ns, k, lambda: generator.resolve(v), EnvValueTypeEnum.AUTOGENERATE)
      
  def get_value(self, ns: str, key: str):
    if ns not in self._store:
      raise ValueError(f"unknown namespace '{ns}'")
    if key not in self._store[ns]:
      raise ValueError(f"unknown key '{key}' in namespace '{ns}'")
    return self._store[ns][key]