use jzon::{array, object, JsonValue};
use lazy_static::lazy_static;
use std::collections::{BTreeSet, HashMap};
use std::sync::Mutex;

use include_dir::{include_dir, Dir};

use crate::include_file;

static MASTERDATA_ROOT: Dir<'_> = include_dir!("$CARGO_MANIFEST_DIR/src/router/databases");

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum Region {
    Jp,
    En,
    Kr,
    ZhCht,
}

lazy_static! {
    static ref TABLE_CACHE: Mutex<HashMap<(Region, String), JsonValue>> =
        Mutex::new(HashMap::new());

    // This also needs to be packed into the client - this never changes
    static ref SCHEMAS: JsonValue = jzon::parse(
        &include_file!("src/router/databases/schemas.json")
    ).expect("schemas.json is malformed");
}

fn region_subdirs(region: Region) -> &'static [&'static str] {
    match region {
        Region::Jp => &["csv"],
        Region::En => &["csv-en"],
        Region::Kr => &["csv-kr", "csv-en"],
        Region::ZhCht => &["csv-zh-cht"],
    }
}

fn bundled_csv_bytes(region: Region, name: &str) -> Option<Vec<u8>> {
    for subdir in region_subdirs(region) {
        let rel = format!("{}/{}.csv", subdir, name);
        if let Some(file) = MASTERDATA_ROOT.get_file(rel) {
            return Some(file.contents().to_vec());
        }
    }
    None
}

fn bundled_table_names(region: Region) -> BTreeSet<String> {
    let mut names = BTreeSet::new();
    for subdir in region_subdirs(region) {
        if let Some(dir) = MASTERDATA_ROOT.get_dir(subdir) {
            for file in dir.files() {
                if let Some(stem) = file.path().file_stem().and_then(|s| s.to_str()) {
                    if !stem.is_empty() {
                        names.insert(stem.to_owned());
                    }
                }
            }
        }
    }
    names
}

pub fn get_all(region: Region) -> JsonValue {
    let mut rv = object!{};

    for table_name in bundled_table_names(region) {
        if let Some(bytes) = csv_bytes(region, &table_name) {
            rv[table_name.as_str()] = String::from_utf8(bytes).unwrap_or_default().into();
        }
    }

    rv
}

pub fn csv_bytes(region: Region, name: &str) -> Option<Vec<u8>> {
    // External runtime masterdata has priority. This makes AndroidEw's optional
    // masterdata picker an override, not a required setting.
    for subdir in region_subdirs(region) {
        let rel = format!("{}/{}.csv", subdir, name);
        if let Some(bytes) = crate::runtime::read_masterdata_file(&rel) {
            return Some(bytes);
        }
    }
    bundled_csv_bytes(region, name)
}

pub fn table(region: Region, name: &str) -> JsonValue {
    let key = (region, name.to_owned());
    if let Some(cached) = TABLE_CACHE.lock().unwrap().get(&key) {
        return cached.clone();
    }

    let bytes = csv_bytes(region, name).unwrap_or_else(|| {
        panic!("masterdata CSV not bundled: {name}.csv ({region:?})")
    });
    let parsed = parse_csv(name, &bytes);

    TABLE_CACHE.lock().unwrap().insert(key, parsed.clone());
    parsed
}

fn field_types(table_name: &str) -> HashMap<String, String> {
    let mut out = HashMap::new();
    let table = &SCHEMAS["tables"][table_name];
    for f in table["fields"].members() {
        out.insert(f["name"].to_string(), f["type"].to_string());
    }
    out
}

fn parse_csv(table_name: &str, bytes: &[u8]) -> JsonValue {
    let types = field_types(table_name);

    let mut rdr = csv::ReaderBuilder::new()
        .has_headers(true)
        .flexible(true)
        .from_reader(bytes);

    let raw_headers: Vec<String> = rdr
        .headers()
        .expect("malformed CSV header")
        .iter()
        .enumerate()
        .map(|(i, h)| {
            if i == 0 {
                h.trim_start_matches('\u{feff}').to_owned()
            } else {
                h.to_owned()
            }
        })
        .collect();

    let json_keys: Vec<String> = raw_headers
        .iter()
        .map(|h| h.strip_prefix('_').unwrap_or(h).to_owned())
        .collect();

    // Default missing schema columns to "string" — safer than guessing.
    let column_types: Vec<&str> = raw_headers
        .iter()
        .map(|h| types.get(h.as_str()).map(String::as_str).unwrap_or("string"))
        .collect();

    let mut out = array![];
    for record in rdr.records() {
        let record = record.expect("malformed CSV row");
        let mut row = object! {};
        for (i, raw) in record.iter().enumerate() {
            let key = match json_keys.get(i) {
                Some(k) => k.as_str(),
                None => continue, // extra trailing columns — ignore
            };
            row[key] = coerce(raw, column_types[i]);
        }
        out.push(row).expect("array push");
    }
    out
}

fn coerce(raw: &str, type_token: &str) -> JsonValue {
    if let Some(elem) = type_token.strip_suffix("[]") {
        if raw.is_empty() {
            return array![];
        }
        let mut out = array![];
        for part in raw.split(',') {
            out.push(coerce(part, elem)).expect("array push");
        }
        return out;
    }

    match type_token {
        "string" => JsonValue::String(raw.to_owned()),
        "bool" => JsonValue::Boolean(matches!(raw, "1" | "true" | "True" | "TRUE")),
        "float" | "double" => {
            if raw.is_empty() {
                JsonValue::from(0.0)
            } else {
                raw.parse::<f64>()
                    .map(JsonValue::from)
                    .unwrap_or_else(|_| JsonValue::String(raw.to_owned()))
            }
        }
        _ => {
            if raw.is_empty() {
                JsonValue::from(0)
            } else {
                raw.parse::<i64>()
                    .map(JsonValue::from)
                    .unwrap_or_else(|_| JsonValue::String(raw.to_owned()))
            }
        }
    }
}
