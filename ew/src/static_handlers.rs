use actix_web::{get, HttpResponse, HttpRequest, http::header::ContentType, web, guard};
use std::fs;
use std::path::{Path, PathBuf};
use crate::router::master_data::canonical_lang;

#[get("/maintenance/maintenance.json")]
async fn maintenance(_req: HttpRequest) -> HttpResponse {
    let root = crate::runtime::get_asset_path();
    let local = Path::new(&root).join("maintenance/maintenance.json");
    if let Ok(bytes) = fs::read(&local) {
        return HttpResponse::Ok()
            .insert_header(ContentType(mime::APPLICATION_JSON))
            .body(bytes);
    }
    HttpResponse::Ok()
        .insert_header(ContentType(mime::APPLICATION_JSON))
        .body(r#"{"opened_at":"2024-02-05 02:00:00","closed_at":"2024-02-05 04:00:00","message":":(","server":1,"gamelib":0}"#)
}

fn safe_join(base: &Path, parts: &[&str]) -> Option<PathBuf> {
    let mut out = base.to_path_buf();
    for part in parts {
        let p = part.trim_start_matches('/');
        if p.is_empty() { continue; }
        if Path::new(p).components().any(|c| matches!(c, std::path::Component::ParentDir)) {
            return None;
        }
        out.push(p);
    }
    Some(out)
}

#[cfg(feature = "library")]
use include_dir::{include_dir, Dir};

#[cfg(all(feature = "library", target_os = "ios"))]
static SPART_FILES: Dir<'_> = include_dir!("assets/iOS/");

#[cfg(all(feature = "library", target_os = "android"))]
static SPART_FILES: Dir<'_> = include_dir!("assets/Android/");

fn lang_aliases(lang: &str) -> Vec<&'static str> {
    match canonical_lang(lang) {
        "ZH" => vec!["ZH-CHT", "ZH", "zh-cht", "zh", "ZH-HANT", "ZH-TW", "ZH-HK"],
        "KR" => vec!["KR", "ko", "KO", "ko-KR"],
        "EN" => vec!["EN", "en", "en-US"],
        "JP" => vec!["JP", "jp", "JA", "ja", "ja-JP"],
        _ => vec!["EN"],
    }
}

fn file_candidates(root: &Path, platform: &str, lang: Option<&str>, hash: &str, file_name: &str) -> Vec<PathBuf> {
    let mut out = Vec::new();
    if let Some(lang) = lang {
        for alias in lang_aliases(lang) {
            if let Some(p) = safe_join(root, &[platform, alias, hash, file_name]) { out.push(p); }
            if let Some(p) = safe_join(root, &[&format!("{alias}-{platform}"), hash, file_name]) { out.push(p); }
            if let Some(p) = safe_join(root, &[alias, platform, hash, file_name]) { out.push(p); }
        }
    } else {
        // JP layout from the original docs: assets/{Android,iOS}/{hash}/{file}
        if let Some(p) = safe_join(root, &[platform, hash, file_name]) { out.push(p); }
        for alias in lang_aliases("JP") {
            if let Some(p) = safe_join(root, &[platform, alias, hash, file_name]) { out.push(p); }
            if let Some(p) = safe_join(root, &[&format!("{alias}-{platform}"), hash, file_name]) { out.push(p); }
            if let Some(p) = safe_join(root, &[alias, platform, hash, file_name]) { out.push(p); }
        }
    }
    out
}

fn handle_assets(req: HttpRequest) -> HttpResponse {
    let platform: String = req.match_info().get("platform").unwrap_or("Android").parse().unwrap_or(String::from("Android"));
    let lang: Option<String> = req.match_info().get("lang").map(|s| s.to_string());
    let file_name: String = req.match_info().get("file").unwrap_or("").parse().unwrap_or_default();
    let hash: String = req.match_info().get("hash").unwrap_or("").parse().unwrap_or_default();

    #[cfg(feature = "library")]
    {
        if let Some(lang) = &lang {
            for alias in lang_aliases(lang) {
                if let Some(file) = SPART_FILES.get_file(format!("{alias}/{hash}/{file_name}")) {
                    let body = file.contents();
                    return HttpResponse::Ok()
                        .insert_header(ContentType(mime::APPLICATION_OCTET_STREAM))
                        .insert_header(("content-length", body.len()))
                        .body(body);
                }
            }
        } else if let Some(file) = SPART_FILES.get_file(format!("{hash}/{file_name}")) {
            let body = file.contents();
            return HttpResponse::Ok()
                .insert_header(ContentType(mime::APPLICATION_OCTET_STREAM))
                .insert_header(("content-length", body.len()))
                .body(body);
        }
    }

    let root_s = crate::runtime::get_asset_path();
    let root = Path::new(&root_s);
    for candidate in file_candidates(root, &platform, lang.as_deref(), &hash, &file_name) {
        if let Ok(contents) = fs::read(&candidate) {
            return HttpResponse::Ok()
                .insert_header(ContentType(mime::APPLICATION_OCTET_STREAM))
                .insert_header(("content-length", contents.len()))
                .body(contents);
        }
    }

    HttpResponse::SeeOther()
        .insert_header(("location", format!("https://sif2.sif.moe{}", req.path())))
        .body("")
}

async fn files_jp(req: HttpRequest) -> HttpResponse { handle_assets(req) }
async fn files_gl(req: HttpRequest) -> HttpResponse { handle_assets(req) }

fn platform_guard(ctx: &guard::GuardContext) -> bool {
    let platform = ctx.head().uri.path()
        .trim_start_matches('/')
        .split('/')
        .next()
        .unwrap_or("");
    matches!(platform, "Android" | "StandaloneWindows64" | "iOS")
}

pub fn routes(cfg: &mut web::ServiceConfig) {
    cfg.service(
        web::resource("/{platform}/{hash}/{file}")
            .guard(guard::fn_guard(platform_guard))
            .route(web::get().to(files_jp))
    )
    .service(
        web::resource("/{platform}/{lang}/{hash}/{file}")
            .guard(guard::fn_guard(platform_guard))
            .route(web::get().to(files_gl))
    );
}
