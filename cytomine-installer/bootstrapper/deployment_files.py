import enum
import os
import yaml
from collections import defaultdict
from .env_store import EnvStore

DOCKER_COMPOSE_FILENAME = "docker-compose.yml"


class UnknownServerError(ValueError):
  def __init__(self, server, *args: object) -> None:
    super().__init__(f"unknown server '{server}'", *args)


class CytomineEnvSectionEnum(enum.Enum):
  GLOBAL = "global"
  SERVICES = "services"


class UnknownCytomineEnvSection(ValueError):
  def __init__(self, section, *args: object) -> None:
    available_values = ', '.join(list(map(lambda v: v.value, CytomineEnvSectionEnum)))
    super().__init__(f"unknown section '{section}', expects one of {{{available_values}}}", *args)


class CytomineEnvsFile:
  """parses a cytomine.yml file"""
  def __init__(self, path, filename="cytomine.yml") -> None:
    self._config_path = os.path.join(path, filename)
    
    with open(self._config_path, "r", encoding="utf8") as file:
      self._raw_config = yaml.load(file)

    # both top-level sections are optional
    for section in self._raw_config.keys():
      try:
        CytomineEnvSectionEnum(section)
      except ValueError:
        raise UnknownCytomineEnvSection(section)
    
    self._global_envs = EnvStore()
    for ns, entries in self._raw_config.get(CytomineEnvSectionEnum.GLOBAL.value, {}).items():
      self._global_envs.add_namespace(ns, entries)
    
    self._servers_env_stores = defaultdict(lambda: EnvStore())
    for server, envs in self._raw_config.get(CytomineEnvSectionEnum.SERVICES.value, {}).items():
      for ns, entries in envs.items():
        self._servers_env_stores[server].add_namespace(ns, entries, store=self._global_envs)

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

  def as_dict(self):
    target_dict = dict()
    target_dict["global"] = self._global_envs.export_dict()
    target_dict["services"] = dict()
    for server, env_store in self._servers_env_stores.items():
      target_dict["services"][server] = env_store.export_dict()
    return target_dict


class DockerComposeFile:
  """light parsing of docker compose files"""
  def __init__(self, path, filename=DOCKER_COMPOSE_FILENAME) -> None:
    self._path = path
    self._filename = filename

    with open(self.filepath, "r", encoding="utf8") as file:
      self._content = yaml.load(file)
 
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
