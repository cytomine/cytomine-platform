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

  @abstractmethod
  def as_dict(self):
    """Generates a directory"""
    pass

class EnvStore(BaseEnvStore):
  def __init__(self) -> None:
    self._store = defaultdict(dict)
    self._initial_type = defaultdict(dict)
    self._initial_value = defaultdict(dict)
  
  def _add_env(self, ns: str, key: str, value, store_value_fn, _type: EnvValueTypeEnum):
    """Register an envirom"""
    if key in self._store[ns]:
      raise KeyAlreadyExistsError(ns, key)
    self._store[ns][key] = store_value_fn()
    self._initial_type[ns][key] = _type
    self._initial_value[ns][key] = value

  def add_namespace(self, ns: str, entries: dict, store=None):
    if ns in self._store:
      raise ValueError(f"namespace '{ns}' already exists in env store")
    
    for k, v in entries.get(EnvValueTypeEnum.CONSTANT.value, {}).items():
      self._add_env(ns, k, v, lambda: v, EnvValueTypeEnum.CONSTANT)
    
    if EnvValueTypeEnum.GLOBAL.value in entries and len(entries[EnvValueTypeEnum.GLOBAL.value]) > 0 and store is None:
      raise ValueError("'global' is not supported in this section")

    for k, v in entries.get(EnvValueTypeEnum.GLOBAL.value, {}).items():
      other_ns, other_key = v.split(".")
      self._add_env(ns, k, v, lambda: store.get_value(other_ns, other_key), EnvValueTypeEnum.GLOBAL)

    gen_factory = EnvValueGeneratorFactory()
    for k, v in entries.get(EnvValueTypeEnum.AUTOGENERATE.value, {}).items():
      generator = gen_factory.make_generator(v)
      self._add_env(ns, k, v, lambda: generator.resolve(v), EnvValueTypeEnum.AUTOGENERATE)
      
  @property
  def namespaces(self):
    return list(self._store.keys())

  def get_value(self, ns: str, key: str):
    if ns not in self._store:
      raise ValueError(f"unknown namespace '{ns}'")
    if key not in self._store[ns]:
      raise ValueError(f"unknown key '{key}' in namespace '{ns}'")
    return self._store[ns][key]

  def as_dict(self):
    output_dict = defaultdict(lambda: defaultdict(dict))
    for ns in self.namespaces:
      for key in self._store[ns].keys():
        init_type, init_value = self._initial_type[ns][key], self._initial_value[ns][key]
        if init_type == EnvValueTypeEnum.AUTOGENERATE and (isinstance(init_value, str) or init_value["freeze"]):
          output_dict[ns][EnvValueTypeEnum.CONSTANT.value][key] = self._store[ns][key]
        else:
          output_dict[ns][init_type.value][key] = self._initial_value[ns][key]
    return output_dict