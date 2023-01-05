import os
import yaml
from collections import defaultdict
from env_store import EnvStore

DOCKER_COMPOSE_FILENAME = "docker-compose.yml"


class CytomineEnvsFile:
  """parses a cytomine.yml file"""
  def __init__(self, config_path="cytomine.yml") -> None:
    self._config_path = config_path
    
    with open(config_path, "r", encoding="utf8") as file:
      self._raw_config = yaml.load(file)
    
    self._global_envs = EnvStore()
    for ns, entries in self._raw_config.get("global", {}).items():
      self._global_envs.add_namespace(ns, entries)
    
    self._servers_env_stores = defaultdict(lambda: EnvStore())
    for server, envs in self._raw_config.get("services", {}).items():
      for ns, entries in envs.items():
        self._servers_env_stores[server].add_namespace(ns, entries, store=self._global_envs)

  @property
  def global_envs(self):
    return self._global_envs
    
  @property
  def servers(self):
    return list(self._servers_env_stores.keys())

  def services(self, server: str):
    return list(self._servers_env_stores[server].keys())

  def as_dict(self):
    target_dict = dict()
    target_dict["global"] = self._global_envs.as_dict()
    target_dict["services"] = dict()
    for server, env_store in self._servers_env_stores.items():
      target_dict["services"][server] = env_store.as_dict()
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
  def services(self):
    return list(self._content.get("services", {}).keys())
  
  @property
  def version(self):
    return self._content.get("version")
