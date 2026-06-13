import { fetchMusic, clearRateHtmlSrc } from "./api.mjs";
import { navigation } from "./navigation.mjs"

const musicList = document.querySelector(".music-list");
const urlParams = new URLSearchParams(window.location.search);

let language = urlParams.get("lang") || localStorage.getItem("lang") || "EN";
let currentPage = parseInt(urlParams.get("page")) || 1;

function displayMusic(music) {
    if (!music || !music.current) {
        musicList.innerHTML = "<p>No songs found.</p>";
        return;
    }
    musicList.innerHTML = "";
    music.current.forEach(item => {
        const musicItem = document.createElement("div");
        musicItem.classList.add("music-item");
        musicItem.innerHTML = `
            <strong>${item.name}</strong>
            <p>Artist: ${item.artist}</p>
            <p>${item.detailInfo}</p>
            <p>Released On: ${item.releaseDateTime}</p>
        `;
        musicList.appendChild(musicItem);
    });
}

const nav = new navigation(".page-navigation", handlePageChange);

async function handlePageChange(newPage, data, init) {
    if (!data) {
        data = await fetchMusic(newPage, language);
    }
    musicList.innerHTML = "";
    displayMusic(data);
    window.scrollTo(0, 0);
    if (!init) updateHistory(newPage, language);
    currentPage = newPage;
}

function updateHistory(page, language) {
    window.history.pushState({}, "", `?page=${page}&lang=${language}`);
}

const languageButtons = document.querySelectorAll(".language-selector button");

function updateSelectedLanguage(lang, init) {
    languageButtons.forEach(btn => btn.classList.remove("active"));
    const newSelected = document.querySelector(`.language-selector button[data-lang="${lang}"]`);
    if (newSelected) {
        newSelected.classList.add("active");
    }
    language = lang;
    localStorage.setItem("lang", language);
    if (!init) {
        handlePageChange(currentPage);
        updateHistory(currentPage, language);
    }
}

languageButtons.forEach(button => {
    button.addEventListener("click", (e) => {
        updateSelectedLanguage(e.target.dataset.lang);
    });
});

async function init() {
    const data = await fetchMusic(currentPage, language);
    nav.createPagination(data.total_pages);
    nav.setActiveButton(currentPage);
    updateSelectedLanguage(language, true);
    handlePageChange(currentPage, data, true);
}

init();

document.querySelector(".clear-rate-link").href = clearRateHtmlSrc;
