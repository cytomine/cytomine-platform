import os
import shutil
from tempfile import TemporaryDirectory
import unittest
from cytomine_installer.deployment.deployment_files import ConfigFile
from cytomine_installer.deployment.deployment_folders import (
    DeploymentFolder,
    InvalidServerConfigurationError,
    ServerFolder,
)
from cytomine_installer.deployment.errors import (
    MissingConfigFileError,
    NoDockerComposeYamlFileError,
)
from cytomine_installer.util import list_relative_files
from tests.util import FileSystemTestCase, TestDeploymentGeneric


class TestServerFolder(FileSystemTestCase):
    def testListSourceFiles(self):
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

    def testGeneratedFiles(self):
        tests_path = os.path.dirname(__file__)
        deploy_path = os.path.join(tests_path, "files", "fake_single_server", "in")
        envs_file = ConfigFile(deploy_path)
        server_folder = ServerFolder("default", deploy_path, envs_file)
        self.assertSetEqual(
            set(server_folder.generated_files),
            {"envs/core.env", "envs/ims.env", ".env", "docker-compose.override.yml"},
        )

    def testTargetFiles(self):
        tests_path = os.path.dirname(__file__)
        deploy_path = os.path.join(tests_path, "files", "fake_single_server", "in")
        envs_file = ConfigFile(deploy_path)
        server_folder = ServerFolder("default", deploy_path, envs_file)
        self.assertSetEqual(
            set(server_folder.target_files),
            set(server_folder.source_files).union(server_folder.generated_files),
        )

    def testFilesFunctionsOneServiceWithoutEnvs(self):
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

    def testCleanValid(self):
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
    def testSingleServerDeployment(self):
        tests_path = os.path.dirname(__file__)
        deploy_path = os.path.join(tests_path, "files", "fake_single_server", "in")
        output_ref_path = os.path.join(tests_path, "files", "fake_single_server", "out")
        deployment_folder = DeploymentFolder(directory=deploy_path)
        with TemporaryDirectory() as tmpdir:
            deployment_folder.deploy_files(tmpdir)
            self.check_single_server_deployment(output_ref_path, tmpdir)

    @unittest.skip("implement later")
    def testMultiServerConfiguration(self):
        tests_path = os.path.dirname(__file__)
        deploy_path = os.path.join(tests_path, "files", "fake_multi_server", "in")
        output_ref_path = os.path.join(tests_path, "files", "fake_multi_server", "out")
        deployment_folder = DeploymentFolder(directory=deploy_path)
        with TemporaryDirectory() as tmpdir:
            deployment_folder.deploy_files(tmpdir)
            self.assertSameDirectories(output_ref_path, tmpdir)

    def testMultiServerMissingServerFolder(self):
        tests_path = os.path.dirname(__file__)
        deploy_path = os.path.join(
            tests_path, "files", "fake_multi_server_missing_folder"
        )
        with self.assertRaises(InvalidServerConfigurationError):
            DeploymentFolder(directory=deploy_path)

    def testNoCytomineYml(self):
        tests_path = os.path.dirname(__file__)
        deploy_path = os.path.join(tests_path, "files", "fake_no_cytomine_yml")
        with self.assertRaises(FileNotFoundError):
            DeploymentFolder(directory=deploy_path)

    def testNoDockerComposeFile(self):
        tests_path = os.path.dirname(__file__)
        deploy_path = os.path.join(tests_path, "files", "fake_no_docker_compose_yml")
        with self.assertRaises(InvalidServerConfigurationError):
            DeploymentFolder(directory=deploy_path)
