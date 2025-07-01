from collections import defaultdict
import csv
import sys


def read_translations(filepath):
  with open(filepath, "r", encoding="utf8") as file:
    reader = csv.DictReader(file, delimiter=",", quotechar='"')
    languages = reader.fieldnames[1:]
    entries = [row for row in reader]
    return languages, entries


def find_duplicate_keys(strings: list):
  """Find the duplicate keys in a list"""
  seen = set()
  duplicates = set()
  for string in strings:
    if string in seen:
      duplicates.add(string)
    else:
      seen.add(string)
  return duplicates


def align_keys(keys1: list, keys2: list):
  """Align keys in two different list to preserve relative ordering when possible"""
  index_map1 = {key: i for i, key in enumerate(keys1)}
  index_map2 = {key: i for i, key in enumerate(keys2)}
  output_keys = list()
  curr1, curr2 = 0, 0
  added_in_output = set()
  while curr1 < len(keys1) and curr2 < len(keys2):
    key1 = keys1[curr1]
    key2 = keys2[curr2]
    if key1 in added_in_output:
      curr1 += 1
      continue
    if key2 in added_in_output:
      curr2 += 1
      continue 
  
    if key1 == key2:
      curr1 += 1
      curr2 += 1
      output_keys.append(key1)
      added_in_output.add(key1)
    else:
      # is new ?
      key2_in_entries1 = index_map1.get(key2)
      key1_in_entries2 = index_map2.get(key1)
      if key2_in_entries1 is None and key1_in_entries2 is None: # both keys are new
        curr1 += 1
        curr2 += 1
        output_keys.append(key1)
        added_in_output.add(key1)
        output_keys.append(key2)
        added_in_output.add(key2)
      else:
        if key2_in_entries1 is None: # key2 is a new key but key1 is not 
          output_keys.append(key2)
          added_in_output.add(key2)
          curr2 += 1
        else: 
          # option 1: key1 is a new key but key2 is not 
          # option 2: both keys exist in the other list, use list 1 as reference but need not to add it later via list 2
          # in both cases, add the keys 
          output_keys.append(key1)
          output_keys.append(key2)
          added_in_output.add(key1)
          added_in_output.add(key2)
          curr1 += 1
          curr2 += 1
          
  if curr1 < len(keys1):
    output_keys.extend([key for key in keys1[curr1:]])

  if curr2 < len(keys2):
    output_keys.extend([key for key in keys2[curr2:]])

  return output_keys


def diff(s1, s2):
    """Generate a diff between two strings (made with ChatGPT)"""
    lines1 = s1.splitlines()
    lines2 = s2.splitlines()
    len1 = len(lines1)
    len2 = len(lines2)

    table = [[0] * (len2 + 1) for _ in range(len1 + 1)]
    for i in range(1, len1 + 1):
        for j in range(1, len2 + 1):
            if lines1[i - 1] == lines2[j - 1]:
                table[i][j] = table[i - 1][j - 1] + 1
            else:
                table[i][j] = max(table[i][j - 1], table[i - 1][j])

    result = []
    i = len1
    j = len2
    while i > 0 or j > 0:
        if i > 0 and j > 0 and lines1[i - 1] == lines2[j - 1]:
            result.append(' ' + lines1[i - 1])
            i -= 1
            j -= 1
        elif j > 0 and (i == 0 or table[i][j - 1] >= table[i - 1][j]):
            result.append('+' + lines2[j - 1])
            j -= 1
        else:
            result.append('-' + lines1[i - 1])
            i -= 1

    return '\n'.join(result[::-1])


def find_transalation_for_key(key, langs, entry1, entry2):
  """Extract translation data from the best possible source"""
  data = {"key": key}
  ambiguity = False
  for lang in langs:
    exists_in_1 = (entry1 is not None and lang in entry1 and entry1[lang] is not None and len(entry1[lang].strip()) > 0)
    exists_in_2 = (entry2 is not None and lang in entry2 and entry2[lang] is not None and len(entry2[lang].strip()) > 0)
    if not exists_in_1 and not exists_in_2:
      data[lang] = ""
    elif not exists_in_1 and exists_in_2:
      data[lang] = entry2[lang]
    elif not exists_in_2 and exists_in_1:
      data[lang] = entry1[lang]
    elif entry1[lang] == entry2[lang]:  # exist in both
      data[lang] = entry1[lang]
    else:
      ambiguity = True
      data[lang] = None
      print(f"translation '{lang}' for key '{key}' exists in both file but with a mismatch\n{diff(entry1[lang], entry2[lang])}", file=sys.stderr)
  return ambiguity, data


def main():
  # Specify the conflicting translation files here
  file1_path = "src/locales/translations.csv"
  file2_path = "/home/rmormont/Downloads/translations__2023_0503_IS_nl.csv"

  lang1, entries1 = read_translations(file1_path)
  lang2, entries2 = read_translations(file2_path)

  duplicates1 = find_duplicate_keys([entry["key"] for entry in entries1])
  duplicates2 = find_duplicate_keys([entry["key"] for entry in entries2])

  if len(duplicates1) > 0:
    raise ValueError(f"found {len(duplicates1)} duplicate(s) in file {file1_path}: {','.join(duplicates1)}")
  if len(duplicates2) > 0:
    raise ValueError(f"found {len(duplicates2)} duplicate(s) in file {file2_path}: {','.join(duplicates2)}")

  entry_map1 = {entry["key"]: entry for entry in entries1}
  entry_map2 = {entry["key"]: entry for entry in entries2}
  ordered_keys_out = align_keys([entry["key"] for entry in entries1], [entry["key"] for entry in entries2])

  all_langs = set(lang1).union(lang2)
  output_translations = list()

  ambiguity = False
  for key in ordered_keys_out:
    ambiguity, translation = find_transalation_for_key(key, all_langs, entry_map1.get(key), entry_map2.get(key))
    output_translations.append(translation)

  if ambiguity:
    raise ValueError("at least one ambiguous translation found, please resolve in both files before proceeding")
  
  # The resulting merge is generated in an output.csv file
  with open("output.csv", "w+", encoding="utf8") as file:
    writer = csv.DictWriter(file, fieldnames=["key", *align_keys(lang1, lang2)], delimiter=",", quotechar='"')
    writer.writeheader()
    writer.writerows(output_translations)


if __name__ == "__main__":
  main()
