let map, markers, accuracyCircles;

let currentLocIdx = -1;
const locCache = new Array();

let currentId;
let globalPrivateKey;
let globalPrivateSigKey;
let globalAccessToken = "";

let newestPictureIndex;
let currentPictureIndex;
let totalPictures = 0;
let currentPictureRaw = null;

// Provide a minimal toast fallback when the library is not bundled.
const Toasted = window.Toasted || class {
    constructor(_opts) { }
    show(msg) { alert(msg); }
};

const KEYCODE_ENTER = 13;
const KEYCODE_ESCAPE = 27;
const KEYCODE_ARROW_LEFT = 37;
const KEYCODE_ARROW_RIGHT = 39;

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

window.addEventListener("load", (event) => init());

window.onclick = function (event) {
    // hide the dropdowns if clicking outside of their respective buttons
    const cam = document.getElementById("cameraDropDown");
    if (cam && event.target.id != "cameraDropDownButtonInner") {
        cam.style.display = "None";
    }
}

function init() {
    const versionView = document.getElementById('version');
    fetch("api/v1/version", {
        method: 'GET'
    })
        .then(function (response) {
            return response.text();
        })
        .then(function (versionCode) {
            versionView.textContent = versionCode;
        })

    setupOnClicks()
    checkWebCryptoApiAvailable()
    handleLoginFromUrl()
}

function setupOnClicks() {
    document.getElementById("loginForm").addEventListener("submit", async (event) => {
        // don't send a request to the server, we do it manually
        event.preventDefault();

        let rmdid = document.getElementById("rmdid").value;
        let password = document.getElementById("password").value;
        let useLongSession = document.getElementById("useLongSession").checked;
        await doLogin(rmdid, password, useLongSession);

        return false;
    });

const bindings = [
        ["locateOlder", () => locateOlder()],
        ["locateNewer", () => locateNewer()],
        ["locate", () => showLocateDropDown()],
        ["locateAll", () => sendToPhone("locate")],
        ["locateGps", () => sendToPhone("locate gps")],
        ["locateCellular", () => sendToPhone("locate cell")],
        ["locateLast", () => sendToPhone("locate last")],
        ["ring", () => sendToPhone("ring")],
        ["lock", () => sendToPhone("lock")],
        ["delete", () => prepareDeleteDevice()],
        ["cameraFront", () => sendToPhone("camera front")],
        ["cameraBack", () => sendToPhone("camera back")],
        ["showPicture", () => showLatestPicture()],
        ["pictureLatest", () => showLatestPicture()],
        ["picturePrev", () => showPreviousPicture()],
        ["pictureNext", () => showNextPicture()],
        ["pictureDownload", () => downloadCurrentPicture()],
        ["pushSave", () => savePushEndpoint()],
        ["pushRefresh", () => refreshPushStatus()],
        ["deleteAccount", () => deleteAccount()],
        ["exportData", () => exportData()]
    ];
    bindings.forEach(([id, fn]) => {
        const el = document.getElementById(id);
        if (el) el.addEventListener("click", () => fn());
    });
}

function checkWebCryptoApiAvailable() {
    if (window.crypto.subtle == undefined) {
        alert("RMD Server won't work because the WebCrypto API is not available.\n\n"
            + "This is most likely because you are visiting this site over insecure HTTP. "
            + "Please use HTTPS. If you are self-hosting, see the README.");
    }
}

// Section: Registration

