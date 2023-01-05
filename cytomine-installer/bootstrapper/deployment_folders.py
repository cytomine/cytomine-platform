from abc import ABC, abstractmethod
import os
import yaml
from bootstrapper.deployment_files import DOCKER_COMPOSE_FILENAME, CytomineEnvsFile, DockerComposeFile
from env_store import EnvStore
from collections import defaultdict


class Deployable(ABC):
  @abstractmethod
  def deploy_files(self, target_directory, envs: CytomineEnvsFile):
    """Generates/transfers a set of files in the target directory."""
    pass


class ServerFolder(Deployable):
  def __init__(self, server_name, directory, configs_folder="configs") -> None:
    self._server_name = server_name
    self._directory = directory
    self._configs_folder = configs_folder
    self._docker_compose_file = DockerComposeFile(directory)

  @property
  def has_config(self):
    return os.path.exists(self.configs_path)

  @property 
  def configs_path(self):
    return os.path.join(self._directory, self._configs_folder)

  @property
  def docker_compose_path(self):
    return os.path.join(self._directory, DOCKER_COMPOSE_FILENAME)

  def deploy_files(self, target_directory, envs: CytomineEnvsFile):
    return super().deploy_files(target_directory, envs)


class DeploymentConfiguration(Deployable):
  def __init__(self, directory="/bootstrap", cytomine_envs_filename="cytomine.yml", 
               configs_folder="configs") -> None:
    self._cytomine_envs = CytomineEnvsFile(config_path=os.path.join(directory, cytomine_envs_filename))

  def deploy_files(self, target_directory, envs: CytomineEnvsFile):
    return super().deploy_files(target_directory, envs)