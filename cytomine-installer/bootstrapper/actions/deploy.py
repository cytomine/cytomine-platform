import os
import shutil
import zipfile
from datetime import datetime
from argparse import ArgumentParser

from bootstrapper.util import delete_dir_content

from .errors import InvalidTargetDirectoryError
from .base import AbstractAction
from ..deployment.deployment_folders import DeploymentFolder


class DeployAction(AbstractAction):
    def _fill_in_subparser(self, sub_parser: ArgumentParser):
        """Fill the given sub_parser with the program arguments"""
        sub_parser.add_argument(
            "-s",
            "--source_directory",
            dest="source_directory",
            default=os.getcwd(),
            help="path to the source directory",
        )
        sub_parser.add_argument(
            "-t",
            "--target_directory",
            dest="target_directory",
            default=os.getcwd(),
            help="path to the target directory (should not exist or be empty)",
        )
        sub_parser.add_argument(
            "-z",
            "--do_zip",
            dest="do_zip",
            action="store_true",
            help="whether or not the current state of the deployment configuration should be"
            "saved into a zip file before being overwritten with the new configuration",
        )
        sub_parser.add_argument(
            "--zip-filename",
            dest="zip_filename",
            help="when do_zip=True, specify the name of the zip file. By default a name will be generated based on datetime",
        )
        sub_parser.add_argument(
            "--envs-folder-name",
            dest="envs_folder",
            default="envs",
            help="name of the environment variables folder",
        )
        sub_parser.add_argument(
            "--configs-folder-name",
            dest="configs_folder",
            default="configs",
            help="name of the configs folder",
        )
        sub_parser.add_argument(
            "--configs-mount-point",
            dest="configs_mount_point",
            default="/cm_configs",
            help="mount point for config files in the container",
        )
        sub_parser.add_argument(
            "--env-config-filename",
            dest="env_config_filename",
            default="cytomine.yml",
            help="name of the environment config file",
        )
        sub_parser.add_argument(
            "-i",
            "--ignored",
            dest="ignored_dirs",
            help="folders to ignores in working directory",
        )
        sub_parser.add_argument(
            "--overwrite",
            dest="overwrite",
            action="store_true",
            help="to clear content of the target_directory before generating the deployment files",
        )
        sub_parser.set_defaults(do_zip=False, overwirte=False)

    def run(self, namespace):
        """Executes the actions.
        Parameters
        ----------
        namespace: Namespace
          Command line arguments of the action (including local/global scope information)
        """
        if namespace.ignored_dirs is None:
            ignored_dirs = set()

        namespace.source_directory = os.path.normpath(namespace.source_directory)
        namespace.target_directory = os.path.normpath(namespace.target_directory)

        if len(os.listdir(namespace.target_directory)) > 0:
            if not namespace.overwrite:
                raise InvalidTargetDirectoryError(namespace.target_directory)
            delete_dir_content(namespace.target_directory)

        # generate in a temporary folder
        if not os.path.exists(namespace.target_directory):
            self.get_logger().info("> target directory does not exist: create it...")
            os.makedirs(namespace.target_directory)

        self.get_logger().info(f"deployment from '{namespace.source_directory}'")

        deployment_folder = DeploymentFolder(
            directory=namespace.source_directory,
            ignored_dirs=ignored_dirs,
            configs_folder=namespace.configs_folder,
            envs_folder=namespace.envs_folder,
            configs_mount_point=namespace.configs_mount_point,
        )

        # zip current files
        if namespace.do_zip:
            if namespace.zip_filename is None:
                zip_filename = datetime.utcnow().strftime("%Y%m%d%H%M%S")
                zip_filename = f"backup_{zip_filename}.zip"
            else:
                zip_filename = namespace.zip_filename
            zip_filepath = os.path.join(namespace.target_directory, zip_filename)
            self.get_logger().info(f"> zipping target files into '{zip_filepath}'...")
            with zipfile.ZipFile(
                zip_filepath, "w", zipfile.ZIP_DEFLATED
            ) as zip_archive:
                for file in deployment_folder.source_files:
                    zip_archive.write(
                        os.path.join(namespace.source_directory, file), file
                    )

        self.get_logger().info("> generate deployment files...")
        self.get_logger().debug(f"  copied : {deployment_folder.source_files}")
        self.get_logger().debug(f"  created: {deployment_folder.generated_files}")
        deployment_folder.deploy_files(namespace.target_directory)

        self.get_logger().info("> done...")
