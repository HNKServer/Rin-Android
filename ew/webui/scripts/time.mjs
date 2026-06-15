import { setTime } from "./api.mjs";

function showError(message) {
    const modalTitle = document.getElementById("modal-title");
    const modalMessage = document.getElementById("modal-message");
    const modal = document.querySelector("dialog");

    modalTitle.textContent = "Error";
    modalMessage.textContent = message;
    modal.showModal();
}

document.querySelector(".close-button").addEventListener("click", () => {
    window.location.href = "account.html";
});

const newTime = document.getElementById("new-time");
const input = document.getElementById("user-input");

const urlParams = new URLSearchParams(window.location.search);

let query = urlParams.get("servertime") || "";
query = query.trim();

if (query === "") {
    window.location.href = "account.html";
}
let time = Math.round(new Date(query).getTime() / 1000);
if (query === "-1") {
    time = 1711741114;
} else if (query === "0") {
    time = 0;
}
if (time < 0 || isNaN(time)) {
    showError(`Invalid input "${query}"`);
    throw new Error("Invalid Time");
}

input.textContent = query;

try {
    let resp = await setTime(time);
    console.log(resp);
} catch(e) {
    showError(`Error: ${e.message}`);
    throw e;
}

if (time === 0) {
    newTime.textContent = "now";
} else {
    newTime.textContent = (new Date(time * 1000)).toString();
}
