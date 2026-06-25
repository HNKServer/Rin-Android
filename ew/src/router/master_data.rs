use actix_web::{web, HttpRequest, HttpResponse, Responder};
use actix_web::http::header::ContentType;
use crate::router::databases::csv::{get_all, Region};
use crate::router::global;
use jzon::object;

pub fn routes(cfg: &mut web::ServiceConfig) {
    cfg.service(
        web::scope("/masterdata")
            .route("/supported", web::get().to(supported))
            .route("/{platform}/{LANG}", web::get().to(mst))
    );
}

pub fn region_from_lang(lang: &str) -> Region {
    let normalized = lang.trim().replace('_', "-").to_ascii_uppercase();
    match normalized.as_str() {
        "JP" | "JA" | "JA-JP" | "JAPANESE" => Region::Jp,
        "KR" | "KO" | "KO-KR" | "KOREAN" => Region::Kr,
        "ZH" | "ZH-CHT" | "ZH-HANT" | "ZH-TW" | "ZH-HK" | "ZH-MO" | "TC" | "TRADITIONAL-CHINESE" => Region::ZhCht,
        _ => Region::En,
    }
}

pub fn canonical_lang(lang: &str) -> &'static str {
    match region_from_lang(lang) {
        Region::Jp => "JP",
        Region::En => "EN",
        Region::Kr => "KR",
        Region::ZhCht => "ZH",
    }
}

async fn mst(req: HttpRequest) -> impl Responder {
    let lang = req.match_info().get("LANG").unwrap_or("JP");
    let region = region_from_lang(lang);

    // JP-safe mode:
    // The original JP client already has working JP masterdata / online CDN data.
    // Returning server-side JP masterdata here changes startup tables such as
    // url/resource_download/title_screen and was observed in log18 as
    // "[MST] replaced ... from server" followed by a TAP-time communication error.
    //
    // For JP requests, intentionally return an empty masterdata object so the
    // client-side hook behaves like the original successful run: no server MST
    // entry is found for each table, and the client falls back to its own data.
    //
    // EN/KR/ZH-CHT still receive server-side translated masterdata.
    let body = if region == Region::Jp {
        object!{}
    } else {
        get_all(region)
    };
    let body = jzon::stringify(body);
    HttpResponse::Ok()
        .insert_header(("content-type", ContentType::json()))
        .insert_header(("content-length", body.len()))
        .body(body)
}

fn supports_extra_game_data(req: &HttpRequest) -> bool {
    // JP-safe mode:
    // The successful original AndroidEw JP run reported masterdata support as false.
    // Returning supported=true changes the hook startup path even if /JP later
    // returns an empty object.  Only advertise support when the request clearly
    // identifies a non-JP client/language.
    if let Some(asset_version) = req.headers()
        .get("aoharu-asset-version")
        .and_then(|v| v.to_str().ok())
    {
        match global::get_player_region(asset_version).as_deref() {
            Some("JP") => return false,
            Some("GL") => return true,
            _ => {}
        }
    }

    if let Some(lang) = req.headers()
        .get("aoharu-language")
        .and_then(|v| v.to_str().ok())
    {
        return region_from_lang(lang) != Region::Jp;
    }

    false
}

async fn supported(req: HttpRequest) -> HttpResponse {
    if supports_extra_game_data(&req) {
        HttpResponse::Ok().body("SUPPORTED")
    } else {
        HttpResponse::NotFound().body("")
    }
}