function showRegisterDialog() {
    // If crypto or argon2 is unavailable (e.g., HTTP on mobile WebView), guide user to register via app or HTTPS.
    if (!window.crypto || !window.crypto.subtle || typeof argon2 === "undefined") {
        alert("Registration requires secure crypto support.\n\nUse the Android app to register, or open the portal over HTTPS/localhost.");
        return;
    }
    const existing = document.getElementById("registerDialog");
    if (existing) existing.remove();

    const overlay = document.createElement("div");
    overlay.id = "registerDialog";
    overlay.className = "overlay";

    const dialog = document.createElement("div");
    dialog.className = "dialog";
    overlay.appendChild(dialog);

    const title = document.createElement("h3");
    title.textContent = "Register";
    dialog.appendChild(title);

    const info = document.createElement("small");
    info.textContent = "Create a new account on this server. The server admin can see the RMD ID.";
    dialog.appendChild(info);

    const idInput = document.createElement("input");
    idInput.placeholder = "RMD ID (leave empty to auto-generate)";
    idInput.id = "registerId";
    dialog.appendChild(idInput);

    const pwInput = document.createElement("input");
    pwInput.placeholder = "Password";
    pwInput.type = "password";
    pwInput.id = "registerPassword";
    dialog.appendChild(pwInput);

    const tokenInput = document.createElement("input");
    tokenInput.placeholder = "Registration token (optional)";
    tokenInput.id = "registerToken";
    dialog.appendChild(tokenInput);

    const buttons = document.createElement("div");
    buttons.className = "dialog-actions";
    dialog.appendChild(buttons);

    const cancel = document.createElement("button");
    cancel.textContent = "Cancel";
    cancel.type = "button";
    cancel.addEventListener("click", () => overlay.remove());
    buttons.appendChild(cancel);

    const confirm = document.createElement("button");
    confirm.textContent = "Register";
    confirm.type = "button";
    confirm.addEventListener("click", async () => {
        const id = idInput.value.trim();
        const pw = pwInput.value;
        const token = tokenInput.value.trim();
        overlay.remove();
        await registerAccount(id, pw, token);
    });
    buttons.appendChild(confirm);

    document.body.appendChild(overlay);
}

async function registerAccount(requestedId, password, registrationToken) {
    if (!password || password.length < 8) {
        alert("Please enter a password with at least 8 characters.");
        return;
    }
    try {
        if (window.crypto && window.crypto.subtle && typeof argon2 !== "undefined") {
            const loginSalt = window.crypto.getRandomValues(new Uint8Array(ARGON2_SALT_LENGTH));
            const hashedPassword = await hashPasswordForLogin(password, loginSalt);
            const keypair = await generateKeysAndWrap(password);

            const response = await fetch("api/v1/device", {
                method: 'PUT',
                body: JSON.stringify({
                    Salt: base64Encode(loginSalt),
                    HashedPassword: hashedPassword,
                    PubKey: keypair.pubKey,
                    PrivKey: keypair.wrappedPrivKey,
                    RequestedUsername: requestedId,
                    RegistrationToken: registrationToken,
                }),
                headers: {
                    'Content-type': 'application/json'
                }
            });

            if (!response.ok) {
                const text = await response.text();
                alert("Registration failed: " + text);
                return;
            }
            const json = await response.json(); // contains DeviceId
            const newId = json.DeviceId || requestedId || "(auto-generated)";
            alert("Registration successful. Your RMD ID is: " + newId + "\nPlease log in with your password.");
            document.getElementById("rmdid").value = newId;
        } else {
            // Fallback: server-side registration with plain password (no client crypto).
            const response = await fetch("api/v1/device", {
                method: 'PUT',
                body: JSON.stringify({
                    PlainPassword: password,
                    RequestedUsername: requestedId,
                    RegistrationToken: registrationToken,
                }),
                headers: {
                    'Content-type': 'application/json'
                }
            });
            if (!response.ok) {
                const text = await response.text();
                alert("Registration failed: " + text);
                return;
            }
            const json = await response.json();
            const newId = json.DeviceId || requestedId || "(auto-generated)";
            alert("Registration successful. Your RMD ID is: " + newId + "\nPlease log in with your password.");
            document.getElementById("rmdid").value = newId;
        }
    } catch (err) {
        console.error(err);
        alert("Registration failed: " + (err.message || err));
    }
}

// Section: Login

const DURATION_DEFAULT_SECS = 15 * 60;      // 15 mins
const DURATION_LONG_SECS = 7 * 24 * 60 * 60 // 1 week

function handleLoginFromUrl() {
    const params = new URLSearchParams(window.location.search);
    const rmdid = params.get("rmdid");
    const password = params.get("password");
    if (rmdid && password) {
        document.getElementById("rmdid").value = rmdid;
        document.getElementById("password").value = password;
        doLogin(rmdid, password, false);
    }
}

