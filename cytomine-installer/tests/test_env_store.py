

from bootstrapper.env_store import EnvStore, KeyAlreadyExistsError
from unittest import TestCase


class TestConstantEnvStore(TestCase):
  def testConstantEnvStore(self):
    namespaces = {
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

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries)
    
    for ns, entries in namespaces.items():
      for env_name, env_value in entries["constant"].items():
        self.assertEqual(env_store.get_value(ns, env_name), env_value)


class TestGlobalEnvStore(TestCase):
  def setUp(self):
    self._global = EnvStore()
    self._global.add_namespace("base", {
      "constant": {
        "VAR1": "gvalue1",
        "VAR3": "gvalue2"
      }
    })
    self._global.add_namespace("advanced", {
      "constant": {
        "GVAR1": "gvalue3",
        "GVAR2": "gvalue4"
      }
    })


  def testAlreadyExists(self):
    namespace =  {
      "global": { "VAR1": "base.VAR1" },
      "constant": { "VAR1": "cst_value" }
    }

    with self.assertRaises(KeyAlreadyExistsError):
      env_store = EnvStore()
      env_store.add_namespace("ns1", namespace, store=self._global)

  def testGlobalEnvStore(self):
    namespaces = {
      "ns1": {
        "global": {
          "VAR1": "base.VAR1",
          "VAR9": "advanced.GVAR1"
        },
        "constant": {
          "VAR3": "value"
        }
      },
      "ns2": {
        "global": {
          "VAR2": "base.VAR3",
          "VAR8": "advanced.GVAR2"
        },
        "constant": {
          "VAR4": "value"
        }
      }
    }

    env_store = EnvStore()
    for ns, entries in namespaces.items():
      env_store.add_namespace(ns, entries, store=self._global)

    for ns, entries in namespaces.items():
      for env_name, env_value in entries["global"].items():
        self.assertEqual(env_store.get_value(ns, env_name), self._global.get_value(*env_value.split(".")))
      for env_name, env_value in entries["constant"].items():
        self.assertEqual(env_store.get_value(ns, env_name), env_value)
