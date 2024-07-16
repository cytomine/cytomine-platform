import os
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase

from cytomine_installer.util import delete_dir_content, list_relative_files, write_dotenv


class TestUtil(TestCase):
  def test_write_dotenv(self):
    envs = {"VAR1": "value1", "VAR2": "value2"}
    with TemporaryDirectory() as tmpdir:
      write_dotenv(tmpdir, envs)
      with open(os.path.join(tmpdir, ".env"), "r", encoding="utf8") as file:
        parsed_envs = {
          line.split("=", 1)[0]: line.strip().split("=", 1)[1]
          for line in file.readlines()
        }

    self.assertDictEqual(envs, parsed_envs)

  def test_write_dotenv_alphabetical(self):
    envs = {
      "C _TEST": "value1",
      "P_TEST": "value2",
      "M_TEST": "value3",
      "A_TEST": "value4",
      "AA_TEST": "value5"
    }
    sorted_envs = [(k, envs[k]) for k in sorted(envs.keys())]
    with TemporaryDirectory() as tmpdir:
      write_dotenv(tmpdir, envs)
      with open(os.path.join(tmpdir, ".env"), "r", encoding="utf8") as file:
        parsed_envs = [
          (line.split("=", 1)[0], line.strip().split("=", 1)[1])
          for line in file.readlines()
        ]

    self.assertListEqual(sorted_envs, parsed_envs)

  def test_list_relative_files_invalid_path(self):
    with TemporaryDirectory() as tmpdir:
      self.assertEqual(len(list_relative_files(os.path.join(tmpdir, "aa"))), 0)

  def test_list_relative_files_empty_path(self):
    with TemporaryDirectory() as tmpdir:
      self.assertEqual(len(list_relative_files(tmpdir)), 0)

  def test_list_relative_files_one_file(self):
    with TemporaryDirectory() as tmpdir:
      with open(os.path.join(tmpdir, "file1.txt"), "w", encoding="utf8"):
        pass
      relative = list_relative_files(tmpdir)
      self.assertEqual(len(relative), 1)
      self.assertEqual(relative[0], "file1.txt")

  def test_list_relative_files_several_files(self):
    with TemporaryDirectory() as tmpdir:
      files = ["file1.txt", "file2.txt"]
      for file in files:
        with open(os.path.join(tmpdir, file), "w", encoding="utf8"):
          pass
      relative = list_relative_files(tmpdir)
      self.assertEqual(len(relative), 2)
      self.assertListEqual(sorted(files), sorted(relative))

  def test_list_relative_files_several_files_and_folders(self):
    with TemporaryDirectory() as tmpdir:
      dirs = ["dir1", "dir2"]
      for dirname in dirs:
        os.makedirs(os.path.join(tmpdir, dirname))
      files = ["dir1/file1.txt", "file2.txt"]
      for file in files:
        with open(os.path.join(tmpdir, file), "w", encoding="utf8"):
          pass
      relative = list_relative_files(tmpdir)
      self.assertEqual(len(relative), 2)
      self.assertListEqual(sorted(files), sorted(relative))

  def test_delete_dir_content(self):
    with TemporaryDirectory() as tmpdir:
      # create fake files and folders
      Path(os.path.join(tmpdir, "file.txt")).touch()
      os.makedirs(os.path.join(tmpdir, "folder"))
      Path(os.path.join(tmpdir, "folder", "file2.txt")).touch()

      delete_dir_content(tmpdir)
      os.makedirs(tmpdir)

      self.assertListEqual(list_relative_files(tmpdir), [])
