from abc import ABC, abstractmethod
import enum
from collections import defaultdict
import json
import logging

from .errors import KeyAlreadyExistsError, UnknownValueTypeError
from .env_generator import EnvValueGeneratorFactory


class EnvValueTypeEnum(enum.Enum):
    CONSTANT = "constant"
    GLOBAL = "global"
    AUTOGENERATE = "auto"


class MergeEnvStorePolicy(enum.Enum):
    PRESERVE = "preserve_target"  # merge by preserving variables inside the merge target envstore


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
        self._store = defaultdict(dict)
        self._initial_type = defaultdict(dict)
        self._initial_value = defaultdict(dict)

    def has_env(self, namespace: str, key: str):
        return namespace in self._store and key in self._store[namespace]

    def _add_env(
        self, ns: str, key: str, value, store_value_fn, _type: EnvValueTypeEnum
    ):
        """Register an envirom"""
        if key in self._store[ns]:
            raise KeyAlreadyExistsError(ns, key)
        self._store[ns][key] = store_value_fn()
        self._initial_type[ns][key] = _type
        self._initial_value[ns][key] = value

    def add_namespace(self, ns: str, entries: dict, store=None):
        if ns in self._store:
            raise ValueError(f"namespace '{ns}' already exists in env store")

        # check for unknown value-type
        for value_type in entries.keys():
            try:
                EnvValueTypeEnum(value_type)
            except ValueError:
                raise UnknownValueTypeError(value_type)

        for k, v in entries.get(EnvValueTypeEnum.CONSTANT.value, {}).items():
            self._add_env(ns, k, v, lambda: v, EnvValueTypeEnum.CONSTANT)

        if (
            EnvValueTypeEnum.GLOBAL.value in entries
            and len(entries[EnvValueTypeEnum.GLOBAL.value]) > 0
            and store is None
        ):
            raise ValueError("'global' is not supported in this section")

        for k, v in entries.get(EnvValueTypeEnum.GLOBAL.value, {}).items():
            other_ns, other_key = v.split(".")
            self._add_env(
                ns,
                k,
                v,
                lambda: store.get_env(other_ns, other_key),
                EnvValueTypeEnum.GLOBAL,
            )

        gen_factory = EnvValueGeneratorFactory()
        for k, v in entries.get(EnvValueTypeEnum.AUTOGENERATE.value, {}).items():
            generator = gen_factory.make_generator(v)
            self._add_env(
                ns, k, v, lambda: generator.resolve(v), EnvValueTypeEnum.AUTOGENERATE
            )

    @property
    def namespaces(self):
        return list(self._store.keys())

    def get_env(self, ns: str, key: str):
        if ns not in self._store:
            raise ValueError(f"unknown namespace '{ns}'")
        if key not in self._store[ns]:
            raise ValueError(f"unknown key '{key}' in namespace '{ns}'")
        return self._store[ns][key]

    def export_dict(self):
        output_dict = defaultdict(lambda: defaultdict(dict))
        for ns in self.namespaces:
            for key in self._store[ns].keys():
                init_type, init_value = (
                    self._initial_type[ns][key],
                    self._initial_value[ns][key],
                )
                if init_type == EnvValueTypeEnum.AUTOGENERATE and (
                    isinstance(init_value, str) or init_value.get("freeze", True)
                ):
                    output_dict[ns][EnvValueTypeEnum.CONSTANT.value][key] = self._store[
                        ns
                    ][key]
                else:
                    output_dict[ns][init_type.value][key] = self._initial_value[ns][key]
        if len(output_dict) == 0:
            return None
        else:
            # https://stackoverflow.com/a/32303615
            # convert to plain dict
            return json.loads(json.dumps(output_dict))

    def get_namespace_envs(self, ns: str):
        if ns not in self._store:
            raise ValueError(f"unknown namespace '{ns}'")
        return {**self._store[ns]}

    def has_namespace(self, ns: str):
        return ns in self._store

    def _merge_inplace(
        self,
        other_env_store,
        merge_policy: MergeEnvStorePolicy = MergeEnvStorePolicy.PRESERVE,
    ):
        """Merge another env store inside a this  env store

        Parameters
        ----------
        other_env_store: EnvStore
        merge_policy: MergeEnvStorePolicy
            Defines how merging is performed. With PRESERVE, keys of this env_store have precedence over keys of other_env_store

        """
        for other_ns, other_ns_entries in other_env_store._store.items():
            for other_key, other_value in other_ns_entries.items():
                other_initial_type = other_env_store._initial_type[other_ns][other_key]
                other_initial_value = other_env_store._initial_value[other_ns][
                    other_key
                ]
                if merge_policy == MergeEnvStorePolicy.PRESERVE:
                    if self.has_env(other_ns, other_key):
                        continue  # if exists in current env store, just ignore key
                    self._store[other_ns][other_key] = other_value
                    self._initial_type[other_ns][other_key] = other_initial_type
                    self._initial_value[other_ns][other_key] = other_initial_value
                else:
                    raise ValueError(f"unknown merge policy '{merge_policy}'")

    @staticmethod
    def merge(
        env_store1,
        env_store2,
        merge_policy: MergeEnvStorePolicy = MergeEnvStorePolicy.PRESERVE,
    ):
        """Merge another env store inside a new env store.

        Parameters
        ----------
        env_store1: EnvStore
        env_store2: EnvStore
        merge_policy: MergeEnvStorePolicy
            Defines how merging is performed. With PRESERVE, keys of the env_store1 have precedence over keys of env_store2

        Returns
        -------
        env_store: EnvStore
            Merged env store
        """
        new_env_store = EnvStore()
        new_env_store._merge_inplace(env_store1, merge_policy=merge_policy)
        new_env_store._merge_inplace(env_store2, merge_policy=merge_policy)
        return new_env_store
