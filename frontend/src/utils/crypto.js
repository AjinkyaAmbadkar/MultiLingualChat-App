// Client-side decryption using the Web Crypto API.
// Mirrors the server's EncryptionService: RSA-OAEP (SHA-256) + AES-256-GCM.
// The private key is held in memory only — never written to localStorage.

const OAEP_PARAMS = { name: 'RSA-OAEP', hash: 'SHA-256' }
const GCM_PARAMS  = (iv) => ({ name: 'AES-GCM', iv, tagLength: 128 })

function b64ToBytes(b64) {
  const bin = atob(b64)
  const buf = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i)
  return buf
}

/**
 * Import a Base64 PKCS#8 RSA private key as a CryptoKey.
 * Call once at login; store the result in React state (memory only).
 */
export async function importPrivateKey(base64Pkcs8) {
  return crypto.subtle.importKey(
    'pkcs8',
    b64ToBytes(base64Pkcs8),
    OAEP_PARAMS,
    false,
    ['decrypt']
  )
}

/**
 * Unwrap a Base64 RSA-encrypted AES key using the user's private CryptoKey.
 * Returns a CryptoKey usable for AES-GCM decryption.
 */
async function unwrapAesKey(wrappedB64, privateKey) {
  const rawAes = await crypto.subtle.decrypt(
    { name: 'RSA-OAEP' },
    privateKey,
    b64ToBytes(wrappedB64)
  )
  return crypto.subtle.importKey('raw', rawAes, { name: 'AES-GCM' }, false, ['decrypt'])
}

/**
 * Decrypt a Base64 AES-GCM ciphertext.
 */
async function aesDecrypt(ciphertextB64, aesKey, ivB64) {
  const plainBuf = await crypto.subtle.decrypt(
    GCM_PARAMS(b64ToBytes(ivB64)),
    aesKey,
    b64ToBytes(ciphertextB64)
  )
  return new TextDecoder().decode(plainBuf)
}

/**
 * Decrypt a message object, appending plaintext fields so MessageBubble works unchanged.
 * Falls back gracefully for pre-migration rows (null encrypted fields).
 *
 * @param {object} msg           - raw message from server
 * @param {CryptoKey} privateKey - user's RSA private key (from importPrivateKey)
 * @param {string|number} myId   - current user's id
 */
export async function decryptMessage(msg, privateKey, myId) {
  if (!privateKey) return { ...msg, originalText: '🔒 Login again to decrypt', translatedText: '🔒', senderTranslatedText: '🔒' }

  const wrappedKey = String(msg.senderId) === String(myId)
    ? msg.aesKeyForSender
    : msg.aesKeyForReceiver

  // Pre-migration row — no encrypted fields
  if (!wrappedKey || !msg.encryptedOriginalText) {
    return { ...msg, originalText: '[older message — not encrypted]', translatedText: '[older message — not encrypted]', senderTranslatedText: '[older message — not encrypted]' }
  }

  try {
    const aesKey = await unwrapAesKey(wrappedKey, privateKey)
    const [originalText, translatedText, senderTranslatedText] = await Promise.all([
      aesDecrypt(msg.encryptedOriginalText, aesKey, msg.aesIvOriginal),
      aesDecrypt(msg.encryptedTranslatedText, aesKey, msg.aesIvTranslated),
      aesDecrypt(msg.encryptedSenderText, aesKey, msg.aesIvSender),
    ])
    return { ...msg, originalText, translatedText, senderTranslatedText }
  } catch (err) {
    console.error('Decryption failed for message', msg.id, err)
    return { ...msg, originalText: '🔒 Decryption failed', translatedText: '🔒 Decryption failed', senderTranslatedText: '🔒 Decryption failed' }
  }
}
