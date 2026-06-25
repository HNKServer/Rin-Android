use jzon::{JsonValue, object};
use actix_web::{web, HttpRequest, HttpResponse};

use crate::encryption;
use crate::router::{userdata, global};
#[cfg(all(feature = "library", target_os = "android"))]
use crate::log_to_logcat;

pub fn routes(cfg: &mut web::ServiceConfig) {
    cfg.route("/start", web::post().to(start));
    cfg.route("/start/assetHash", web::post().to(asset_hash));
}

fn get_asset_hash(req: &HttpRequest, body: &JsonValue) -> String {
    if global::get_player_region(&body["asset_version"].to_string()).is_none() {
        println!("Warning! Asset version is not what was expected. (Did the app update?)");
    }

    let platform = req.headers()
        .get("aoharu-platform")
        .and_then(|v| v.to_str().ok())
        .map(global::parse_platform)
        .unwrap_or("Android");

    println!("Login on platform: {}", platform);

    let lang = body["language"].as_str()
        .or_else(|| body["lang"].as_str())
        .or_else(|| body["locale"].as_str())
        .or_else(|| req.headers().get("aoharu-language").and_then(|v| v.to_str().ok()))
        .unwrap_or("EN");

    global::get_asset_hash_for_lang(&body["asset_version"].to_string(), platform, lang)
        .or_else(|| global::get_asset_hash(&body["asset_version"].to_string(), platform))
        .unwrap()
}

pub async fn asset_hash(req: HttpRequest, body: String) -> HttpResponse {
    #[cfg(feature = "library")]
    #[cfg(target_os = "android")]
    log_to_logcat!("ew", "Handle: POST /api/start/assetHash");
    let body = jzon::parse(&encryption::decrypt_packet(&body).unwrap()).unwrap();

    global::api(&req, Some(object!{
        "asset_hash": get_asset_hash(&req, &body)
    }))
}

pub async fn start(req: HttpRequest, body: String) -> HttpResponse {
    #[cfg(feature = "library")]
    #[cfg(target_os = "android")]
    log_to_logcat!("ew", "Handle: POST /api/start");
    let key = global::get_login(req.headers(), &body);
    let body = jzon::parse(&encryption::decrypt_packet(&body).unwrap()).unwrap();
    let mut user = userdata::get_acc(&key);
    
    println!("Signin from uid: {}", user["user"]["id"].clone());
    
    user["user"]["last_login_time"] = global::timestamp().into();
    
    userdata::save_acc(&key, user);
    
    global::api(&req, Some(object!{
        "asset_hash": get_asset_hash(&req, &body),
        "token": hex::encode("Hello") //what is this?
    }))
}
