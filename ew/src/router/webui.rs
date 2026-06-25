use actix_web::{
    HttpResponse,
    HttpRequest,
    http::header::HeaderValue,
    http::header::ContentType
};
use jzon::{JsonValue, object};
use lazy_static::lazy_static;
use include_dir::{include_dir, Dir};
use std::{collections::HashMap, fs};

use crate::include_file;
use crate::router::{userdata, items};
use crate::router::databases::csv::Region;

fn get_config() -> JsonValue {
    let args = crate::get_args();
    object!{
        import: !args.disable_imports,
        export: !args.disable_exports
    }
}

fn get_login_token(req: &HttpRequest) -> Option<String> {
    let blank_header = HeaderValue::from_static("");
    let cookies = req.headers().get("Cookie").unwrap_or(&blank_header).to_str().unwrap_or("");
    if cookies.is_empty() {
        return None;
    }
    Some(cookies.split("ew_token=").last().unwrap_or("").split(';').collect::<Vec<_>>()[0].to_string())
}

fn error(msg: &str) -> HttpResponse {
    let resp = object!{
        result: "ERR",
        message: msg
    };
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}

pub fn login(_req: HttpRequest, body: String) -> HttpResponse {
    let body = jzon::parse(&body).unwrap();
    let token = userdata::webui_login(body["uid"].as_i64().unwrap(), &body["password"].to_string());
    
    if token.is_err() {
        return error(&token.unwrap_err());
    }
    
    let resp = object!{
        result: "OK"
    };
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .insert_header(("Set-Cookie", format!("ew_token={}; SameSite=Strict; HttpOnly", token.unwrap())))
        .body(jzon::stringify(resp))
}

pub fn import(_req: HttpRequest, body: String) -> HttpResponse {
    if !get_config()["import"].as_bool().unwrap() {
        return error("Importing accounts is disabled on this server.");
    }
    let body = jzon::parse(&body).unwrap();
    
    let result = userdata::webui_import_user(body);
    
    if result.is_err() {
        return error(&result.unwrap_err());
    }
    let result = result.unwrap();
    
    let resp = object!{
        result: "OK",
        uid: result["uid"].clone(),
        migration_token: result["migration_token"].clone()
    };
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}

pub fn user(req: HttpRequest) -> HttpResponse {
    let token = get_login_token(&req);
    if token.is_none() {
        return error("Not logged in");
    }
    let data = userdata::webui_get_user(&token.unwrap());
    if data.is_none() {
        return error("Expired login");
    }
    let mut data = data.unwrap();
    
    data["userdata"]["user"]["rank"] = items::get_user_rank_data(data["userdata"]["user"]["exp"].as_i64().unwrap())["rank"].clone();
    
    let resp = object!{
        result: "OK",
        data: data
    };
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}

pub fn start_loginbonus(req: HttpRequest, body: String) -> HttpResponse {
    let token = get_login_token(&req);
    if token.is_none() {
        return error("Not logged in");
    }
    let body = jzon::parse(&body).unwrap();
    let resp = userdata::webui_start_loginbonus(body["bonus_id"].as_i64().unwrap(), &token.unwrap());
    
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}

pub fn set_time(req: HttpRequest, body: String) -> HttpResponse {
    let token = get_login_token(&req);
    if token.is_none() {
        return error("Not logged in");
    }
    let body = jzon::parse(&body).unwrap();
    let resp = userdata::set_server_time(body["timestamp"].as_i64().unwrap(), &token.unwrap());
    
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}

pub fn logout(req: HttpRequest) -> HttpResponse {
    let token = get_login_token(&req);
    if token.is_some() {
        userdata::webui_logout(&token.unwrap());
    }
    let resp = object!{
        result: "OK"
    };
    HttpResponse::Found()
        .insert_header(ContentType::json())
        .insert_header(("Set-Cookie", "ew_token=deleted; expires=Thu, 01 Jan 1970 00:00:00 GMT"))
        .insert_header(("Location", "/login.html"))
        .body(jzon::stringify(resp))
}

