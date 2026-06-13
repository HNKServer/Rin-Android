import { fetchCards } from "./api.mjs";
import { navigation } from "./navigation.mjs"

const urlParams = new URLSearchParams(window.location.search);

const cardGrid = document.querySelector(".card-grid");
const searchInput = document.getElementById("search-input");
const searchButton = document.getElementById("search-button");

let query = urlParams.get("query") || "";

function displayCards(cards) {
    if (!cards || !cards.current) {
        cardGrid.innerHTML = "<p>No cards found.</p>";
        return;
    }
    cards.current.forEach(card => {
        const cardElement = document.createElement("div");
        cardElement.classList.add("card");

        const image = document.createElement("img");
        image.src = `webui/images/card-thumbnails/t_${card.image}.webp`;
        image.alt = card.name;
        image.setAttribute("loading", "lazy");
        cardElement.appendChild(image);

        const strong = document.createElement("strong");
        strong.textContent = card.name;
        cardElement.appendChild(strong);

        const characterParagraph = document.createElement("p");
        const characterI = document.createElement("i");
        characterI.textContent = `Character: ${card.character}`;
        characterParagraph.appendChild(characterI);
        cardElement.appendChild(characterParagraph);

        const rarityParagraph = document.createElement("p");
        rarityParagraph.textContent = `Rarity: ${card.rarity}`;
        cardElement.appendChild(rarityParagraph);

        const attributeParagraph = document.createElement("p");
        attributeParagraph.textContent = `Attribute: ${card.attribute}`;
        cardElement.appendChild(attributeParagraph);

        cardGrid.appendChild(cardElement);
    });
}

const nav = new navigation(".page-navigation", handlePageChange);

async function handlePageChange(newPage, data, init) {
    if (!data) {
        data = await fetchCards(newPage, query);
    }
    cardGrid.innerHTML = "";
    displayCards(data);
    const q = typeof query === "string" && query.trim() ? `&query=${query}` : "";
    if (!init) window.history.pushState({}, "", `?page=${newPage}${q}`);
    window.scrollTo(0, 0);
}

const currentPage = parseInt(urlParams.get("page")) || 1;

async function init(first) {
    const initialData = await fetchCards(currentPage, query);
    if (initialData) {
        nav.createPagination(initialData.total_pages);
    } else {
        nav.clearPagination();
    }
    handlePageChange(currentPage, initialData, first);
    nav.setActiveButton(currentPage);
}

init(true);

// Search - just re-init with search query
async function search() {
    query = searchInput.value.trim();
    init(false);
}

searchButton.addEventListener("click", search);

searchInput.addEventListener("keydown", async (event) => {
    if (event.key === "Enter") {
        search();
    }
});

searchInput.value = query;
