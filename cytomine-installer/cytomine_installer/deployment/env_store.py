from abc import ABC, abstractmethod
import enum
from collections import defaultdict
import json
import logging

from cytomine_installer.deployment.util.trie import Trie

from .errors import InvalidGlobalValue, KeyAlreadyExistsError, UnknownValueTypeError
from .env_generator import EnvValueGeneratorFactory


class EnvValueTypeEnum(enum.Enum):
  CONSTANT = "constant"
  GLOBAL = "global"
  AUTOGENERATE = "auto"


class MergeEnvStorePolicy(enum.Enum):
  # merge by preserving variables inside the merge target envstore
  PRESERVE = "preserve_target"
  # merge by overwriting variable inside the merge target envstore
  OVERWRITE = "overwrite_target"
  # merge by overwriting variable inside the merge target envstore if they are listed in an allow list
  ALLOW_LIST = "overwrite_by_allow_list"


class DictExportable(ABC):
  @abstractmethod
  def export_dict(self):
    """Generates an dictionary for env store export"""
    pass


class BaseEnvStore(DictExportable):
  @abstractmethod
  def get_env(self, namespace: str, key: str):
    """Get a value stored in the EnvStore"""
    pass

  @abstractmethod
  def has_env(self, namespace: str, key: str):
    """Check if a namespace has a key"""
    pass

  @abstractmethod
  def get_namespace_envs(self, namespace: str):
    """Get all values of a given namespace as a flat dictionary"""
    pass

  @abstractmethod
  def has_namespace(self, ns: str):
    """Checks whether a namespace exists or not"""
    pass


