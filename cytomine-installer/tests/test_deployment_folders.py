import os
import shutil
import unittest
from tempfile import TemporaryDirectory

from cytomine_installer.deployment.deployment_files import ConfigFile
from cytomine_installer.deployment.deployment_folders import (
    DeploymentFolder, InvalidServerConfigurationError, ServerFolder)
from cytomine_installer.util import list_relative_files
from tests.util import FileSystemTestCase, TestDeploymentGeneric


class TestServerFolder(FileSystemTestCase):
  def test_list_source_files(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_single_server", "in")
    envs_file = ConfigFile(deploy_path)
    server_folder = ServerFolder("default", deploy_path, envs_file)
    self.assertSetEqual(
      set(server_folder.source_files),
      {
        "configs/core/etc/cytomine/cytomine-app.yml",
        "configs/ims/usr/local/cytom/ims.conf",
        "docker-compose.yml",
      },
    )

  def test_generated_files(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_single_server", "in")
    envs_file = ConfigFile(deploy_path)
    server_folder = ServerFolder("default", deploy_path, envs_file)
    self.assertSetEqual(
      set(server_folder.generated_files),
      {"envs/core.env", "envs/ims.env", ".env", "docker-compose.override.yml"},
    )

  def test_target_files(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_single_server", "in")
    envs_file = ConfigFile(deploy_path)
    server_folder = ServerFolder("default", deploy_path, envs_file)
    self.assertSetEqual(
      set(server_folder.target_files),
      set(server_folder.source_files).union(server_folder.generated_files),
    )

  def test_files_functions_one_service_without_envs(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_multi_server", "in")
    server_path = os.path.join(deploy_path, "server-core")
    envs_file = ConfigFile(deploy_path)
    server_folder = ServerFolder("server-core", server_path, envs_file)
    self.assertSetEqual(
      set(server_folder.source_files),
      {"configs/core/etc/cytomine/cytomine-app.yml", "docker-compose.yml"},
    )
    self.assertSetEqual(
      set(server_folder.generated_files),
      {
        "envs/core.env",
        "envs/postgres.env",
        ".env",
        "docker-compose.override.yml",
      },
    )

  def test_clean_valid(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_multi_server", "in")
    server_path = os.path.join(deploy_path, "server-core")
    out_deploy_path = os.path.join(tests_path, "files", "fake_multi_server", "out")
    out_server_path = os.path.join(out_deploy_path, "server-core")
    envs_file = ConfigFile(deploy_path)
    server_folder = ServerFolder("server-core", server_path, envs_file)
    with TemporaryDirectory() as tmpdir:
      target_server_path = os.path.join(tmpdir, "out")
      shutil.copytree(out_server_path, target_server_path)
      self.assertSetEqual(
        set(list_relative_files(target_server_path)),
        set(server_folder.target_files),
      )
      server_folder.clean_generated_files(target_server_path)
      self.assertSetEqual(
        set(list_relative_files(target_server_path)),
        set(server_folder.source_files),
      )


class TestDeploymentFolder(TestDeploymentGeneric):
  def test_single_server_deployment(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_single_server", "in")
    output_ref_path = os.path.join(tests_path, "files", "fake_single_server", "out")
    deployment_folder = DeploymentFolder(directory=deploy_path)
    with TemporaryDirectory() as tmpdir:
      deployment_folder.deploy_files(tmpdir)
      self.check_single_server_deployment(output_ref_path, tmpdir)

  @unittest.skip("implement later")
  def test_multi_server_configuration(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_multi_server", "in")
    output_ref_path = os.path.join(tests_path, "files", "fake_multi_server", "out")
    deployment_folder = DeploymentFolder(directory=deploy_path)
    with TemporaryDirectory() as tmpdir:
      deployment_folder.deploy_files(tmpdir)
      self.assert_same_directories(output_ref_path, tmpdir)

  def test_multi_server_missing_server_folder(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(
      tests_path, "files", "fake_multi_server_missing_folder"
    )
    with self.assertRaises(InvalidServerConfigurationError):
      DeploymentFolder(directory=deploy_path)

  def test_no_cytomine_yml(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_no_cytomine_yml")
    with self.assertRaises(FileNotFoundError):
      DeploymentFolder(directory=deploy_path)

  def test_no_docker_compose_file(self):
    tests_path = os.path.dirname(__file__)
    deploy_path = os.path.join(tests_path, "files", "fake_no_docker_compose_yml")
    with self.assertRaises(InvalidServerConfigurationError):
      DeploymentFolder(directory=deploy_path)
