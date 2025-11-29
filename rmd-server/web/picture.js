async function parsePicture(rsaCryptoKey, pictureData) {
    try {
        const picture = await decryptPacket(rsaCryptoKey, pictureData);
        return picture
    } catch (e) {
        // Fallback: if it was stored as plain base64 (no encryption), return as-is.
        console.warn("Picture decrypt failed, returning raw data", e);
        return pictureData;
    }
}