async function doLogin(rmdid, password, useLongSession) {
    if (typeof argon2 === "undefined") {
        alert("Password hashing library failed to load.\n\nTry reloading, or open the portal via HTTPS/localhost, or use the Android app to register/login.");
        return;
    }
    let sessionDurationSeconds = DURATION_DEFAULT_SECS;
    if (useLongSession) {
        sessionDurationSeconds = DURATION_LONG_SECS;
    }

    currentId = rmdid;
    if (password == "") {
        alert("Password is empty.");
        return;
    }

    let response = await fetch("api/v1/salt", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: rmdid,
            Data: "unused",
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    let saltJson = await response.json();
    let salt = saltJson.Data;

    if (salt == "") {
        alert("This account does not exist.");
        return;
    }

    let passwordHash;
    try {
        passwordHash = await hashPasswordForLogin(password, salt);
    } catch (error) {
        console.log(error.message, error.code);
        alert("Failed to hash password!\n\n"
            + `The error was: ${error.message}\n\n`
            + "If you are using GrapheneOS with the Vanadium browser, please enable JavaScript JIT (needed for WebAssembly). "
            + "Then reload the page and try again.");
        return;
    }

    try {
        await tryLoginWithHash(rmdid, passwordHash, sessionDurationSeconds);
    } catch (statusCode) {
        if (statusCode == 423) {
            alert("Too many attempts. Try again in 10 minutes.");
        } else if (statusCode == 403) {
            alert("Wrong ID or wrong password.");
        } else {
            alert("Unhandled error: " + statusCode);
        }
        return;
    }

    try {
        await getPrivateKey(password);
    } catch (error) {
        console.log(error.message, error.code);
        alert("Failed to get private key!\n\n"
            + `The error was: ${error.message}`);
        return;
    }

    setupPushWarning();

    showAuthedUi();

    await locate(-1);
}

function showAuthedUi() {
    const dv = document.getElementById("dataView");
    if (dv) dv.classList.remove("hidden");
    const lf = document.getElementById("loginForm");
    if (lf) lf.classList.add("hidden");
    const idv = document.getElementById("idView");
    if (idv) idv.textContent = currentId || "";
    const pp = document.getElementById("picturePanel");
    if (pp) pp.classList.remove("hidden");
    const pushPanel = document.getElementById("pushPanel");
    if (pushPanel) pushPanel.classList.remove("hidden");
    refreshPushStatus();
    updatePushStatusFooter();
}

async function tryLoginWithHash(rmdid, passwordHash, sessionDurationSeconds) {
    const response = await fetch("api/v1/requestAccess", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: rmdid,
            Data: passwordHash,
            SessionDurationSeconds: sessionDurationSeconds,
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.ok) {
        const tokenJson = await response.json()
        globalAccessToken = tokenJson.Data;
        return globalAccessToken;
    } else {
        throw response.status;
    }
}

async function getPrivateKey(password) {
    const response = await fetch("api/v1/key", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: "unused",
        }),
        headers: {
            'Content-type': 'application/json'
        }
    })
    if (!response.ok) {
        throw response.status;
    }
    const keyData = await response.json();

    const [rsaEncKey, rsaSigKey] = await unwrapPrivateKey(password, keyData.Data);
    globalPrivateKey = rsaEncKey;
    globalPrivateSigKey = rsaSigKey;
}

async function redirectToLogin(toastMessage) {
    const toasted = new Toasted({
        position: 'top-center',
        duration: 3000
    })
    toasted.show(toastMessage);

    await sleep(3000);

    window.location.replace("/");
}

async function tokenExpiredRedirect() {
    redirectToLogin('Session expired, please log in again.');
}

// Section: Push Warning

async function setupPushWarning() {
    const pushUrl = await getPushUrl(globalAccessToken);
    const ele = document.getElementById("pushWarning");
    const statusEl = document.getElementById("pushStatus");
    if (!ele) return;
    ele.innerHTML = "";
    if (!pushUrl) {
        ele.innerHTML = `
            <p class="warning-text">
                UnifiedPush is not configured for this device.<br/>
                Open the RMD app and enable push to control the device from this portal.
            </p>
        `;
        if (statusEl) {
            statusEl.textContent = "Not configured";
            statusEl.style.color = "#eb6f92";
        }
    } else {
        if (statusEl) {
            statusEl.textContent = "Configured";
            statusEl.style.color = "var(--foam)";
        }
    }
}

async function refreshPushStatus() {
    const statusEl = document.getElementById("pushStatus");
    if (!statusEl || !globalAccessToken) return;
    try {
        const pushUrl = await getPushUrl(globalAccessToken);
        if (pushUrl && pushUrl.trim() !== "") {
            statusEl.textContent = "Configured";
            statusEl.style.color = "var(--foam)";
            const warn = document.getElementById("pushWarning");
            if (warn) warn.innerHTML = "";
            updatePushStatusFooter("Configured");
        } else {
            statusEl.textContent = "Not configured";
            statusEl.style.color = "#eb6f92";
            updatePushStatusFooter("Not configured");
        }
    } catch (e) {
        statusEl.textContent = "Push check failed";
        statusEl.style.color = "#eb6f92";
        updatePushStatusFooter("Push check failed");
    }
}

