from abc import ABC, abstractmethod
import os
import yaml
import shutil
from tempfile import TemporaryDirectory
from bootstrapper.deployment_files import DOCKER_COMPOSE_FILENAME, CytomineEnvsFile, DockerComposeFile, EditableDockerCompose
from bootstrapper.util import write_dotenv


class Deployable(ABC):
  @abstractmethod
  def deploy_files(self, target_directory):
    """Generates/transfers a set of files in the target directory."""
    pass


class ServerFolder(Deployable):
  def __init__(self, server_name, directory, envs: CytomineEnvsFile, configs_folder="configs", envs_folder="envs", in_container_configs_folder="cm_configs") -> None:
    """
    Parameters:
    -----------
    server_name: str
    directory: str
      Server directory path
    configs_folder: str
      Name of the configs folder (default: 'configs')
    envs_folder:
      Name of the target environment folder (default: 'envs')
    in_container_configs_folder:
      Name of the configuration target folder within the container (default: 'cm_configs)
    """
    self._server_name = server_name
    self._directory = directory
    self._configs_folder = configs_folder
    self._in_container_configs_folder = in_container_configs_folder
    self._envs_folder = envs_folder
    self._docker_compose_file = DockerComposeFile(directory)
    self._envs = envs

  @property
  def server_name(self):
    return self._server_name

  @property
  def has_config(self):
    return os.path.exists(self.configs_path)

  @property 
  def configs_path(self):
    return os.path.join(self._directory, self._configs_folder)

  @property
  def docker_compose_path(self):
    return os.path.join(self._directory, DOCKER_COMPOSE_FILENAME)

  def deploy_files(self, target_directory):
    """Generates a target server folder"""
    # docker-compose
    shutil.copyfile(
      self._docker_compose_file.filepath,
      os.path.join(target_directory, self._docker_compose_file.filename)
    )

    # .env file
    global_envs = dict()
    for namespace in self._envs.global_envs.namespaces:
      ns_envs = self._envs.global_envs.get_namespace_envs(namespace)
      global_envs.update({f"{namespace.upper()}_{key.upper()}": value for key, value in ns_envs.items()})
    write_dotenv(target_directory, global_envs)

    # docker-compose.override.yml
    override_file = EditableDockerCompose(version=self._docker_compose_file.version)

    # envs/{SERVICE}.env files 
    target_envs = os.path.join(target_directory, self._envs_folder)
    os.makedirs(target_envs)
    for service in self._docker_compose_file.services:
      env_store = self._envs.server_store(self._server_name)
      if not env_store.has_namespace(service):
        continue
      service_envs = env_store.get_namespace_envs(service)
      env_filepath = write_dotenv(target_envs, service_envs, filename=f"{service}.env")
      override_file.set_service_env_file(service, os.path.relpath(env_filepath, target_directory))
    
    # configs
    for service in self._docker_compose_file.services:
      src_service_configs_path = os.path.join(self._directory, self._configs_folder, service)
      if os.path.exists(src_service_configs_path):
        target_config_relpath = os.path.join(self._configs_folder, service)
        override_file.add_service_volume(service, f"{target_config_relpath}:/{self._in_container_configs_folder}")

    shutil.copytree(
      os.path.join(self._directory, self._configs_folder),
      os.path.join(target_directory, self._configs_folder)
    )

    # save override
    override_file.write_to(target_directory, "docker-compose.override.yml")

    return target_directory


class DeploymentFolder(Deployable):
  SERVER_DEFAULT = "default"

  def __init__(self, directory="/bootstrap", cytomine_envs_filename="cytomine.yml", 
               configs_folder="configs", envs_folder="envs", ignore_dirs=None, 
               in_container_configs_folder="cm_configs") -> None:
    """
    Parameters
    ----------
    directory: str
      path of the directory where cytomine.yml is stored
    cytomine_envs_filename: str
      name of the cytomine environment variables file
    configs_folder: str
      Name of the configs folder in each server folder (default: 'configs')
    envs_folder:
      Name of the target environment folder in the target server folder (default: 'envs')
    in_container_configs_folder:
      Name of the configuration target folder within the container (default: 'cm_configs)
    """
    if ignore_dirs is None:
      ignore_dirs = set()

    self._directory = directory
    self._ignore_dirs = set(ignore_dirs)
    self._configs_folder = configs_folder
    self._envs_folder = envs_folder
    self._in_container_configs_folder = in_container_configs_folder
    self._cytomine_envs = CytomineEnvsFile(path=self._directory, filename=cytomine_envs_filename)
    
    # check whether single-server or multi-server
    
    server_folder_common_params = {
      "configs_folder": self._configs_folder,
      "envs_folder": self._envs_folder,
      "in_container_configs_folder": self._in_container_configs_folder
    }

    self._server_folders = dict()
    _, subdirectories, _ = next(os.walk(self._directory))
    subdirectories = set(subdirectories)

    self._single_server = len(self._cytomine_envs.servers) == 1 and \
        self._cytomine_envs.servers[0] == self.SERVER_DEFAULT and \
        self.SERVER_DEFAULT not in subdirectories

    if self._single_server:
      # single server
      self._server_folders[self.SERVER_DEFAULT] = ServerFolder(
        server_name=self.SERVER_DEFAULT,
        directory=self._directory,
        envs=self._cytomine_envs,
        **server_folder_common_params
      )
    else:    
      for subdir in subdirectories.difference(ignore_dirs):
        self._server_folders[subdir] = ServerFolder(
          server_name=subdir,
          directory=os.path.join(self._directory, subdir),
          envs=self._cytomine_envs,
          **server_folder_common_params
        )
      
  def deploy_files(self, target_directory):
    dst_cytomine_envs_path = os.path.join(target_directory, self._cytomine_envs.filename)
    with open(dst_cytomine_envs_path, "w", encoding="utf8") as file:
      yaml.dump(self._cytomine_envs.export_dict(), file)
    
    for server_folder in self._server_folders.values():
      if self._single_server:
        server_target_dir = target_directory
      else:
        server_target_dir = os.path.join(target_directory, server_folder.server_name)
        os.makedirs(server_target_dir)
      server_folder.deploy_files(server_target_dir)
    

    
      