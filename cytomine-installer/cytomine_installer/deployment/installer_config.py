import os
from enum import Enum

import yaml


class UpdatePolicy(Enum):
  """Policies for updating yaml from"""

  UPDATE_NEW_ONLY = "update_new_only"
  # TODO: UPDATE_ALL_BUT_AUTO = "update_all_but_auto"
  # TODO: UPDATE_NONE = "update_none"
  # TODO: UPDATE_ALL = "update_all"


class InstallerConfig():
  def __init__(self, filepath="installer_config.yml"):
    # read file config, if any
    self._filepath = filepath
    file_config = {}
    if os.path.isfile(filepath):
      with open(filepath, "r", encoding="utf8") as file:
        file_config = yaml.load(file, Loader=yaml.Loader)

    self._set_update_policy(file_config.get("update", {}))

  def _set_update_policy(self, update_config: dict):
    self.update_policy = update_config.get("policy", UpdatePolicy.UPDATE_NEW_ONLY)

    self.update_allow_list = None
    if self.update_policy == UpdatePolicy.UPDATE_NEW_ONLY:
      self.update_allow_list = update_config.get("allow_list", [])

  @property
  def filepath(self):
    return self._filepath

  @property
  def filename(self):
    if self.filepath is None:
      return None
    return os.path.basename(self.filepath)