function updatePushStatusFooter(text) {
    const footerEl = document.getElementById("pushStatusFooter");
    if (!footerEl) return;
    footerEl.textContent = text || "";
}

async function savePushEndpoint() {
    if (!globalAccessToken) {
        alert("Log in first.");
        return;
    }
    const input = document.getElementById("pushEndpointInput");
    const msg = document.getElementById("pushSaveMsg");
    const endpoint = input ? input.value.trim() : "";
    if (!endpoint) {
        if (msg) msg.textContent = "Enter an endpoint.";
        return;
    }
    const response = await fetch("api/v1/push", {
        method: 'POST',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: endpoint,
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return;
    }
    if (!response.ok) {
        if (msg) msg.textContent = "Failed to save endpoint";
        return;
    }
    if (msg) msg.textContent = "Endpoint saved";
    await refreshPushStatus();
}

// Section: Locate

function showLocateDropDown() {
    document.getElementById("locateDropDown").style.display = "block";
}

async function locate(requestedIndex) {
    if (!globalAccessToken) {
        console.log("Missing accessToken!");
        return;
    }
    let response = await fetch("api/v1/locationDataSize", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: "unused",
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return
    }
    if (!response.ok) {
        throw response.status;
    }
    const locationDataSizeJson = await response.json();
    const highestLocIndex = Number.parseInt(locationDataSizeJson.Data, 10) - 1;

    if (requestedIndex > highestLocIndex) {
        currentLocIdx = highestLocIndex; // reset
        const toasted = new Toasted({
            position: 'top-center',
            duration: 3000
        })
        toasted.show('No newer locations');
        // Nothing to do, finish here
        return
    } else if (requestedIndex == -1) {
        currentLocIdx = highestLocIndex;
    } else {
        currentLocIdx = requestedIndex;
    }

    if (currentLocIdx < 0) {
        setNoLocationDataAvailable("The server has not yet stored a location for this device. Try requesting a location with the button below!");
        return
    }

    response = await fetch("api/v1/location", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: currentLocIdx.toString()
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (!response.ok) {
        throw response.status;
    }
    const locationData = await response.json();
    let selectedLoc;
    try {
        selectedLoc = await parseLocation(globalPrivateKey, locationData);
    } catch (error) {
        console.log(error);
        setNoLocationDataAvailable("Error parsing location data, see the dev console.");
        return;
    }
    // Check if location is already in cache
    // If not add the location and rearrange the items
    if (!locCache.find((val) => val["idx"] == currentLocIdx)) {
        locCache.push({
            "idx": currentLocIdx,
            "loc": selectedLoc,
        });
        locCache.sort((a, b) => a["idx"] - b["idx"]);
    }

    const selectedLocTime = new Date(selectedLoc.time);

    const idView = document.getElementById("idView");
    const locInfo = document.getElementById("locationInfo");
    if (idView) idView.textContent = currentId;
    if (locInfo) locInfo.textContent = `${selectedLoc.provider} on ${selectedLocTime.toLocaleDateString()} at ${selectedLocTime.toLocaleTimeString()}`;

    updateLocateOlderButton(currentLocIdx);
}

function updateLocateOlderButton(index) {
    const button = document.getElementById("locateOlder");
    if (!button) return;
    button.disabled = index <= 0;
}

function setNoLocationDataAvailable(reasonMessage) {
    const idView = document.getElementById("idView");
    const locInfo = document.getElementById("locationInfo");
    if (idView) idView.textContent = currentId;
    if (locInfo) locInfo.textContent = reasonMessage;
}

document.addEventListener("keydown", function (event) {
    // Don't interfere with navigating the map view
    if (document.activeElement.id != "map") {
        cycleThroughLocationsWithArrowKeys(event);
    }
});

function cycleThroughLocationsWithArrowKeys(event) {
    if (event.keyCode == KEYCODE_ARROW_LEFT) {
        locateOlder();
    } else if (event.keyCode == KEYCODE_ARROW_RIGHT) {
        locateNewer();
    }
}

async function locateOlder() {
    if (globalPrivateKey != null && currentLocIdx > 0) {
        currentLocIdx -= 1;
        await locate(currentLocIdx);
    } else {
        currentLocIdx = 0;
    }
}

async function locateNewer() {
    if (globalPrivateKey != null) {
        currentLocIdx += 1;
        await locate(currentLocIdx);
    }
}

// Section: Command

async function sendToPhone(message) {
    if (!globalAccessToken) {
        console.log("Missing accessToken!");
        return;
    }

    const time = Date.now();
    const sig = await sign(globalPrivateSigKey, `${time}:${message}`);

    const response = await fetch("api/v1/command", {
        method: 'POST',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: message,
            UnixTime: time,
            CmdSig: sig,
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return
    }
    if (!response.ok) {
        throw response.status;
    }
    const toasted = new Toasted({
        position: 'top-center',
        duration: 2000
    });
    toasted.show('Command send!');
}

async function showCommandLogs() {
    if (!globalAccessToken) {
        console.log("Missing accessToken!");
        return;
    }

    const response = await fetch("api/v1/commandLogs", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: "",
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return
    }
    if (!response.ok) {
        throw response.status;
    }
    const json = await response.json();
    const logs = await parseCommandLogs(globalPrivateKey, json.Data);
    displayCommandLogs(logs)
}
// Section: Picture

function updatePictureStatus(text) {
    const el = document.getElementById("pictureStatus");
    if (el) el.textContent = text || "";
}

function renderPicture(pictureBase64) {
    const img = document.getElementById("pictureImg");
    const empty = document.getElementById("pictureEmpty");
    if (!img || !empty) return;
    currentPictureRaw = pictureBase64;
    if (!pictureBase64) {
        img.classList.add("hidden");
        empty.classList.remove("hidden");
        updatePictureStatus("No photo");
        return;
    }
    img.src = "data:image/jpeg;base64," + pictureBase64;
    img.classList.remove("hidden");
    empty.classList.add("hidden");
    updatePictureStatus(`Image ${currentPictureIndex + 1} of ${totalPictures}`);
}

async function showLatestPicture() {
    if (!globalAccessToken) {
        console.log("Missing accessToken!");
        return;
    }
    const response = await fetch("api/v1/pictureSize", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: ""
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return
    }
    if (!response.ok) {
        throw response.status;
    }
    const json = await response.json();
    if (json.Data == "0") {
        renderPicture(null);
        return;
    }
    totalPictures = Number.parseInt(json.Data, 10);
    newestPictureIndex = totalPictures - 1;
    currentPictureIndex = newestPictureIndex;
    await loadPicture(currentPictureIndex);
}

async function loadPicture(index) {
    if (!globalAccessToken) {
        console.log("Missing accessToken!");
        return;
    }
    const response = await fetch("api/v1/picture", {
        method: 'PUT',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: index.toString()
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return
    }
    if (!response.ok) {
        throw response.status;
    }
    const data = await response.text();
    let picture = null;
    try {
        picture = await parsePicture(globalPrivateKey, data);
    } catch (e) {
        console.warn("parsePicture failed, falling back to raw", e);
        picture = data;
    }
    renderPicture(picture);
}

async function showPreviousPicture() {
    if (totalPictures <= 0) {
        renderPicture(null);
        return;
    }
    currentPictureIndex -= 1;
    if (currentPictureIndex < 0) currentPictureIndex = totalPictures - 1;
    await loadPicture(currentPictureIndex);
}

async function showNextPicture() {
    if (totalPictures <= 0) {
        renderPicture(null);
        return;
    }
    currentPictureIndex += 1;
    if (currentPictureIndex >= totalPictures) currentPictureIndex = 0;
    await loadPicture(currentPictureIndex);
}

function downloadCurrentPicture() {
    if (!currentPictureRaw) {
        const toasted = new Toasted({ position: 'top-center', duration: 2000 });
        toasted.show('No photo to download');
        return;
    }
    const link = document.createElement('a');
    link.href = "data:image/jpeg;base64," + currentPictureRaw;
    link.download = `rmd-photo-${currentPictureIndex + 1}.jpg`;
    document.body.appendChild(link);
    link.click();
    link.remove();
}

function displayCommandLogs(logs) {
    const div = document.createElement("div");
    div.classList.add("prompt");

    const logP = document.createElement("p");
    logP.id = "commandlogs"
    logP.innerHTML = logs;
    div.appendChild(logP);

    const buttonDiv = document.createElement("div");
    buttonDiv.className = "center";

    const btn = document.createElement("button");
    btn.textContent = "close";
    btn.addEventListener('click', function () {
        div.remove()
    }, false);
    buttonDiv.appendChild(btn);

    div.appendChild(buttonDiv);
    document.body.appendChild(div);
}

// Section: Camera

function showCameraDropDown() {
    document.getElementById("cameraDropDown").style.display = "block";
}

// Section: Settings

function showSettingsDropDown() {
    document.getElementById("settingsDropDown").style.display = "block";
}

// Section: Delete device

function prepareDeleteDevice() {
    const div = document.createElement("div");
    div.id = "passwordPrompt";
    div.classList.add("prompt");

    const label = document.createElement("label");
    label.id = "password_prompt_label";
    label.className = "center"
    label.textContent = "Please enter the device pin:";
    label.for = "password_prompt_input";
    div.appendChild(label);

    div.appendChild(document.createElement("br"));

    const centedInnerDiv = document.createElement("div");
    centedInnerDiv.className = "center";
    div.appendChild(centedInnerDiv);

    const input = document.createElement("input");
    input.id = "password_prompt_input";
    input.type = "password";
    centedInnerDiv.appendChild(input);

    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));

    document.body.appendChild(div);

    input.focus();
    input.addEventListener("keyup", function (event) {
        if (event.keyCode == KEYCODE_ENTER) {
            const pin = input.value;
            if (pin != "") {
                div.remove()
                sendToPhone("delete " + pin);
            }
        }
    }, false);
    window.addEventListener("keyup", function removeDeletePinDialog(event) {
        if (event.keyCode == KEYCODE_ESCAPE) {
            div.remove()
            window.removeEventListener("keyup", removeDeletePinDialog);
        }
    }, false);
}

// Section: Delete Account

async function deleteAccount() {
    if (!confirm("Do you really want to delete this account and all associated data from the server?")) {
        const toasted = new Toasted({
            position: 'top-center',
            duration: 3000
        })
        toasted.show("Account deletion cancelled");
        return;
    }
    if (!globalAccessToken) {
        console.log("Missing accessToken!");
        return;
    }
    const response = await fetch("api/v1/device", {
        method: 'POST',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: ""
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return;
    }
    if (!response.ok) {
        throw response.status;
    }
    redirectToLogin("Account deleted");
}

// Section: Export Data

async function exportData() {
    if (!globalAccessToken) {
        console.log("Missing accessToken!");
        return;
    }

    // Locations
    let response = await fetch("api/v1/locations", {
        method: 'POST',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: ""
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return;
    }
    if (!response.ok) {
        throw response.status;
    }
    let locationsCSV = "Date,Provider,Battery Percentage,Longitude,Latitude\n";
    const locationsAsJSON = await response.json();
    for (let locationJSON of locationsAsJSON) {
        const loc = await parseLocation(globalPrivateKey, JSON.parse(locationJSON))
        locationsCSV += new Date(loc.time).toISOString() + "," + loc.provider + "," + loc.bat + "," + loc.lon + "," + loc.lat + "\n"
    }

    // Pictures
    response = await fetch("api/v1/pictures", {
        method: 'POST',
        body: JSON.stringify({
            IDT: globalAccessToken,
            Data: ""
        }),
        headers: {
            'Content-type': 'application/json'
        }
    });
    if (response.status == 401) {
        tokenExpiredRedirect();
        return
    }
    if (!response.ok) {
        throw response.status;
    }
    const picturesAsJSON = await response.json();
    const pictures = [];
    for (let picture of picturesAsJSON) {
        const pic = await parsePicture(globalPrivateKey, picture);
        pictures.push(pic);
    }

    // General info
    const pushUrl = await getPushUrl(globalAccessToken);
    const generalInfo = {
        "rmdId": currentId,
        "pushUrl": pushUrl,
    };

    // ZIP everything
    const zip = new JSZip();
    zip.file("info.json", JSON.stringify(generalInfo));
    zip.file("locations.csv", locationsCSV);
    const picturesFolder = zip.folder("pictures");
    for (let [index, pic] of pictures.entries()) {
        picturesFolder.file(String(index) + ".png", pic, { base64: true });
    }
    const content = await zip.generateAsync({ type: "blob" });

    const formattedDate = new Date().toISOString().split('T')[0];

    const link = document.createElement('a');
    link.href = URL.createObjectURL(content);
    link.download = `rmd-export-${formattedDate}.zip`;

    // Append to the document and trigger download
    document.body.appendChild(link);
    link.click();

    // Clean up
    link.remove();
}
