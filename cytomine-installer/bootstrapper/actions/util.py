from .errors import StoppedByUserError


def prompt_proceed_question(reason):
  reason = reason.strip()
  if len(reason) > 0 and reason[-1] not in {'.', '!', '?', '\n', '\r'}:
    reason += ". " 
  print(reason + "Do you want to proceed ? [y]es/[n]o", flush=True, end=" ")
  response = ""
  positive, negative = {"y", "yes"}, {"n", "no"}
  while response not in positive.union(negative):
    response = input()
  if response.lower() in negative:
    raise StoppedByUserError(reason)
