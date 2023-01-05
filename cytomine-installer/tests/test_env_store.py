from bootstrapper.env_store import EnvStore, KeyAlreadyExistsError
from unittest import TestCase


def phony_namespaces_constant():
  return {
    "ns1": {
      "constant": {
        "VAR1": "value1",
        "VAR2": "value2"
      }
    },
    "ns2": {
      "constant": {
        "VAR1": "value3",
        "VAR3": "value4"
      }
    }
  }


def phony_namespaces_and_global():
  nss = phony_namespaces_constant()
  
  nss["ns1"]["global"] = {
    "VAR3": "base.VAR1",
    "VAR9": "advanced.GVAR1"
  }

  nss["ns2"]["global"] = {
    "VAR2": "base.VAR3",
    "VAR8": "advanced.GVAR2"
  }

  env_store = EnvStore()
  env_store.add_namespace("base", {
    "constant": {
      "VAR1": "gvalue1",
      "VAR3": "gvalue2"
    }
  })
  env_store.add_namespace("advanced", {
    "constant": {
      "GVAR1": "gvalue3",
      "GVAR2": "gvalue4"
    }
  })
  
  return env_store, nss


def phony_namespaces_all():
  env_store, nss = phony_namespaces_and_global()
  nss["ns1"]["auto"] = {
    "VARAUTO1": "random_uuid",
    "VARAUTO2": {
      "type": "random_uuid",
      "freeze": False
    }
  } 
  nss["ns2"]["auto"] = {
    "VARAUTO3": "random_uuid",
    "VARAUTO4": {
      "type": "openssl",
      "base64": True,
      "length": 8,
      "freeze": True
    }
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
        self.assertEqual(env_store.get_value(ns, env_name), env_value)

  def testEnvVarAlreadyExists(self):
    namespace =  {
      "global": { "VAR1": "base.VAR1" },
      "constant": { "VAR1": "cst_value" }
    }

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
        self.assertEqual(env_store.get_value(ns, env_name), self._global.get_value(*env_value.split(".")))
      for env_name, env_value in entries["constant"].items():
        self.assertEqual(env_store.get_value(ns, env_name), env_value)

  def testValidEnvStoreWithAuto(self):
    _, namespaces = phony_namespaces_all()

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries, store=self._global)
  
    uuid_pattern = r"[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"
    self.assertRegex(env_store.get_value("ns1", "VARAUTO1"), uuid_pattern)
    self.assertRegex(env_store.get_value("ns1", "VARAUTO2"), uuid_pattern)
    self.assertRegex(env_store.get_value("ns2", "VARAUTO3"), uuid_pattern)
    self.assertRegex(env_store.get_value("ns2", "VARAUTO4"), r".+")
  
  def testAsDict(self):
    _, namespaces = phony_namespaces_all()

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries, store=self._global)
    
    generated_dict = env_store.as_dict()
    
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
 
          
      