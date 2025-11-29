package backend

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"

	"rmd-server/user"
	"rmd-server/version"

	"github.com/rs/zerolog/log"
	"golang.org/x/crypto/argon2"
)

const HEADER_CONTENT_TYPE = "Content-Type"
const CT_APPLICATION_JSON = "application/json"
const CT_TEXT_JAVASCRIPT = "text/javascript"

const ERR_ACCESS_TOKEN_INVALID = "Access token not valid"
const ERR_JSON_INVALID = "Invalid JSON"

type registrationData struct {
	Salt              string
	HashedPassword    string
	PubKey            string
	PrivKey           string
	RequestedUsername string
	RegistrationToken string
	PlainPassword     string
}

type passwordUpdateData struct {
	IDT            string
	Salt           string
	HashedPassword string
	PrivKey        string
	PlainPassword  string
}

// This is historically grown, and was originally a DataPackage
type loginData struct {
	IDT                    string
	PasswordHash           string `json:"Data"`
	SessionDurationSeconds uint64
	PlainPassword          string
}

// suboptimal naming for backwards compatibility
type commandData struct {
	IDT      string // access token
	Data     string // plaintext command
	UnixTime uint64 // unix time in milliseconds
	CmdSig   string // base64-encoded signature over "UnixTime:Data"
}

// universal package for string transfer
// IDT = DeviceID or AccessToken
// If both will be send. ID is always IDT
type DataPackage struct {
	IDT  string
	Data string
}

// ------- Location -------

func getLocation(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}
	index, _ := strconv.Atoi(request.Data)
	if index == -1 {
		index = uio.GetLocationSize(user)
	}
	data := uio.GetLocation(user, index)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write([]byte(fmt.Sprint(string(data))))
}

func getAllLocations(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}
	data := uio.GetAllLocations(user)
	jsonData, err := json.Marshal(data)
	if err != nil {
		http.Error(w, "Failed to export data", http.StatusConflict)
		return
	}
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write([]byte(fmt.Sprint(string(jsonData))))
}

func postLocation(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	locationAsString, _ := json.MarshalIndent(request, "", " ")
	uio.AddLocation(user, string(locationAsString))
	w.WriteHeader(http.StatusOK)
}

func getLocationDataSize(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	size := uio.GetLocationSize(user)

	dataSize := DataPackage{Data: strconv.Itoa(size)}
	result, _ := json.Marshal(dataSize)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)
}

// ------- Picture -------

func getPicture(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	index, _ := strconv.Atoi(request.Data)
	if index == -1 {
		index = uio.GetPictureSize(user)
	}
	data := uio.GetPicture(user, index)
	w.Header().Set(HEADER_CONTENT_TYPE, "text/plain")
	w.Write([]byte(fmt.Sprint(string(data))))
}

func getAllPictures(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}
	data := uio.GetAllPictures(user)
	jsonData, err := json.Marshal(data)
	if err != nil {
		http.Error(w, "Failed to export data", http.StatusConflict)
		return
	}
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write([]byte(fmt.Sprint(string(jsonData))))
}

func getPictureSize(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	highest := uio.GetPictureSize(user)

	dataSize := DataPackage{Data: strconv.Itoa(highest)}
	result, _ := json.Marshal(dataSize)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)
}

func postPicture(w http.ResponseWriter, r *http.Request) {
	var data DataPackage
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	picture := data.Data
	uio.AddPicture(user, picture)
	w.WriteHeader(http.StatusOK)
}

// ------- Public/Private Keys -------

func getPrivKey(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	dataReply := DataPackage{IDT: request.IDT, Data: uio.GetPrivateKey(user)}
	result, _ := json.Marshal(dataReply)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)
}

func getPubKey(w http.ResponseWriter, r *http.Request) {
	var request DataPackage
	err := json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(request.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	dataReply := DataPackage{IDT: request.IDT, Data: uio.GetPublicKey(user)}
	result, _ := json.Marshal(dataReply)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)
}

// ------- Commands -------

func getCommand(w http.ResponseWriter, r *http.Request) {
	var data DataPackage
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}
	cmd, time, sig := uio.GetCommandToUser(user)

	// commandAsString may be an empty string, that's fine
	reply := commandData{IDT: data.IDT, Data: cmd, UnixTime: time, CmdSig: sig}
	result, _ := json.Marshal(reply)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write([]byte(result))
}

func postCommand(w http.ResponseWriter, r *http.Request) {
	var data commandData
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}
	uio.SetCommandToUser(user, data.Data, data.UnixTime, data.CmdSig)
	w.WriteHeader(http.StatusOK)
}

/*
func getCommandLog(w http.ResponseWriter, r *http.Request) {
	var data DataPackage
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	commandLog := uio.GetCommandLog(user)

	// commandLogs may be empty, that's fine
	reply := DataPackage{IDT: data.IDT, Data: commandLog}
	result, _ := json.Marshal(reply)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write([]byte(result))
}
*/

