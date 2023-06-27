import os
import re


def write_dotenv(directory: str, envs: dict, filename: str = ".env"):
    """Write environment variable encoded in a dictonary into a .env file
    Returns filepath
    """
    filepath = os.path.join(directory, filename)
    newline_pattern = re.compile(r"(?:\r\n|\n|\r)")
    with open(filepath, "w", encoding="utf8") as file:
        for key, value in envs.items():
            if isinstance(value, bool):
                value = "true" if value else "false"
            if newline_pattern.search(str(value)) is not None:
                value = f'"{value}"'
            file.write(f"{key}={value}{os.linesep}")
    return filepath


def list_relative_files(_dir: str):
    if not os.path.isdir(_dir):
        return []
    curr, dirs, files = next(os.walk(_dir))
    for dirname in dirs:
        files.extend(
            [
                os.path.join(dirname, filepath)
                for filepath in list_relative_files(os.path.join(curr, dirname))
            ]
        )
    return files


def delete_dir_content(_dir: str):
    for file_to_remove in list_relative_files(_dir):
        file_path = os.path.join(_dir, file_to_remove)
        if not os.path.exists(file_path):
            continue
        os.remove(file_path)
        try:
            os.removedirs(os.path.dirname(file_path))
        except OSError:
            pass
