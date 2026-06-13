import { getServerInfo, login } from "./api.mjs";
const form = document.querySelector("form");

function showError(message) {
    const modalTitle = document.getElementById("modal-title");
    const modalMessage = document.getElementById("modal-message");
    const modal = document.querySelector("dialog");

    modalTitle.textContent = "Error";
    modalMessage.textContent = message;
    modal.showModal();
}

document.querySelector(".close-button").addEventListener("click", () => {
    document.querySelector("dialog").close();
});

const uidElement = document.querySelector("form #id");
const passElement = document.querySelector("form #password");

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    let uid = uidElement.value.replaceAll(" ", "");
    let password = passElement.value;
    if (!uid || !password) {
        showError("Missing userid/password");
        return;
    }
    if (isNaN(uid)) {
        showError("UserID should be a number. (The \"Friend ID\" in your profile)");
        return;
    }
    if (window.localStorage) localStorage.setItem("uid", uid);
    try {
        await login(parseInt(uid), password);
    } catch(e) {
        showError(e.message);
    }
})

uidElement.value = localStorage.getItem("uid") || "";

try {
    const info = await getServerInfo();
    if (info.account_import === true) {
        document.querySelector(".sub").style.display = "inline-block";
    }
} catch(e) {
    console.warn(e);
    // Ignore error
}

document.getElementById("import-button").addEventListener("click", e => {
    e.preventDefault();
    window.location.href = "import.html";
})
