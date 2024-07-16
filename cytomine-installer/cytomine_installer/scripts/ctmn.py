import os
import sys
from cytomine_installer.errors import InstallerError
from cytomine_installer.parser import call


def run():
  try:
    call(sys.argv[1:])
  except InstallerError:
    sys.exit(os.EX_USAGE)
