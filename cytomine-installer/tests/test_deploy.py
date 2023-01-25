import os
from distutils.dir_util import copy_tree
from pathlib import Path
from tempfile import TemporaryDirectory
import zipfile

from bootstrapper import parser
from bootstrapper.actions.errors import InvalidTargetDirectoryError
from bootstrapper.deployment.deployment_folders import InvalidServerConfigurationError
from bootstrapper.util import list_relative_files
from tests.test_deployment_folders import FileSystemTestCase




class TestDeploy(FileSystemTestCase): 

  def testDeployInNonEmptyDirectory(self):
    tests_path = os.path.dirname(__file__)
    deploy_file_path = os.path.join(tests_path, "files", "fake_single_server_no_auto", "in")
    with TemporaryDirectory() as tmpdir:
      # create fake files and folders
      Path(os.path.join(tmpdir, "file.txt")).touch()
      os.makedirs(os.path.join(tmpdir, "folder"))
      Path(os.path.join(tmpdir, "folder", "file2.txt")).touch()

      with self.assertRaises(InvalidTargetDirectoryError):
        parser.call([
          "deploy", 
          "-s", deploy_file_path,
          "-t", tmpdir
        ])
    
  def testDeployInNonEmptyDirectoryWithOverwrite(self):
    tests_path = os.path.dirname(__file__)
    deploy_file_path = os.path.join(tests_path, "files", "fake_single_server_no_auto", "in")
    output_ref_path = os.path.join(tests_path, "files", "fake_single_server_no_auto", "out")
    with TemporaryDirectory() as tmpdir:
      # create fake files and folders
      Path(os.path.join(tmpdir, "file.txt")).touch()
      os.makedirs(os.path.join(tmpdir, "folder"))
      Path(os.path.join(tmpdir, "folder", "file2.txt")).touch()

      parser.call([
        "deploy", 
        "-s", deploy_file_path,
        "-t", tmpdir,
        "--overwrite"
      ])

      self.assertSameDirectories(tmpdir, output_ref_path)

  def testFakeSingleServerNoZip(self):
    tests_path = os.path.dirname(__file__)
    deploy_file_path = os.path.join(tests_path, "files", "fake_single_server_no_auto", "in")
    output_ref_path = os.path.join(tests_path, "files", "fake_single_server_no_auto", "out")
    with TemporaryDirectory() as tmpdir:
      parser.call([
        "deploy", 
        "-s", deploy_file_path,
        "-t", tmpdir
      ])
    
      self.assertSameDirectories(tmpdir, output_ref_path)
      self.assertListEqual([f for f in os.listdir(tmpdir) if f.endswith(".zip")], [])

  def testDeployFakeSingleServerWithZip(self):
    tests_path = os.path.dirname(__file__)
    deploy_file_path = os.path.join(tests_path, "files", "fake_single_server_no_auto", "in")
    output_ref_path = os.path.join(tests_path, "files", "fake_single_server_no_auto", "out")
    with TemporaryDirectory() as tmpdir:
      parser.call([
        "deploy", 
        "-s", deploy_file_path,
        "-t", tmpdir,
        "-z", 
      ])

      self.assertSameDirectories(tmpdir, output_ref_path)
      zip_files = [f for f in os.listdir(tmpdir) if f.endswith(".zip")]
      self.assertTrue(len(zip_files), 1)
      zip_file = zip_files[0]
      
      with zipfile.ZipFile(os.path.join(tmpdir, zip_file), "r", zipfile.ZIP_DEFLATED) as zip_archive:
        self.assertListEqual(sorted(zip_archive.namelist()), sorted(list_relative_files(deploy_file_path)))
  

  def testMultiServer(self):
    tests_path = os.path.dirname(__file__)
    deploy_file_path = os.path.join(tests_path, "files", "fake_multi_server", "in")
    output_ref_path = os.path.join(tests_path, "files", "fake_multi_server", "out")
    
    with TemporaryDirectory() as tmpdir:
      parser.call([
        "deploy", 
        "-s", deploy_file_path,
        "-t", tmpdir,
        "-z", 
      ])

      self.assertSameDirectories(tmpdir, output_ref_path)

      zip_files = [f for f in os.listdir(tmpdir) if f.endswith(".zip")]
      self.assertTrue(len(zip_files), 1)
      zip_file = zip_files[0]
      
      with zipfile.ZipFile(os.path.join(tmpdir, zip_file), "r", zipfile.ZIP_DEFLATED) as zip_archive:
        self.assertListEqual(sorted(zip_archive.namelist()), sorted(list_relative_files(deploy_file_path)))

  def testMultiServerMissingFolder(self):
    tests_path = os.path.dirname(__file__)
    deploy_file_path = os.path.join(tests_path, "files", "fake_multi_server_missing_folder")
    with TemporaryDirectory() as tmpdir:
      with self.assertRaises(InvalidServerConfigurationError):
        parser.call([
          "deploy", 
          "-s", deploy_file_path,
          "-t", tmpdir,
          "-z", 
        ], raise_boostrapper_errors=True)

      