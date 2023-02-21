import enum
import json
import os
import yaml
from collections import defaultdict

from .errors import (
    MissingCytomineYamlFileError,
    NoDockerComposeYamlFileError,
    UnknownCytomineEnvSection,
    UnknownServiceError,
)
from .enums import CytomineEnvSectionEnum
from .env_store import DictExportable, EnvStore

DOCKER_COMPOSE_FILENAME = "docker-compose.yml"
DOCKER_COMPOSE_OVERRIDE_FILENAME = "docker-compose.override.yml"


class UnknownServerError(ValueError):
    def __init__(self, server, *args: object) -> None:
        super().__init__(f"unknown server '{server}'", *args)


class CytomineEnvsFile(DictExportable):
    """parses a cytomine.yml file"""

    def __init__(self, path, filename="cytomine.yml") -> None:
        self._filename = filename
        self._path = path

        if not os.path.isfile(self.filepath):
            raise MissingCytomineYamlFileError(path, filename)

        with open(self.filepath, "r", encoding="utf8") as file:
            self._raw_config = yaml.load(file, Loader=yaml.Loader)

        # both top-level sections are optional
        for section in self._raw_config.keys():
            try:
                CytomineEnvSectionEnum(section)
            except ValueError:
                raise UnknownCytomineEnvSection(section)

        self._global_envs = EnvStore()
        for ns, entries in self._raw_config.get(
            CytomineEnvSectionEnum.GLOBAL.value, {}
        ).items():
            self._global_envs.add_namespace(ns, entries)

        self._servers_env_stores = defaultdict(lambda: EnvStore())
        for server, envs in self._raw_config.get(
            CytomineEnvSectionEnum.SERVICES.value, {}
        ).items():
            for ns, entries in envs.items():
                self._servers_env_stores[server].add_namespace(
                    ns, entries, store=self._global_envs
                )

    @property
    def filename(self):
        return self._filename

    @property
    def path(self):
        return self._path

    @property
    def filepath(self):
        return os.path.join(self.path, self.filename)

    @property
    def global_envs(self):
        return self._global_envs

    @property
    def servers(self):
        return list(self._servers_env_stores.keys())

    def services(self, server: str):
        """Returns the list of services for a given server"""
        if server not in self._servers_env_stores:
            raise UnknownServerError(server)
        return list(self._servers_env_stores[server].keys())

    def server_store(self, server: str):
        """Returns the env store for a given server"""
        if server not in self._servers_env_stores:
            raise UnknownServerError(server)
        return self._servers_env_stores.get(server, None)

    def export_dict(self):
        target_dict = dict()
        target_dict["global"] = self._global_envs.export_dict()
        target_dict["services"] = dict()
        for server, env_store in self._servers_env_stores.items():
            target_dict["services"][server] = env_store.export_dict()
        # https://stackoverflow.com/a/32303615
        # convert to plain dict
        return json.loads(json.dumps(target_dict))


class DockerComposeFile:
    """light parsing of docker compose files"""

    def __init__(self, path, filename=DOCKER_COMPOSE_FILENAME) -> None:
        self._path = path
        self._filename = filename

        if not os.path.isfile(self.filepath):
            raise NoDockerComposeYamlFileError(self._path)

        with open(self.filepath, "r", encoding="utf8") as file:
            self._content = yaml.load(file, Loader=yaml.Loader)

    @property
    def filepath(self):
        return os.path.join(self._path, self._filename)

    @property
    def filename(self):
        return self._filename

    @property
    def services(self):
        return list(self._content.get("services", {}).keys())

    @property
    def version(self):
        return self._content.get("version")


class EditableDockerCompose:
    """A class for creating and changing a docker compose (intentionally very limited scope).
    Supports edition of:
    - service 'env_file'
    - service 'volumes'
    """

    def __init__(self, version="3.9") -> None:
        self._compose = dict()
        self._compose["services"] = {}
        self._compose["version"] = version

    def _get_service_dict(self, service):
        if service not in self._compose["services"]:
            self._compose["services"][service] = {}
        return self._compose["services"][service]

    def set_service_env_file(self, service, filepath):
        self._get_service_dict(service)["env_file"] = filepath

    def get_service_volumes(self, service):
        if service not in self._compose["services"]:
            raise UnknownServiceError(service)
        return self._compose["services"][service]["volumes"]

    def add_service_volume(self, service, mapping):
        service_dict = self._get_service_dict(service)
        if "volumes" not in service_dict:
            self._compose["services"][service]["volumes"] = list()
        self._compose["services"][service]["volumes"].append(mapping)

    def clear_service_volumes(self, service):
        if (
            service in self._compose["services"]
            and "volumes" in self._compose["services"][service]
        ):
            del self._compose["services"][service]["volumes"]

    def write_to(self, path, filename="docker-compose.yml"):
        filepath = os.path.join(path, filename)
        with open(filepath, "w", encoding="utf8") as file:
            yaml.dump(self._compose, file)
