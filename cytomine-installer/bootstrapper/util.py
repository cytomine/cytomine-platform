import os


def write_dotenv(directory: str, envs: dict, filename: str=".env"):
  """Write environment variable encoded in a dictonary into a .env file
  Returns filepath
  """
  filepath = os.path.join(directory, filename)
  with open(filepath, "w", encoding="utf8") as file:
    file.writelines([f"{key}={value}" for key, value in envs])
  return filepath