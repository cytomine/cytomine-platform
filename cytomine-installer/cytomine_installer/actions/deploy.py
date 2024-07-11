import os
import pathlib
import shutil
import zipfile
from argparse import ArgumentParser
from datetime import datetime
from datetime import UTC
from distutils.dir_util import copy_tree
from tempfile import TemporaryDirectory

from cytomine_installer.deployment.installer_config import InstallerConfig

from ..deployment.deployment_folders import DeploymentFolder
from .base import AbstractAction
from .errors import InvalidTargetDirectoryError


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
            help="path to the target directory (by default, the target is the source directory)",
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
            "--working-config-filename",
            dest="working_config_filename",
            default="cytomine.yml",
            help="name of the working yaml config file",
        )
        sub_parser.add_argument(
            "--template-config-filename",
            dest="template_config_filename",
            default="cytomine.template",
            help="name of the template yaml config file",
        )
        sub_parser.add_argument(
            "--overwrite",
            dest="overwrite",
            action="store_true",
            help="to clear content of the target_directory before generating the deployment files (only used if a target directory different from the source directory is specified)",
        )
        sub_parser.add_argument(
            "--installer_config",
            dest="installer_config",
            default="installer_config.yml",
            help="name of the installer yaml configuration file",
        )
        sub_parser.set_defaults(do_zip=False, overwrite=False)

    def run(self, namespace):
        """Executes the actions.
        Parameters
        ----------
        namespace: Namespace
          Command line arguments of the action (including local/global scope information)
        """
        namespace.source_directory = os.path.normpath(namespace.source_directory)
        if namespace.target_directory is None:  # in-place
            namespace.target_directory = namespace.source_directory
        else:
            namespace.target_directory = os.path.normpath(namespace.target_directory)

        installer_config = InstallerConfig(
            filepath=os.path.join(
                namespace.source_directory, namespace.installer_config
            )
        )

        if (
            namespace.source_directory != namespace.target_directory
            and os.path.exists(namespace.target_directory)
            and len(os.listdir(namespace.target_directory)) > 0
            and not namespace.overwrite
        ):
            raise InvalidTargetDirectoryError(namespace.target_directory)

        if namespace.source_directory == namespace.target_directory:
            self.get_logger().info(
                f"deploy files in-place in '{namespace.source_directory}'"
            )
        else:
            os.makedirs(namespace.target_directory, exist_ok=True)
            self.get_logger().info(
                f"deploy files from '{namespace.source_directory}' to '{namespace.target_directory}'"
            )

        deployment_folder = DeploymentFolder(
            directory=namespace.source_directory,
            configs_folder=namespace.configs_folder,
            envs_folder=namespace.envs_folder,
            configs_mount_point=namespace.configs_mount_point,
            working_config_filename=namespace.working_config_filename,
            template_config_filename=namespace.template_config_filename,
            installer_config=installer_config,
        )

        with TemporaryDirectory() as tmpdir:
            # zip current files
            if namespace.do_zip:
                if namespace.zip_filename is None:
                    zip_filename = datetime.now(UTC).strftime("%Y%m%d%H%M%S")
                    zip_filename = f"backup_{zip_filename}.zip"
                else:
                    zip_filename = namespace.zip_filename
                zip_filepath = os.path.join(namespace.target_directory, zip_filename)
                self.get_logger().info(f"zipping source files into '{zip_filepath}'...")
                with zipfile.ZipFile(
                    zip_filepath, "w", zipfile.ZIP_DEFLATED
                ) as zip_archive:
                    for file in deployment_folder.source_files:
                        zip_archive.write(
                            os.path.join(namespace.source_directory, file), file
                        )

            self.get_logger().info("generate deployment files...")
            self.deploy_and_move(
                deployment_folder=deployment_folder,
                gen_dir=tmpdir,
                target_dir=namespace.target_directory,
            )

        self.get_logger().info("done...")

    def deploy_and_move(
        self, deployment_folder: DeploymentFolder, gen_dir: str, target_dir: str
    ):
        # generate relative paths
        deployment_folder.deploy_files(gen_dir)
        for file_relpath in (
            deployment_folder.generated_files + deployment_folder.source_files
        ):
            self.move_file_or_folder(
                source_dir=gen_dir,
                target_dir=target_dir,
                relpath=file_relpath,
                delete_target_before=True,
                skip_missing_source=False,
            )

    def move_file_or_folder(
        self,
        source_dir,
        target_dir,
        relpath,
        delete_target_before=False,
        skip_missing_source=True,
    ):
        source_path = os.path.normpath(os.path.join(source_dir, relpath))
        target_path = os.path.normpath(os.path.join(target_dir, relpath))
        if not os.path.exists(source_path):
            if skip_missing_source:
                return
            raise FileNotFoundError(
                f"cannot find source file or folder '{source_path}'"
            )
        if os.path.exists(target_path):
            self.get_logger().debug(f"> '{relpath}' (replace)")
        else:
            self.get_logger().debug(f"> '{relpath}' (create)")
        if delete_target_before:
            pathlib.Path(target_path).unlink(missing_ok=True)
        os.makedirs(os.path.dirname(target_path), exist_ok=True)
        if os.path.isdir(source_path):
            copy_tree(source_path, target_path)
        else:
            shutil.copyfile(source_path, target_path)
