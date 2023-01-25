from abc import ABC, abstractmethod
from argparse import ArgumentParser


class AbstractAction(ABC):
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

  def fill_in_subparser(self, action_name: str, main_parser: ArgumentParser):
    sub_parser = main_parser.add_parser(action_name)
    self._fill_in_subparser(sub_parser)
    sub_parser.set_defaults(func=self.run)
    return sub_parser