import enum
from collections import defaultdict

from bootstrapper.env_generator import EnvValueGeneratorFactory


class EnvValueTypeEnum(enum.Enum):
  CONSTANT = "constant"
  GLOBAL = "global"
  AUTOGENERATE = "auto"


class EnvStore:
  def __init__(self) -> None:
    self._store = defaultdict(dict)
    self._store_type = defaultdict(dict)
  
  def _add_env(self, ns, key, value_fn, _type):
    self._store[ns][key] = value_fn()
    self._store_type[ns][key] = _type

  def add_namespace(self, ns: str, d: dict, store=None):
    if ns in self._store:
      raise ValueError(f"namespace '{ns}' already exists in env store")
    
    for k, v in d.get(EnvValueTypeEnum.CONSTANT.value, {}).items():
      self._add_env(ns, k, lambda: v, EnvValueTypeEnum.CONSTANT)
    
    if EnvValueTypeEnum.GLOBAL.value in d and len(d[EnvValueTypeEnum.GLOBAL.value]) > 0 and store is None:
      raise ValueError("'global' is not supported in this section")

    for k, v in d.get(EnvValueTypeEnum.GLOBAL.value, {}):
      other_ns, other_key = v.split(".")
      self._add_env(ns, k, lambda: store.get(other_ns, other_key), EnvValueTypeEnum.GLOBAL)

    gen_factory = EnvValueGeneratorFactory()
    for k, v in d.get(EnvValueTypeEnum.AUTOGENERATE.value, {}):
      generator = gen_factory.make_generator(v)
      self._add_env(ns, k, lambda: generator.resolve(v), EnvValueTypeEnum.AUTOGENERATE)
      
  def get_value(self, ns: str, key: str):
    if ns not in self._store:
      raise ValueError(f"unknown namespace '{ns}'")
    if key not in self._store[ns]:
      raise ValueError(f"unknown key '{key}' in namespace '{ns}'")
    return self._store[ns][key]