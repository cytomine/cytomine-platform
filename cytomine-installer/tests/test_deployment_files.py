import os
from unittest import TestCase

from bootstrapper.env_store import UnknownValueTypeError
from bootstrapper.deployment_files import DockerComposeFile, CytomineEnvsFile, UnknownCytomineEnvSection
from tests.util import UUID_PATTERN


class TestDockerComposeFile(TestCase):
  def testValidFile(self):
    tests_path = os.path.dirname(__file__)
    dc_path = os.path.join(tests_path, "files")
    dc_filename =  "docker_compose.valid.yml"
    docker_compose_file = DockerComposeFile(dc_path, filename=dc_filename)
    services = {"nginx", "core", "postgresql", "mongodb", "rabbitmq", "memcached", "pims"}
    self.assertSetEqual(services, set(docker_compose_file.services))  
    self.assertEqual(docker_compose_file.version, "3.4")


class TestCytomineEnvsFile(TestCase):
  def testMinimalFile(self):
    tests_path = os.path.dirname(__file__)
    ce_path = os.path.join(tests_path, "files")
    ce_filename = "cytomine.mini.yml"
    cytomine_envs_file = CytomineEnvsFile(ce_path, filename=ce_filename)
    self.assertEqual(cytomine_envs_file.global_envs.get_value("namespace1", "VAR1"), "value1")
    self.assertEqual(cytomine_envs_file.global_envs.get_value("namespace1", "VAR2"), "value2")
    self.assertRegex(cytomine_envs_file.global_envs.get_value("namespace2", "KEY1"), UUID_PATTERN)
    self.assertRegex(cytomine_envs_file.global_envs.get_value("namespace2", "KEY2"), UUID_PATTERN)

    self.assertEqual(len(cytomine_envs_file.servers), 1)
    self.assertEqual(cytomine_envs_file.servers[0], "default")
    default_store = cytomine_envs_file.server_store("default")
    self.assertEqual(default_store.get_value("core", "EMAIL"), "emailcore")
    self.assertEqual(default_store.get_value("core", "VAR1"), cytomine_envs_file.global_envs.get_value("namespace1", "VAR1"))
    self.assertRegex(default_store.get_value("core", "GENERATED"), UUID_PATTERN)
    self.assertEqual(default_store.get_value("rabbitmq", "VAR1"), cytomine_envs_file.global_envs.get_value("namespace1", "VAR1"))
    self.assertEqual(default_store.get_value("rabbitmq", "VAR2"), cytomine_envs_file.global_envs.get_value("namespace2", "KEY1"))

  def testFileWithInvalidValueType(self):
    tests_path = os.path.dirname(__file__)
    ce_path = os.path.join(tests_path, "files")
    ce_filename = "cytomine.unknown-value-type.yml"
    with self.assertRaises(UnknownValueTypeError):
      CytomineEnvsFile(ce_path, filename=ce_filename)

  def testFileWithInvalidTopLevelSection(self):
    tests_path = os.path.dirname(__file__)
    ce_path = os.path.join(tests_path, "files")
    ce_filename = "cytomine.invalid-top-level-sections.yml"
    with self.assertRaises(UnknownCytomineEnvSection):
      CytomineEnvsFile(ce_path, filename=ce_filename)