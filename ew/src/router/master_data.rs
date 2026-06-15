use actix_web::{web, HttpRequest, HttpResponse, Responder};
use actix_web::http::header::ContentType;
use crate::router::databases::csv::{get_all, Region};

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
        Region::ZhCht => "ZH-CHT",
    }
}

async fn mst(req: HttpRequest) -> impl Responder {
    let lang = req.match_info().get("LANG").unwrap_or("JP");
    let body = get_all(region_from_lang(lang));
    let body = jzon::stringify(body);
    HttpResponse::Ok()
        .insert_header(("content-type", ContentType::json()))
        .insert_header(("content-length", body.len()))
        .body(body)
}

async fn supported() -> impl Responder {
    "SUPPORTED"
}
