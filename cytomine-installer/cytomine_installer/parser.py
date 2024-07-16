import sys
from argparse import ArgumentParser

from .actions.base import AbstractAction
from .actions.deploy import DeployAction
from .errors import InstallerError


def _recursive_parser_register(subparsers, actions: dict):
  """
  Parameters
  ----------
  subparsers: ArgumentParser
    A subparser action object (i.e. obtained with the `parser.add_subparsers`
    methods) where to register actions from 'actions'
  actions: dict
    An action dictionary: maps action or actions group key (string) with an
    Action or an action dictionary respectively.
  """
  for name_or_scope, action_or_group in actions.items():
    if isinstance(action_or_group, AbstractAction):
      action = action_or_group
      action.fill_in_subparser(name_or_scope, subparsers)
    elif isinstance(action_or_group, dict):
      intermediate = subparsers.add_parser(name_or_scope)
      next_subparsers = intermediate.add_subparsers()
      _recursive_parser_register(next_subparsers, action_or_group)
    else:
      raise TypeError(
        f"unexpected object in the actions dictionary: {type(action_or_group)}"
      )


def make_parser():
  """Build and return the parser"""
  main_parser = ArgumentParser(
    prog="cytoboot",
    description="Cytomine installer, a tool for generating docker-compose"
                "-based deployment configuration of cytomine.",
  )

  root_subparsers = main_parser.add_subparsers()

  all_actions = {
    # TODO "clean": CleanAction(),
    "deploy": DeployAction()
  }

  _recursive_parser_register(root_subparsers, all_actions)

  return main_parser


def call(argv, raise_boostrapper_errors=True):
  parser = make_parser()
  args = parser.parse_args(argv)
  if not hasattr(args, "func"):
    parser.print_help()
  else:
    try:
      args.func(args)
    except InstallerError as e:
      print(f"error: {e}", file=sys.stderr, flush=True)
      if raise_boostrapper_errors:
        raise e


if __name__ == "__main__":
  call(sys.argv[1:])
