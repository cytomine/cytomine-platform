import os
import yaml
from tempfile import TemporaryDirectory
from unittest import TestCase

from cytomine_installer.deployment.env_store import UnknownValueTypeError
from cytomine_installer.deployment.deployment_files import (
    DockerComposeFile,
    ConfigFile,
    EditableDockerCompose,
    UnknownCytomineEnvSection,
)
from tests.util import UUID_PATTERN


class TestDockerComposeFile(TestCase):
    def testValidFile(self):
        tests_path = os.path.dirname(__file__)
        dc_path = os.path.join(tests_path, "files")
        dc_filename = "docker_compose.valid.yml"
        docker_compose_file = DockerComposeFile(dc_path, filename=dc_filename)
        services = {
            "nginx",
            "core",
            "postgresql",
            "mongodb",
            "rabbitmq",
            "memcached",
            "pims",
        }
        self.assertSetEqual(services, set(docker_compose_file.services))
        self.assertEqual(docker_compose_file.version, "3.4")


class TestCytomineEnvsFile(TestCase):
    def testMinimalFile(self):
        tests_path = os.path.dirname(__file__)
        ce_path = os.path.join(tests_path, "files")
        ce_filename = "cytomine.mini.yml"
        cytomine_envs_file = ConfigFile(ce_path, filename=ce_filename)
        self.assertEqual(cytomine_envs_file.path, ce_path)
        self.assertEqual(cytomine_envs_file.filename, ce_filename)
        self.assertEqual(
            cytomine_envs_file.filepath, os.path.join(ce_path, ce_filename)
        )
        self.assertEqual(
            cytomine_envs_file.global_envs.get_env("namespace1", "VAR1"), "value1"
        )
        self.assertEqual(
            cytomine_envs_file.global_envs.get_env("namespace1", "VAR2"), "value2"
        )
        self.assertRegex(
            cytomine_envs_file.global_envs.get_env("namespace2", "KEY1"), UUID_PATTERN
        )
        self.assertRegex(
            cytomine_envs_file.global_envs.get_env("namespace2", "KEY2"), UUID_PATTERN
        )

        self.assertEqual(len(cytomine_envs_file.servers), 1)
        self.assertEqual(cytomine_envs_file.servers[0], "default")
        default_store = cytomine_envs_file.server_store("default")
        self.assertEqual(default_store.get_env("core", "EMAIL"), "emailcore")
        self.assertEqual(
            default_store.get_env("core", "VAR1"),
            cytomine_envs_file.global_envs.get_env("namespace1", "VAR1"),
        )
        self.assertRegex(default_store.get_env("core", "GENERATED"), UUID_PATTERN)
        self.assertEqual(
            default_store.get_env("rabbitmq", "VAR1"),
            cytomine_envs_file.global_envs.get_env("namespace1", "VAR1"),
        )
        self.assertEqual(
            default_store.get_env("rabbitmq", "VAR2"),
            cytomine_envs_file.global_envs.get_env("namespace2", "KEY1"),
        )

    def testFileWithInvalidValueType(self):
        tests_path = os.path.dirname(__file__)
        ce_path = os.path.join(tests_path, "files")
        ce_filename = "cytomine.unknown-value-type.yml"
        with self.assertRaises(UnknownValueTypeError):
            ConfigFile(ce_path, filename=ce_filename)

    def testFileWithInvalidTopLevelSection(self):
        tests_path = os.path.dirname(__file__)
        ce_path = os.path.join(tests_path, "files")
        ce_filename = "cytomine.invalid-top-level-sections.yml"
        with self.assertRaises(UnknownCytomineEnvSection):
            ConfigFile(ce_path, filename=ce_filename)


class TestEditableDockerCompose(TestCase):
    def testEmpty(self):
        dc_version = "3.8"
        edc = EditableDockerCompose(dc_version)

        with TemporaryDirectory() as tmpdir:
            edc.write_to(tmpdir)
            with open(
                os.path.join(tmpdir, "docker-compose.yml"), "r", encoding="utf8"
            ) as file:
                edc_content = yaml.load(file, Loader=yaml.Loader)

        self.assertDictEqual({"version": dc_version, "services": {}}, edc_content)

    def testWithEnvFileInOneService(self):
        dc_version = "3.8"
        edc = EditableDockerCompose(dc_version)
        service = "myservice"
        env_file_path = "/my/path"
        edc.set_service_env_file(service, env_file_path)

        with TemporaryDirectory() as tmpdir:
            edc.write_to(tmpdir)
            with open(
                os.path.join(tmpdir, "docker-compose.yml"), "r", encoding="utf8"
            ) as file:
                edc_content = yaml.load(file, Loader=yaml.Loader)

        self.assertDictEqual(
            {"version": dc_version, "services": {service: {"env_file": env_file_path}}},
            edc_content,
        )

    def testWithVolumes(self):
        dc_version = "3.8"
        edc = EditableDockerCompose(dc_version)
        service = "myservice"
        volumes = ["a:b", "c:d"]
        for volume in volumes:
            edc.add_service_volume(service, volume)

        with TemporaryDirectory() as tmpdir:
            edc.write_to(tmpdir)
            with open(
                os.path.join(tmpdir, "docker-compose.yml"), "r", encoding="utf8"
            ) as file:
                edc_content = yaml.load(file, Loader=yaml.Loader)

        self.assertDictEqual(
            {"version": dc_version, "services": {service: {"volumes": volumes}}},
            edc_content,
        )
