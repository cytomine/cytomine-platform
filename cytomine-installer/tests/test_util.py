

import os
from tempfile import TemporaryDirectory
from unittest import TestCase

from bootstrapper.util import write_dotenv


class TestUtil(TestCase):
  def testWriteDotenv(self):
    envs = {
      "VAR1": "value1",
      "VAR2": "value2"
    }
    with TemporaryDirectory() as tmpdir:
      write_dotenv(tmpdir, envs)
      with open(os.path.join(tmpdir, ".env"), "r", encoding="utf8") as file:
        parsed_envs = {line.split("=", 1)[0]: line.strip().split("=", 1)[1] for line in file.readlines()}
    
    self.assertDictEqual(envs, parsed_envs)