// ------- Push -------

func getPushUrl(w http.ResponseWriter, r *http.Request) {
	var data DataPackage
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	endpoint := strings.TrimSpace(data.Data)
	if endpoint != "" {
		uio.SetPushUrl(user, endpoint)
		w.WriteHeader(http.StatusOK)
		return
	}

	url := uio.GetPushUrl(user)
	w.Write([]byte(fmt.Sprint(url)))
}

func postPushUrl(w http.ResponseWriter, r *http.Request) {
	var data DataPackage
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	uio.SetPushUrl(user, strings.TrimSpace(data.Data))
	w.WriteHeader(http.StatusOK)
}

// ------- Authentication, Login -------

func requestSalt(w http.ResponseWriter, r *http.Request) {
	var data DataPackage
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	if !user.IsUserIdValid(data.IDT) {
		http.Error(w, "Invalid RMD ID", http.StatusBadRequest)
		return
	}
	salt := uio.GetSalt(data.IDT)
	dataReply := DataPackage{IDT: data.IDT, Data: salt}
	result, _ := json.Marshal(dataReply)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)

}

func requestAccess(w http.ResponseWriter, r *http.Request) {
	var data loginData
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	if !user.IsUserIdValid(data.IDT) {
		http.Error(w, "Invalid RMD ID", http.StatusBadRequest)
		return
	}

	pwHash := data.PasswordHash
	if pwHash == "" && data.PlainPassword != "" {
		u := uio.GetUser(data.IDT)
		if u == nil {
			http.Error(w, "Invalid RMD ID", http.StatusBadRequest)
			return
		}
		saltBytes := decodeSalt(u.Salt)
		innerHash, err := argon2Encoded("context:loginAuthentication"+data.PlainPassword, saltBytes)
		if err != nil {
			http.Error(w, "Password hashing failed", http.StatusBadRequest)
			return
		}
		pwHash = innerHash
	}

	accessToken, err := uio.RequestAccess(data.IDT, pwHash, data.SessionDurationSeconds, getRemoteIp(r))

	if err == user.ErrAccountLocked {
		http.Error(w, "Account is locked", http.StatusLocked)
		return
	}
	if err != nil {
		http.Error(w, "Access denied", http.StatusForbidden)
		return
	}

	accessTokenReply := DataPackage{IDT: data.IDT, Data: accessToken.Token}
	result, _ := json.Marshal(accessTokenReply)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)
}

func postPassword(w http.ResponseWriter, r *http.Request) {
	var data passwordUpdateData
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}

	// Fallback: allow PlainPassword and reuse existing privkey/pubkey if client cannot hash.
	if data.HashedPassword == "" && data.Salt == "" && data.PrivKey == "" && data.PlainPassword != "" {
		newSalt := make([]byte, 16)
		if _, err := rand.Read(newSalt); err != nil {
			http.Error(w, "failed to generate salt", http.StatusBadRequest)
			return
		}
		newHash, err := argon2Encoded("context:loginAuthentication"+data.PlainPassword, newSalt)
		if err != nil {
			http.Error(w, "failed to hash password", http.StatusBadRequest)
			return
		}
		data.Salt = base64.RawStdEncoding.EncodeToString(newSalt)
		data.HashedPassword = newHash
		data.PrivKey = user.PrivateKey // keep existing wrapped key
	}

	if data.HashedPassword == "" || data.Salt == "" || data.PrivKey == "" {
		http.Error(w, "missing password data", http.StatusBadRequest)
		return
	}

	uio.UpdateUserPassword(user, data.PrivKey, data.Salt, data.HashedPassword)

	dataReply := DataPackage{IDT: data.IDT, Data: "true"}
	result, _ := json.Marshal(dataReply)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)
}

// ------- (De-) Registration -------

func deleteDevice(w http.ResponseWriter, r *http.Request) {
	var data DataPackage
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}
	user, err := uio.CheckAccessTokenAndGetUser(data.IDT)
	if err != nil {
		http.Error(w, ERR_ACCESS_TOKEN_INVALID, http.StatusUnauthorized)
		return
	}
	uio.DeleteUser(user)
	w.WriteHeader(http.StatusOK)
}

type createDeviceHandler struct {
	RegistrationToken string
}