static WEBUI_ASSETS: Dir<'_> = include_dir!("webui/");

pub fn main(req: HttpRequest) -> HttpResponse {
    let path = if req.path().ends_with("/") { format!("{}index.html", req.path()) } else { req.path().to_string() };
    let mut chars = path.chars();
    chars.next();
    let path = chars.as_str();

    if path == "login.html" {
        let token = get_login_token(&req);
        if token.is_some() {
            let data = userdata::webui_get_user(&token.unwrap());
            if data.is_some() {
                return HttpResponse::Found()
                    .insert_header(("Location", "/account.html"))
                    .body("");
            }
        }
    }

    if let Some(file) = WEBUI_ASSETS.get_file(&path)
        // include_dir!("webui/") stores paths as images/..., scripts/..., etc.
        // Browser requests, however, may be /webui/images/....
        // Try the original path first, then the webui/stripped path.  This keeps
        // the original 303 fallback intact and only adds a local hit before it.
        .or_else(|| path.strip_prefix("webui/").and_then(|p| WEBUI_ASSETS.get_file(p)))
    {
        let body = file.contents();
        let mime = mime_guess::from_path(path).first_or_octet_stream();
        return HttpResponse::Ok()
            .insert_header(ContentType(mime))
            .insert_header(("content-length", body.len()))
            .body(body);
    } else if path.starts_with("webui/images/card-thumbnails") {
        let args = crate::get_args();

        let file_name = path.split("/").last().unwrap_or("");
        let mut candidates = vec![
            // Desktop/dev convenience when the downloader has populated
            // webui/images/card-thumbnails in the working tree.
            path.to_string(),
        ];
        if args.image_asset_path != "" {
            candidates.extend([
                format!("{}/{}", args.image_asset_path, file_name).replace("//", "/"),
                format!("{}/card-thumbnails/{}", args.image_asset_path, file_name).replace("//", "/"),
                format!("{}/images/card-thumbnails/{}", args.image_asset_path, file_name).replace("//", "/"),
                format!("{}/webui/images/card-thumbnails/{}", args.image_asset_path, file_name).replace("//", "/"),
            ]);
        }

        return if let Some(body) = candidates.iter().find_map(|p| fs::read(p).ok()) {
            let mime = mime_guess::from_path(path).first_or_octet_stream();
            HttpResponse::Ok()
                .insert_header(ContentType(mime))
                .insert_header(("content-length", body.len()))
                .body(body)
        } else {
            if args.image_asset_path != "" {
                println!("File '{file_name}' was requested, but no file was found on the disk!");
            }
            HttpResponse::SeeOther()
                .insert_header(("location", format!("https://sif2-api.ethanthesleepy.one{}", req.path())))
                .body("")
        }
    }

    HttpResponse::Found()
        .insert_header(("Location", "/"))
        .body("")
}

pub fn export(req: HttpRequest) -> HttpResponse {
    if !get_config()["export"].as_bool().unwrap() {
        return error("Exporting accounts is disabled on this server.");
    }
    let token = get_login_token(&req);
    if token.is_none() {
        return error("Not logged in");
    }
    let resp = object!{
        result: "OK",
        data: userdata::export_user(&token.unwrap()).unwrap()
    };
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}

pub fn server_info(_req: HttpRequest) -> HttpResponse {
    let args = crate::get_args();

    let resp = object!{
        result: "OK",
        data: {
            account_import: get_config()["import"].as_bool().unwrap(),
            links: {
                global: args.global_android,
                japan: args.japan_android,
                ios: {
                    global: args.global_ios,
                    japan: args.japan_ios
                },
                assets: args.assets_url
            }
        }
    };
    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}


fn webui_region_from_lang(lang: &str) -> Region {
    match lang.to_ascii_uppercase().as_str() {
        "EN" => Region::En,
        "KR" | "KO" => Region::Kr,
        "ZH" | "ZH-CHT" | "ZH_HANT" | "ZH-HANT" | "ZH-TW" | "ZH-HK" => Region::ZhCht,
        _ => Region::Jp,
    }
}

