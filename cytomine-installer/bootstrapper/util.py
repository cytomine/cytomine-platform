import os


def write_dotenv(directory: str, envs: dict, filename: str=".env"):
  """Write environment variable encoded in a dictonary into a .env file
  Returns filepath
  """
  filepath = os.path.join(directory, filename)
  with open(filepath, "w", encoding="utf8") as file:
    file.writelines([f"{key}={value}{os.linesep}" for key, value in envs.items()])
  return filepath


def list_relative_files(dir: str):
  curr, dirs, files = next(os.walk(dir))
  for dirname in dirs:
    files.extend([
      os.path.join(dirname, filepath)
      for filepath in list_relative_files(os.path.join(curr, dirname))
    ])
  return files