func (h createDeviceHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	var reg registrationData
	err := json.NewDecoder(r.Body).Decode(&reg)
	if err != nil {
		http.Error(w, ERR_JSON_INVALID, http.StatusBadRequest)
		return
	}

	// Server-side registration fallback: generate keys and hashes when client cannot.
	if reg.HashedPassword == "" || reg.PrivKey == "" || reg.PubKey == "" {
		err = populateServerSideRegistration(&reg)
		if err != nil {
			http.Error(w, fmt.Sprintf("Registration failed: %s", err.Error()), http.StatusBadRequest)
			return
		}
	}

	if h.RegistrationToken != "" && h.RegistrationToken != reg.RegistrationToken {
		log.Error().Msg("invalid RegistrationToken")
		http.Error(w, "Registration Token not valid", http.StatusUnauthorized)
		return
	}

	id, err := uio.CreateNewUser(reg.PrivKey, reg.PubKey, reg.Salt, reg.HashedPassword, reg.RequestedUsername)
	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to create username: %s", err.Error()), http.StatusBadRequest)
		return
	}

	accessToken := user.AccessToken{DeviceId: id, Token: ""}
	result, _ := json.Marshal(accessToken)
	w.Header().Set(HEADER_CONTENT_TYPE, CT_APPLICATION_JSON)
	w.Write(result)
}

// ------- Main Web Request Handling -------

func getVersion(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, version.VERSION)
}

func mainLocation(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodPut:
		getLocation(w, r)
	case http.MethodPost:
		postLocation(w, r)
	}
}

// populateServerSideRegistration fills the registration payload with generated
// salt, hashed password, and wrapped private/public keys using the same
// parameters as the web/client flow. This enables registration when the client
// cannot run WebCrypto/Argon2.
func populateServerSideRegistration(reg *registrationData) error {
	if reg.PlainPassword == "" {
		return fmt.Errorf("missing password and no client-provided crypto material")
	}

	// Generate salt and argon2 hash (encoded)
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		return err
	}
	hashEncoded, err := argon2Encoded("context:loginAuthentication"+reg.PlainPassword, salt)
	if err != nil {
		return err
	}

	// Generate RSA keypair
	privKey, err := rsa.GenerateKey(rand.Reader, 3072)
	if err != nil {
		return err
	}
	pubDer, err := x509.MarshalPKIXPublicKey(&privKey.PublicKey)
	if err != nil {
		return err
	}
	pubB64 := base64.StdEncoding.EncodeToString(pubDer)

	privDer, err := x509.MarshalPKCS8PrivateKey(privKey)
	if err != nil {
		return err
	}
	wrappedPriv, err := wrapPrivateKey(reg.PlainPassword, privDer)
	if err != nil {
		return err
	}

	reg.Salt = base64.RawStdEncoding.EncodeToString(salt)
	reg.HashedPassword = hashEncoded
	reg.PrivKey = wrappedPriv
	reg.PubKey = pubB64
	return nil
}

func argon2Encoded(input string, salt []byte) (string, error) {
	if len(salt) == 0 {
		return "", fmt.Errorf("missing salt")
	}
	hash := argon2.IDKey([]byte(input), salt, 1, 131072, 4, 32)
	saltB64 := base64.RawStdEncoding.EncodeToString(salt)
	hashB64 := base64.RawStdEncoding.EncodeToString(hash)
	return fmt.Sprintf("$argon2id$v=19$m=131072,t=1,p=4$%s$%s", saltB64, hashB64), nil
}

func wrapPrivateKey(password string, pkcs8 []byte) (string, error) {
	// Derive AES key from password
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		return "", err
	}
	key := argon2.IDKey([]byte("context:asymmetricKeyWrap"+password), salt, 1, 131072, 4, 32)

	// AES-GCM encrypt
	block, err := aes.NewCipher(key)
	if err != nil {
		return "", err
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}
	iv := make([]byte, gcm.NonceSize())
	if _, err := rand.Read(iv); err != nil {
		return "", err
	}
	ciphertext := gcm.Seal(nil, iv, pkcs8, nil)

	// concat salt + iv + ciphertext
	out := make([]byte, 0, len(salt)+len(iv)+len(ciphertext))
	out = append(out, salt...)
	out = append(out, iv...)
	out = append(out, ciphertext...)
	return base64.StdEncoding.EncodeToString(out), nil
}

func decodeSalt(s string) []byte {
	if s == "" {
		return nil
	}
	bytes, err := base64.RawStdEncoding.DecodeString(s)
	if err != nil {
		return nil
	}
	return bytes
}

func mainPicture(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodPut:
		getPicture(w, r)
	case http.MethodPost:
		postPicture(w, r)
	}
}

func mainCommand(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodPut:
		getCommand(w, r)
	case http.MethodPost:
		postCommand(w, r)
	}
}

func mainPushUrl(w http.ResponseWriter, r *http.Request) {
	// Historically, POST is used to fetch the push URL (with an empty body)
	// and PUT was used to set it. We now also accept POST with Data=<endpoint>
	// to register/update the push URL so that both the app and web portal work.
	switch r.Method {
	case http.MethodPost:
		getPushUrl(w, r)
	case http.MethodPut:
		postPushUrl(w, r)
	}
}

type mainDeviceHandler struct {
	createDeviceHandler createDeviceHandler
}

func (h mainDeviceHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodPost:
		deleteDevice(w, r)
	case http.MethodPut:
		h.createDeviceHandler.ServeHTTP(w, r)
	}
}
