import { readFile, writeFile, existsSync, mkdirSync } from 'fs';
import { join } from 'path';


function CSVToArray( strData, str_delim=',') {
  // https://stackoverflow.com/questions/1293147/example-javascript-code-to-parse-csv-data
  // Create a regular expression to parse the CSV values.
  var objPattern = new RegExp((
        // Delimiters.
        "(\\" + str_delim + "|\\r?\\n|\\r|^)" +
        // Quoted fields.
        "(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|" +
        // Standard fields.
        "([^\"\\" + str_delim + "\\r\\n]*))"
    ), "gi" );
  var arrData = [[]];
  var arrMatches = null;

  while (arrMatches = objPattern.exec( strData )){
    // Get the delimiter that was found.
    let strMatchedDelimiter = arrMatches[ 1 ];

    // Check to see if the given delimiter has a length (is not the start of string) and if it matches
    // field delimiter. If id does not, then we know that this delimiter is a row delimiter.
    if (strMatchedDelimiter.length && strMatchedDelimiter !== str_delim){
      // Since we have reached a new row of data, add an empty row to our data array.
      arrData.push( [] );
    }

    let strMatchedValue;

    // Now that we have our delimiter out of the way, let's check to see which kind of value we
    // captured (quoted or unquoted).
    if (arrMatches[ 2 ]){
      // We found a quoted value. When we capture this value, unescape any double quotes.
      strMatchedValue = arrMatches[2].replace(new RegExp( "\"\"", "g" ),"\"");
    } else {
        
        strMatchedValue = arrMatches[3]; // We found a non-quoted value.
    }
    // Now that we have our value string, let's add // it to the data array.
    arrData[arrData.length - 1].push( strMatchedValue );
  }

  // Return the parsed data.
  return arrData;
}

function create_deep_field(obj, keys, value, overwrite=false) {
  if (keys.length < 1) {
    throw Error("key should contain at least one element");
  } else if (keys.length == 1) {
    let key = keys[0];
    if(!overwrite && key in obj){
      throw Error(`key ${key} already contained`);
    }
    obj[key] = value;
    return;
  } 
  let curr_key = keys[0];
  if (!(curr_key in obj)) {
    obj[curr_key] = {};
  }
  create_deep_field(obj[curr_key], keys.slice(1), value);
}

export function make_i18n_jsons (csv_path, dest_path) {
  readFile(csv_path, "utf-8", (err, content) => {
    if (err) {
      throw err;
    }
    
    let csv_content = CSVToArray(content, ',');
    let headers = csv_content[0];
    let data = csv_content.slice(1);
    let languages = headers.slice(1);
    let i18n = {};
    for (let lang_index in languages) {
      i18n[languages[lang_index]] = {};
    }

    console.log(`Found ` + data.length + ` translations in ` + languages.length + ` language(s): ` + JSON.stringify(languages));

    for (let row_index in data) {
      let row = data[row_index];
      let keys = row[0].split(".");
      for (let lang_index = 1; lang_index <= languages.length; ++lang_index) {
        create_deep_field(i18n[languages[lang_index - 1]], keys, row[lang_index]);
      }
    }

    if (!existsSync(dest_path)) {
      mkdirSync(dest_path, { recursive: true });
    }

    for (let lang_index in languages) {
      let lang = languages[lang_index];
      writeFile(join(dest_path, lang + ".i18n.json"), JSON.stringify(i18n[lang]), (err) => {
        if (err) {
          throw err;
        }
      });
    }
  })
}


if (process.argv.length == 4) {
  console.log("generate translations files for " + JSON.stringify(process.argv));
  make_i18n_jsons(process.argv[2], process.argv[3]);
}