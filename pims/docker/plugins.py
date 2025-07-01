#  * Copyright (c) 2020-2021. Authors: see NOTICE file.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.

# Can only use stdlib as it will be run before `pip install`
import csv
import os
import re
import subprocess
import sys
from argparse import ArgumentParser
from enum import Enum

INSTALL_PREREQUISITES = "install-prerequisites.sh"


class Method(str, Enum):
    DOWNLOAD = "download"
    DEPENDENCIES_BEFORE_VIPS = "dependencies_before_vips"
    DEPENDENCIES_BEFORE_PYTHON = "dependencies_before_python"
    INSTALL = "install"
    GENERATE_CHECKER_RESOLUTION_FILE = "checker_resolution_file"


def load_plugin_list(csv_path):
    with open(csv_path, "r") as file:
        return [
            {k: v for k, v in row.items()}
            for row in csv.DictReader(file, skipinitialspace=True)
        ]


def generate_checker_resolution_file(plugins, csv_path, name_column, resolution_order_column):
    with open(csv_path, "w", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=[name_column, resolution_order_column])
        writer.writeheader()
        for plugin in plugins:
            name = plugin.get(name_column).strip()
            resolution_order = plugin.get(resolution_order_column, 0).strip()
            if re.match(r"^(?:-?[0-9]+)?$", resolution_order) is None:
                raise ValueError(
                    "Review resolution order '"
                    + resolution_order
                    + "' for plugin "
                    + name
                    + ": the resolution order must be an integer or empty string"
                )
            else:
                resolution_order = int(resolution_order) if len(resolution_order) > 0 else 0

            writer.writerow({name_column: name, resolution_order_column: resolution_order})


def enabled_plugins(plugins):
    return [plugin for plugin in plugins if plugin["enabled"] != "0"]


def download_plugins(plugins, install_path):
    for plugin in plugins:
        print(f"Download {plugin['name']}")

        path = os.path.join(install_path, plugin["name"])
        command = f"git clone {plugin['git_url']} {path}"
        if plugin["git_branch_or_tag"]:
            command += f" && cd {path} && git checkout {plugin['git_branch_or_tag']}"

        output = subprocess.run(command, shell=True, check=True)
        print(output.stdout)
        print(output.stderr)


def run_install_func_for_plugins(plugins, install_path, func):
    for plugin in plugins:
        print(f"Run {func} for {plugin['name']}")

        path = os.path.join(install_path, plugin["name"])
        command = f"bash {INSTALL_PREREQUISITES} {func}"
        output = subprocess.run(command, shell=True, check=True, cwd=path)
        print(output.stdout)
        print(output.stderr)


def install_python_plugins(plugins, install_path):
    for plugin in plugins:
        print(f"Install {plugin['name']}")

        path = os.path.join(install_path, plugin["name"])
        command = f"pip install --no-cache-dir -e ."
        output = subprocess.run(command, shell=True, check=True, cwd=path)
        print(output.stdout)
        print(output.stderr)


if __name__ == "__main__":
    parser = ArgumentParser(prog="PIMS Plugins installer")
    parser.add_argument("--plugin_csv_path", help="Plugin list CSV path")
    parser.add_argument(
        "--checkerResolution_file_path",
        help="resolution_orders plugin CSV path",
        default="checkerResolution.csv",
    )
    parser.add_argument(
        "--resolution_order_column",
        help="Name of the resolution_order column from plugin list for checkerResolution file",
        default="resolution_order",
    )
    parser.add_argument(
        "--name_column",
        help="Name of the name column from plugin list for checkerResolution file",
        default="name",
    )
    parser.add_argument("--install_path", help="Plugin installation absolute path")
    parser.add_argument(
        "--method",
        help="What method to apply",
        choices=[enum_member for enum_member in Method],
    )
    params, other = parser.parse_known_args(sys.argv[1:])

    plugins = enabled_plugins(load_plugin_list(params.plugin_csv_path))

    if params.method == Method.GENERATE_CHECKER_RESOLUTION_FILE:
        if len(plugins) > 0 and params.resolution_order_column in plugins[0]:
            generate_checker_resolution_file(
                plugins,
                params.checkerResolution_file_path,
                params.name_column,
                params.resolution_order_column,
            )
    else:
        os.makedirs(params.install_path, exist_ok=True)
        if params.method == Method.DOWNLOAD:
            download_plugins(plugins, params.install_path)
        elif params.method == Method.INSTALL:
            install_python_plugins(plugins, params.install_path)
        else:
            run_install_func_for_plugins(plugins, params.install_path, params.method)