fn card_attribute_name(attr: i64) -> &'static str {
    match attr { 1 => "Smile", 2 => "Pure", 3 => "Cool", _ => "" }
}

fn card_rarity_name(rarity: i64) -> &'static str {
    match rarity { 1 => "R", 2 => "SR", 3 => "UR", _ => "" }
}

fn character_name_map(region: Region) -> HashMap<i64, String> {
    let characters = crate::router::databases::csv::table(region, "character");
    let mut map = HashMap::new();
    for c in characters.members() {
        if let Some(id) = c["id"].as_i64() {
            map.insert(id, c["name"].to_string());
        }
    }
    map
}

fn enrich_card(mut card: JsonValue, chars: &HashMap<i64, String>) -> JsonValue {
    card["image"] = card["illustId"].clone();
    if let Some(cid) = card["masterCharacterId"].as_i64() {
        card["character"] = chars.get(&cid).cloned().unwrap_or_default().into();
    }
    if let Some(r) = card["rarity"].as_i64() {
        card["rarityName"] = card_rarity_name(r).into();
    }
    if let Some(t) = card["type"].as_i64() {
        card["attribute"] = card_attribute_name(t).into();
    }
    card
}

fn decode_query_component(value: &str) -> String {
    let plus_as_space = value.replace('+', " ");
    urlencoding::decode(&plus_as_space)
        .map(|v| v.into_owned())
        .unwrap_or(plus_as_space)
}

fn get_query_str(req: &HttpRequest, key: &str, def: &str) -> String {
    for part in req.query_string().split('&') {
        let Some((k, v)) = part.split_once('=') else { continue; };
        if k == key {
            return decode_query_component(v);
        }
    }
    def.to_string()
}

fn normalize_search_text(value: &str) -> String {
    value.trim().to_lowercase()
}

fn push_card_search_term(out: &mut String, value: &JsonValue) {
    let s = value.to_string();
    if !s.is_empty() {
        out.push(' ');
        out.push_str(&normalize_search_text(&s));
    }
}

fn card_search_index() -> HashMap<i64, String> {
    let mut index: HashMap<i64, String> = HashMap::new();
    for region in [Region::Jp, Region::En, Region::Kr, Region::ZhCht] {
        let cards = crate::router::databases::csv::table(region, "card");
        let chars = character_name_map(region);
        for item in cards.members() {
            let card = enrich_card(item.clone(), &chars);
            let Some(id) = card["id"].as_i64() else { continue; };
            let entry = index.entry(id).or_default();
            push_card_search_term(entry, &card["id"]);
            push_card_search_term(entry, &card["name"]);
            push_card_search_term(entry, &card["character"]);
            push_card_search_term(entry, &card["rarityName"]);
            push_card_search_term(entry, &card["attribute"]);
            push_card_search_term(entry, &card["masterCharacterId"]);
            push_card_search_term(entry, &card["illustId"]);
        }
    }
    index
}

