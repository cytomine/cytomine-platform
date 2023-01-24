
import os
import shutil
import zipfile
from datetime import datetime
from argparse import ArgumentParser
from tempfile import TemporaryDirectory
from bootstrapper.util import list_relative_files
from bootstrapper.deployment_folders import DeploymentFolder


def deploy(working_path, ignored_dirs=None, do_zip=False):
  if ignored_dirs is None:
    ignored_dirs = set()
  
  print(f"deployment from '{working_path}':")

  working_files = list_relative_files(working_path)
  working_files = [file for file in working_files if not any([file.startswith(d) for d in ignored_dirs])]

  # generate in a temporary folder
  with TemporaryDirectory() as tmp_dir:
    print("> intermediate deployment...")
    deployment_folder = DeploymentFolder(directory=working_path)
    deployment_folder.deploy_files(tmp_dir)

    # zip current files
    if do_zip:
      zip_filename = "backup_{}.zip".format(datetime.utcnow().strftime("%Y%m%d%H%M%S"))
      zip_filepath = os.path.join(tmp_dir, zip_filename)
      print(f"zipping current files into '{zip_filename}'...")
      with zipfile.ZipFile(zip_filepath, 'w', zipfile.ZIP_DEFLATED) as zip_archive:
        for file in working_files:
          zip_archive.write(os.path.join(working_path, file), file)
        
    print("> cleaning...")
    _, dirs, files = next(os.walk(working_path))
    for dirname in dirs:
      if dirname not in ignored_dirs:
        shutil.rmtree(os.path.join(working_path, dirname))
    for file in files:
      if not file.endswith(".zip"):
        os.remove(os.path.join(working_path, file))

    print("> moving generated files to working path...")
    _, dirs_to_copy, files_to_copy = next(os.walk(tmp_dir))
    for to_copy in dirs_to_copy:
      shutil.copytree(os.path.join(tmp_dir, to_copy), os.path.join(working_path, to_copy))
    for to_copy in files_to_copy:
      shutil.copyfile(os.path.join(tmp_dir, to_copy), os.path.join(working_path, to_copy))
    print("> done...")




def main(argv):
  parser = ArgumentParser("Cytomine configurator")
  parser.add_argument("-w", "--working_path", dest="working_path", help="working path, where the deployment files are located", default="/bootstrap")
  parser.add_argument("--env_file", dest="env_file", help="name to the cytomine yaml environment file", default="cytomine.yml")
  parser.add_argument("--zip", dest="do_zip", action="store_true", help="whether or not the current state of the deployment configuration should be saved into a zip file before being overwritten with the new configuration")
  parser.add_argument("-i", "--ignore", dest="ignored", nargs="*", action="append", help="folders from the working path to be ignored by the bootstrapper")
  parser.set_defaults(do_zip=False)
  args, _ = parser.parse_known_args(argv)

  if args.ignored is None:
    args.ignored = list()

  # normalize and filter ignored path
  ignored_dirs = list()
  for ignored in args.ignored:
    ignored = os.path.normpath(ignored)
    if ignored.startswith("/") or "/" in ignored:
      raise ValueError(f"invalid folder name {ignored}")
    ignored_path = os.path.join(ignored)
    if not os.path.exists(ignored_path):
      continue
    if not os.path.isdir(ignored_path):
      raise ValueError(f"ignored folder at '{ignored_path}' is not a directory")
    ignored_dirs.append(ignored)

  # TODO better error handling and displaying
  deploy(
    args.working_path, 
    ignored_dirs=ignored_dirs,
    do_zip=args.do_zip)


if __name__ == "__main__":
  import sys
  main(sys.argv)