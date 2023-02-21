import os
import sys
from bootstrapper.errors import BootstrapperError
from bootstrapper.parser import call


def run():
    try:
        call(sys.argv[1:])
    except BootstrapperError:
        sys.exit(os.EX_USAGE)