pub fn get_card_info(req: HttpRequest) -> HttpResponse {
    let page = get_query_str(&req, "page", "1").parse::<usize>().unwrap_or(1) - 1;
    let max = get_query_str(&req, "max", "10").parse::<usize>().unwrap_or(10);
    let all = get_query_str(&req, "all", "false");
    let name_query = get_query_str(&req, "query", "");
    let lang = get_query_str(&req, "lang", "JP");
    let region = webui_region_from_lang(&lang);
    let start = page * max;
    let items = crate::router::databases::csv::table(region, "card");
    let chars = character_name_map(region);
    let mut enriched: Vec<JsonValue> = items.members().map(|item| enrich_card(item.clone(), &chars)).collect();
    if all == "true" {
        let resp = object!{ total_pages: 1, current: enriched };
        return HttpResponse::Ok().content_type(ContentType::json()).body(jzon::stringify(resp));
    }
    if !name_query.is_empty() {
        let query = normalize_search_text(&name_query);
        let search_index = card_search_index();
        enriched.retain(|item| {
            let mut local_terms = String::new();
            push_card_search_term(&mut local_terms, &item["id"]);
            push_card_search_term(&mut local_terms, &item["name"]);
            push_card_search_term(&mut local_terms, &item["character"]);
            push_card_search_term(&mut local_terms, &item["rarityName"]);
            push_card_search_term(&mut local_terms, &item["attribute"]);
            let global_terms = item["id"].as_i64()
                .and_then(|id| search_index.get(&id))
                .map(|s| s.as_str())
                .unwrap_or("");
            local_terms.contains(&query) || global_terms.contains(&query)
        });
    }
    let total_len = enriched.len();
    let page_items: Vec<_> = enriched.into_iter().skip(start).take(max).collect();
    if page_items.is_empty() { return HttpResponse::NotFound().finish(); }
    let total_pages = (total_len as f64 / max as f64).ceil() as usize;
    let args = crate::get_args();
    let resp = object!{ total_pages: total_pages, current: page_items, image_path: args.image_asset_path };
    HttpResponse::Ok().content_type(ContentType::json()).body(jzon::stringify(resp))
}

pub fn get_music_info(req: HttpRequest) -> HttpResponse {
    let page = get_query_str(&req, "page", "1").parse::<usize>().unwrap_or(1) - 1;
    let max = get_query_str(&req, "max", "10").parse::<usize>().unwrap_or(10);
    let lang = get_query_str(&req, "lang", "JP");
    let region = webui_region_from_lang(&lang);
    let start = page * max;
    let items = crate::router::databases::csv::table(region, "music");
    let page_items: Vec<_> = items.members().skip(start).take(max).cloned().collect();
    if page_items.is_empty() { return HttpResponse::NotFound().finish(); }
    let total_items = items.len();
    let total_pages = (total_items as f64 / max as f64).ceil() as usize;
    let resp = object!{ total_pages: total_pages, current: page_items };
    HttpResponse::Ok().content_type(ContentType::json()).body(jzon::stringify(resp))
}

lazy_static! {
    static ref ITEM: JsonValue = jzon::parse(&include_file!("src/router/webui/item.json")).unwrap();
    static ref LOGIN_BONUS: JsonValue = jzon::parse(&include_file!("src/router/webui/login_bonus.json")).unwrap();
}

pub fn list_login_bonus(_req: HttpRequest) -> HttpResponse {
    HttpResponse::Ok()
        .content_type(ContentType::json())
        .body(jzon::stringify(LOGIN_BONUS.clone()))
}

pub fn list_items(_req: HttpRequest) -> HttpResponse {
    HttpResponse::Ok()
        .content_type(ContentType::json())
        .body(jzon::stringify(ITEM.clone()))
}

pub fn cheat(req: HttpRequest, _body: String) -> HttpResponse {
    let token = get_login_token(&req);
    if token.is_none() {
        return error("Not logged in");
    }
    let key = userdata::webui_login_token(&token.unwrap());
    if key.is_none() {
        return error("Not logged in");
    }
    let key = key.unwrap();
    let mut user = userdata::get_acc_home(&key);

    for item in ITEM.entries() {
        let id = item.0.parse::<i32>().unwrap_or(0);
        let data = item.1;
        if id == 0 {
            continue;
        }
        let reward_type = data["reward_type"].as_i32().unwrap();
        let limit = if reward_type == 4 {
            items::LIMIT_COINS
        } else if reward_type == 1 {
            items::LIMIT_PRIMOGEMS
        } else {
            items::LIMIT_ITEMS
        };
        items::gift_item_basic(id, limit, reward_type, "You have cheated. Here are \"gifts\".", &mut user);
    }

    userdata::save_acc_home(&key, user);

    let resp = object!{
        result: "OK"
    };

    HttpResponse::Ok()
        .insert_header(ContentType::json())
        .body(jzon::stringify(resp))
}
