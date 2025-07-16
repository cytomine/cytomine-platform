

from collections import defaultdict
import csv


if __name__ == "__main__":
  filepath = "src/locales/translations.csv"

  with open(filepath, "r", encoding="utf8") as file:
    reader = csv.DictReader(file, delimiter=",", quotechar='"')
    langs = reader.fieldnames[1:]
    print(langs)

    missing_info = defaultdict(set)
    for row in reader:
      for lang in langs:
        if lang not in row or row[lang] is None or len(row[lang].strip()) == 0:
          missing_info[row["key"]].add(lang)
    
    print("missing translations per languages:")
    for lang in langs:
      count = len([s for s in missing_info.values() if lang in s])
      print(f"lang '{lang}': {count}")
    
    print("keys with at least on missing translation:")
    for key, s in missing_info.items():
      print(f"- key '{key}': {s}")