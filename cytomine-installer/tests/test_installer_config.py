


import os
from unittest import TestCase

from cytomine_installer.deployment.installer_config import InstallerConfig, UpdatePolicy


class TestInstallerConfig(TestCase):
  def testDefaultConfig(self):
    config = InstallerConfig()

    self.assertEqual(config.update_policy, UpdatePolicy.UPDATE_NEW_ONLY)
    self.assertListEqual(config.update_allow_list, list())

  def testFakeConfig(self):
    tests_path = os.path.dirname(__file__)
    test_files_path = os.path.join(tests_path, "files")
    config = InstallerConfig(os.path.join(test_files_path, "fake_installer_config.yml"))

    self.assertEqual(config.update_policy, UpdatePolicy.UPDATE_NEW_ONLY)
    self.assertListEqual(config.update_allow_list, ["global.*"])