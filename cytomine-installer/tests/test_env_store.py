from cytomine_installer.deployment.env_store import EnvStore, KeyAlreadyExistsError
from unittest import TestCase

from tests.util import UUID_PATTERN


def phony_namespaces_constant():
  return {
    "ns1": {"constant": {"VAR1": "value1", "VAR2": "value2"}},
    "ns2": {"constant": {"VAR1": "value3", "VAR3": "value4"}},
  }


def phony_namespaces_and_global():
  nss = phony_namespaces_constant()

  nss["ns1"]["global"] = {"VAR3": "base.VAR1", "VAR9": "advanced.GVAR1"}

  nss["ns2"]["global"] = {"VAR2": "base.VAR3", "VAR8": "advanced.GVAR2"}

  env_store = EnvStore()
  env_store.add_namespace(
    "base", {"constant": {"VAR1": "gvalue1", "VAR3": "gvalue2"}}
  )
  env_store.add_namespace(
    "advanced", {"constant": {"GVAR1": "gvalue3", "GVAR2": "gvalue4"}}
  )

  return env_store, nss


def phony_namespaces_all():
  env_store, nss = phony_namespaces_and_global()
  nss["ns1"]["auto"] = {
    "VARAUTO1": "random_uuid",
    "VARAUTO2": {"type": "random_uuid", "freeze": False},
  }
  nss["ns2"]["auto"] = {
    "VARAUTO3": "random_uuid",
    "VARAUTO4": {"type": "openssl", "base64": True, "length": 8, "freeze": True},
  }
  return env_store, nss


class TestEnvStore(TestCase):
  def setUp(self):
    self._global, _ = phony_namespaces_and_global()

  def testValidEnvStoreWithConstantOnly(self):
    namespaces = phony_namespaces_constant()

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries)

    for ns, entries in namespaces.items():
      for env_name, env_value in entries["constant"].items():
        self.assertEqual(env_store.get_env(ns, env_name), env_value)

    for ns in env_store.namespaces:
      self.assertTrue(env_store.has_namespace(ns))

    self.assertTrue(env_store.has_namespace("ns1"))
    self.assertTrue(env_store.has_namespace("ns2"))
    self.assertFalse(env_store.has_namespace("ns3"))
    self.assertFalse(env_store.has_namespace("other"))

    self.assertDictEqual(
      namespaces["ns1"]["constant"], env_store.get_namespace_envs("ns1")
    )

  def testEnvVarAlreadyExists(self):
    namespace = {"global": {"VAR1": "base.VAR1"}, "constant": {"VAR1": "cst_value"}}

    with self.assertRaises(KeyAlreadyExistsError):
      env_store = EnvStore()
      env_store.add_namespace("ns1", namespace, store=self._global)

  def testValidEnvStoreWithGlobal(self):
    _, namespaces = phony_namespaces_and_global()

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries, store=self._global)

    for ns, entries in namespaces.items():
      for env_name, env_value in entries["global"].items():
        self.assertEqual(
          env_store.get_env(ns, env_name),
          self._global.get_env(*env_value.split(".")),
        )
      for env_name, env_value in entries["constant"].items():
        self.assertEqual(env_store.get_env(ns, env_name), env_value)

    self.assertDictEqual(
      {**namespaces["ns1"]["constant"], "VAR3": "gvalue1", "VAR9": "gvalue3"},
      env_store.get_namespace_envs("ns1"),
    )

  def testValidEnvStoreWithAuto(self):
    _, namespaces = phony_namespaces_all()

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries, store=self._global)

    self.assertRegex(env_store.get_env("ns1", "VARAUTO1"), UUID_PATTERN)
    self.assertRegex(env_store.get_env("ns1", "VARAUTO2"), UUID_PATTERN)
    self.assertRegex(env_store.get_env("ns2", "VARAUTO3"), UUID_PATTERN)
    self.assertRegex(env_store.get_env("ns2", "VARAUTO4"), r".+")

  def testExportDict(self):
    _, namespaces = phony_namespaces_all()

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries, store=self._global)

    generated_dict = env_store.export_dict()

    for ns, entries in namespaces.items():
      self.assertIn(ns, generated_dict)
      for env_type in ["constant", "global"]:
        for k, v in entries.get(env_type).items():
          self.assertIn(k, generated_dict[ns][env_type])
          self.assertEqual(generated_dict[ns][env_type][k], v)
      for k, v in entries.get("auto").items():
        if not isinstance(v, dict) or "freeze" not in v or v["freeze"]:
          self.assertIn(k, generated_dict[ns]["constant"])
        else:
          self.assertIn(k, generated_dict[ns]["auto"])
          self.assertEqual(generated_dict[ns]["auto"][k], v)

  def testFreezeOmittedInAutogenerate(self):
    namespaces = {"ns1": {"auto": {"VAR1": {"type": "openssl"}}}}
    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries, store=self._global)

    generated_dict = env_store.export_dict()

    self.assertRegex(generated_dict["ns1"]["constant"]["VAR1"], r".+")

  def testMergeSameNamespaceNewVar(self):
    nss1 = {"ns1": {"auto": {"VAR1": {"type": "openssl"}}}}
    nss2 = {"ns1": {"auto": {"VAR2": {"type": "openssl"}}}}
    env_store1 = EnvStore()
    for ns, entries in nss1.items():
      env_store1.add_namespace(ns, entries, store=self._global)
    env_store2 = EnvStore()
    for ns, entries in nss2.items():
      env_store2.add_namespace(ns, entries, store=self._global)

    # merge
    env_store3 = EnvStore.merge(env_store1, env_store2)

    generated_dict3 = env_store3.export_dict()
    self.assertRegex(generated_dict3["ns1"]["constant"]["VAR1"], r".+")
    self.assertRegex(generated_dict3["ns1"]["constant"]["VAR2"], r".+")


  def testMergeSameNamespaceSameVar(self):
    nss1 = {"ns1": {"constant": {"VAR1": "varvalue"}}}
    nss2 = {"ns1": {"constant": {"VAR1": "varvalue2"}}}
    env_store1 = EnvStore()
    for ns, entries in nss1.items():
      env_store1.add_namespace(ns, entries, store=self._global)
    env_store2 = EnvStore()
    for ns, entries in nss2.items():
      env_store2.add_namespace(ns, entries, store=self._global)

    # merge
    env_store3 = EnvStore.merge(env_store1, env_store2)
    generated_dict1 = env_store1.export_dict()
    generated_dict2 = env_store2.export_dict()
    generated_dict3 = env_store3.export_dict()

    self.assertEqual(generated_dict1["ns1"]["constant"]["VAR1"], generated_dict3["ns1"]["constant"]["VAR1"])
    self.assertNotEqual(generated_dict2["ns1"]["constant"]["VAR1"], generated_dict3["ns1"]["constant"]["VAR1"])