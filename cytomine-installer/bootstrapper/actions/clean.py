


from abc import abstractmethod
from argparse import ArgumentParser

from bootstrapper.actions.base import AbstractAction


class CleanAction(AbstractAction):
  @abstractmethod
  def _fill_in_subparser(self, sub_parser: ArgumentParser):
    """Fill the given sub_parser with the program arguments"""
    pass

  @abstractmethod
  def run(self, namespace):
    """Executes the actions.
    Parameters
    ----------
    namespace: Namespace
      Command line arguments of the action (including local/global scope information)
    """
    pass
