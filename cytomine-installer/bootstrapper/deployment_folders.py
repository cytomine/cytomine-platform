from abc import ABC, abstractmethod
import os
import shutil
import yaml
from bootstrapper.deployment_files import DOCKER_COMPOSE_FILENAME, CytomineEnvsFile, DockerComposeFile, EditableDockerCompose
from bootstrapper.util import write_dotenv
from env_store import EnvStore
from collections import defaultdict


class Deployable(ABC):
  @abstractmethod
  def deploy_files(self, target_directory, envs: CytomineEnvsFile):
    """Generates/transfers a set of files in the target directory."""
    pass


class ServerFolder(Deployable):
  def __init__(self, server_name, directory, configs_folder="configs", envs_folder="envs", in_container_configs_folder="cm_configs") -> None:
    self._server_name = server_name
    self._directory = directory
    self._configs_folder = configs_folder
    self._in_container_configs_folder = in_container_configs_folder
    self._envs_folder = envs_folder
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
    """Generates a target server folder"""
    # docker-compose
    shutil.copyfile(
      self._docker_compose_file.filepath,
      os.path.join(self._directory, self._docker_compose_file.filename)
    )

    # .env file
    global_envs = dict()
    for namespace in envs.global_envs.namespaces:
      ns_envs = envs.global_envs.get_namespace_envs(namespace)
      global_envs.update({f"{namespace}_{key}": value for key, value in ns_envs.items()})
    write_dotenv(target_directory, global_envs)

    # docker-compose.override.yml
    override_file = EditableDockerCompose()

    # envs/{SERVICE}.env files 
    target_envs = os.path.join(target_directory, self._envs_folder)
    os.makedirs(target_envs)
    for service in self._docker_compose_file.services:
      env_store = envs.server_store(self._server_name)
      if not env_store.has_namespace(service):
        continue
      service_envs = env_store.get_namespace_envs(service)
      env_filepath = write_dotenv(target_envs, service_envs, filename=f"{service}.env")
      override_file.set_service_env_file(service, os.path.relpath(env_filepath, target_directory))
    
    # configs
    for service in self._docker_compose_file.services:
      server_dir_path = os.path.join(self._directory, self._server_name)
      service_configs_path = os.path.join(server_dir_path, self._configs_folder, service)
      if os.path.exists(service_configs_path):
        configs_relpath = os.path.relpath(service_configs_path, server_dir_path)
        override_file.add_service_volume(service, f"{configs_relpath}:/{self._in_container_configs_folder}")

    shutil.copytree(
      os.path.join(self._directory, self._configs_folder),
      os.path.join(target_directory, self._configs_folder)
    )

    # save override
    override_file.write_to(target_directory, "docker-compose.override.yml")

    return target_directory


class DeploymentConfiguration(Deployable):
  def __init__(self, directory="/bootstrap", cytomine_envs_filename="cytomine.yml", 
               configs_folder="configs") -> None:
    self._cytomine_envs = CytomineEnvsFile(config_path=os.path.join(directory, cytomine_envs_filename))

  def deploy_files(self, target_directory, envs: CytomineEnvsFile):
    return super().deploy_files(target_directory, envs)