class EnvStore(BaseEnvStore):
  def __init__(self) -> None:
    # all dict here map (namespace, key) with:
    # - a lambda that returns the resolved value of the variable
    self._store = defaultdict(dict)
    # - the type of the variable (constant, auto, etc), see EnvValueTypeEnum
    self._initial_type = defaultdict(dict)
    # - the raw value before resolution (can be a dictionary)
    self._initial_value = defaultdict(dict)
    # - the final value, only filled when the variable needs to be resolved for the first time
    self._resolved_value = defaultdict(dict)

  def has_env(self, namespace: str, key: str):
    return namespace in self._store and key in self._store[namespace]

  def _set_env(
    self,
    ns: str,
    key: str,
    value,
    _type: EnvValueTypeEnum,
    other_store=None,
    allow_update=False,
  ):
    """Register an envirom"""
    if not allow_update and key in self._store[ns]:
      raise KeyAlreadyExistsError(ns, key)

    # generate correct store lambda based on value type
    if _type == EnvValueTypeEnum.CONSTANT:
      self._store[ns][key] = lambda: value
    elif _type == EnvValueTypeEnum.GLOBAL:
      try:
        other_ns, other_key = value.split(".")
      except ValueError:
        raise InvalidGlobalValue(ns, key, value)
      self._store[ns][key] = lambda: other_store.get_env(other_ns, other_key)
    elif _type == EnvValueTypeEnum.AUTOGENERATE:
      gen_factory = EnvValueGeneratorFactory()
      generator = gen_factory.make_generator(value)
      self._store[ns][key] = lambda: generator.resolve(value)
    else:
      raise UnknownValueTypeError(_type)

    self._initial_type[ns][key] = _type
    self._initial_value[ns][key] = value

  def add_namespace(self, ns: str, entries: dict, store=None):
    """Add a namespace to the current env-store
    Parameters
    ----------
    ns: str
      Namespace key
    entries: dict
      The entries of the namespace
    store: EnvStore
      The store in which to look for global
    """
    if ns in self._store:
      raise ValueError(f"namespace '{ns}' already exists in env store")

    # check for unknown value-type
    for value_type in entries.keys():
      try:
        EnvValueTypeEnum(value_type)
      except ValueError:
        raise UnknownValueTypeError(value_type)

    if (
      EnvValueTypeEnum.GLOBAL.value in entries
      and len(entries[EnvValueTypeEnum.GLOBAL.value]) > 0
      and store is None
    ):
      raise ValueError(
        f"'{EnvValueTypeEnum.GLOBAL.value}' is not supported in this section, namespace {ns}"
      )

    for value_type in EnvValueTypeEnum:
      for k, v in entries.get(value_type.value, {}).items():
        self._set_env(ns, k, v, value_type, other_store=store)

  @property
  def namespaces(self):
    return list(self._store.keys())

  def get_env(self, ns: str, key: str):
    """Resolves variables"""
    if ns not in self._store:
      raise ValueError(f"unknown namespace '{ns}'")
    if key not in self._store[ns]:
      raise ValueError(f"unknown key '{key}' in namespace '{ns}'")
    # resolve once
    if key not in self._resolved_value[ns]:
      self._resolved_value[ns][key] = self._store[ns][key]()
    return self._resolved_value[ns][key]

  @classmethod
  def _check_is_type_auto_and_frozen(cls, _type, value):
    """Check if a field is auto-generate type and frozen"""
    return _type == EnvValueTypeEnum.AUTOGENERATE and (
      isinstance(value, str) or value.get("freeze", True)
    )

  def export_dict(self):
    """
    Resolves the `auto` variables (that need to be frozen)
    """
    output_dict = defaultdict(lambda: defaultdict(dict))
    for ns in self.namespaces:
      for key in self._store[ns].keys():
        init_type, init_value = (
          self._initial_type[ns][key],
          self._initial_value[ns][key],
        )
        if self._check_is_type_auto_and_frozen(init_type, init_value):
          output_dict[ns][EnvValueTypeEnum.CONSTANT.value][
            key
          ] = self.get_env(ns, key)
        else:
          output_dict[ns][init_type.value][key] = self._initial_value[ns][key]
    if len(output_dict) == 0:
      return None
    else:
      # https://stackoverflow.com/a/32303615
      # convert to plain dict
      return json.loads(json.dumps(output_dict))

  def get_namespace_envs(self, ns: str):
    """Resolves variables"""
    if ns not in self._store:
      raise ValueError(f"unknown namespace '{ns}'")
    return {key: self.get_env(ns, key) for key in self._store[ns].keys()}

  def has_namespace(self, ns: str):
    return ns in self._store

  def _merge_env(
    self,
    other_ns: str,
    other_key: str,
    other_initial_type: EnvValueTypeEnum,
    other_initial_value,
    merge_policy: MergeEnvStorePolicy,
    ref_store,
    merge_trie: Trie,
    merge_prefix: list,
  ):
    """Merge logic for a single env entry"""
    if ref_store is None and other_initial_type == EnvValueTypeEnum.GLOBAL:
      raise ValueError(
        f"'{EnvValueTypeEnum.GLOBAL.value}' is not supported in this section, namespace {other_ns}"
      )

    # skip overwrite if
    store_has_env = self.has_env(other_ns, other_key)

    if store_has_env and merge_policy == MergeEnvStorePolicy.PRESERVE:
      return

    if store_has_env and (
      merge_policy == MergeEnvStorePolicy.ALLOW_LIST
      and (
        not merge_trie.has(merge_prefix + [other_ns, other_key])
        or (
          self._check_is_type_auto_and_frozen(
            other_initial_type, other_initial_value
          )
          and self._initial_type[other_ns][other_key]
          == EnvValueTypeEnum.CONSTANT
        )
      )
    ):
      return

    self._set_env(
      other_ns,
      other_key,
      other_initial_value,
      other_initial_type,
      ref_store,
      allow_update=merge_policy != MergeEnvStorePolicy.PRESERVE,
    )

  def _merge_inplace(
    self,
    to_merge,
    merge_policy: MergeEnvStorePolicy = MergeEnvStorePolicy.PRESERVE,
    ref_store=None,
    merge_trie: Trie = None,
    merge_prefix: list = None,
  ):
    """Merge another env store inside this env store

    Parameters
    ----------
    env_store_to_merge: EnvStore
      The env store to merge in the current
    merge_policy: MergeEnvStorePolicy
      Defines how merging is performed. With PRESERVE, keys of this env_store have precedence over keys of other_env_store
    ref_store: EnvStore
      The store in which to look for global variables
    merge_trie: Trie
      A set containing the keys that should be updated when policy is ALLOW_LIST, default is 'set()'
    merge_prefix: list
      The prefix list to which attach namespace and env name for checking if a value should be inserted when policy is ALLOW_LIST
    """
    for other_ns, other_ns_entries in to_merge._store.items():
      for other_key, _ in other_ns_entries.items():
        other_initial_type = to_merge._initial_type[other_ns][other_key]
        other_initial_value = to_merge._initial_value[other_ns][other_key]
        self._merge_env(
          other_ns,
          other_key,
          other_initial_type,
          other_initial_value,
          merge_policy,
          ref_store,
          merge_trie,
          merge_prefix,
        )

  @staticmethod
  def merge(
    env_store1,
    env_store2,
    merge_policy: MergeEnvStorePolicy = MergeEnvStorePolicy.PRESERVE,
    ref_store=None,
    merge_trie: Trie = None,
    merge_prefix: list = None,
  ):
    """Merge two env stores inside a new env store.

    Parameters
    ----------
    env_store1: EnvStore
    env_store2: EnvStore
    merge_policy: MergeEnvStorePolicy
      Defines how merging is performed. With PRESERVE, keys of the env_store1 have precedence over keys of env_store2
    ref_store: EnvStore
      The store in which to look for global variables
    merge_trie: Trie
      A set containing the keys that should be updated when policy is ALLOW_LIST, default is 'set()'
    merge_prefix: list
      The prefix list to which attach namespace and env name for checking if a value should be inserted when policy is ALLOW_LIST
    Returns
    -------
    env_store: EnvStore
      Merged env store
    """
    new_env_store = EnvStore()
    new_env_store._merge_inplace(
      env_store1, merge_policy=merge_policy, ref_store=ref_store
    )
    new_env_store._merge_inplace(
      env_store2,
      merge_policy=merge_policy,
      ref_store=ref_store,
      merge_trie=merge_trie,
      merge_prefix=merge_prefix,
    )
    return new_env_store
