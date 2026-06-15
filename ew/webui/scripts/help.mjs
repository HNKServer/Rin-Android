import { getServerInfo } from "./api.mjs";

let downloadUrlElement = document.getElementById('downloadUrl');
let downloadUrliOSGLElement = document.getElementById('downloadUrliOSGL');
let downloadUrliOSJPElement = document.getElementById('downloadUrliOSJP');
let assetUrlElements = document.querySelectorAll('.assetUrl');
let locationElements = document.querySelectorAll(".windowlocation");


try {
    const info = await getServerInfo();
    if (!info.links) {
        throw new Error("No links are set.");
    }
    //console.log(info);
    if (info.links.global && info.links.japan) {
        let linkHtml = `Your server admin has a link to download! Download <a href="${info.links.japan}" target="_blank">Japan</a> or <a href="${info.links.global}" target="_blank">Global</a>`;
        downloadUrlElement.innerHTML = linkHtml;
    } else if (info.links.global) {
        let linkHtml = `Your server admin has a link to download! Download <a href="${info.links.global}" target="_blank">Global</a>`;
        downloadUrlElement.innerHTML = linkHtml;
    } else if (info.links.japan) {
        let linkHtml = `Your server admin has a link to download! Download <a href="${info.links.japan}" target="_blank">Japan</a>`;
        downloadUrlElement.innerHTML = linkHtml;
    }

    if (info.links.assets) {
        assetUrlElements.forEach(elem => {
            elem.textContent = info.links.assets;
        })
    }
    
    if (info.links.ios) {
        if (info.links.ios.japan) {
            downloadUrliOSJPElement.src = info.links.ios.japan;
        }

        if (info.links.ios.global) {
            downloadUrliOSGLElement.src = info.links.ios.global;
        }
    }
} catch(e) {
    //Ignore error
}

locationElements.forEach(element => {
    element.textContent = window.location.origin;
})
