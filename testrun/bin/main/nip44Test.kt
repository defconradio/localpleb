package nip17

import com.example.crypto.importKeyPairFromHex
import com.example.crypto.nip44.Nip44v2
import com.example.crypto.nip44.Hkdf


fun main() {
    //TEST1 general example
    val nip44v2 = com.example.crypto.nip44.Nip44v2()
    val (senderPriv, senderPub) = com.example.crypto.generateRandomKeyPair()
    val (receiverPriv, receiverPub) = com.example.crypto.generateRandomKeyPair()
    val message = "Hello, World!"

    val conversationKey1 = nip44v2.computeConversationKey(senderPriv, receiverPub)
    val encrypted = nip44v2.encrypt(message, conversationKey1)
    val decryptedInfo1 = nip44v2.decrypt(encrypted, conversationKey1)
    val decrypted = nip44v2.unpad(decryptedInfo1.ciphertext)

    println("Original message: $message")
    println("Encrypted message: $encrypted")
    println("Decrypted message: $decrypted")

    if (message == decrypted) {
        println("Test passed!")
    } else {
        println("Test failed!")
    }
    println("\n")

    //TEST2 simulate nip44 encription decription using our libraries step by step

    // 1. generate sender and receiver key objects to work with.
    // we only need the X only public keys right ? // YES for NIP-44, you use the x-only public keys
    val secretHexSender = "c3975b71347b41a066516e66ae15596fe336c3e290d2dba1dbc9a98e249d3c38"
    val (senderPrivKey, _) = importKeyPairFromHex(secretHexSender)
    val senderXOnlyPub = senderPrivKey.xOnlyPublicKey()
    println("Imported X-Only sender Public key (hex): ${senderXOnlyPub.value.toByteArray().joinToString("") { "%02x".format(it) }}")
    
    val secretHexReceiver = "d1cc1ae94947a97a57a7c38f6179f5f3cc84ee850b463d45a9819a112f0a8265"
    val (receiverPrivKey, receiverPubKey ) = importKeyPairFromHex(secretHexReceiver)
    val receiverXOnlyPub = receiverPrivKey.xOnlyPublicKey()
    println("Imported X-Only receiver Public key (hex): ${receiverXOnlyPub.value.toByteArray().joinToString("") { "%02x".format(it) }}")
    println("\n")
 
    // 2. get conversation key
    val conversationKeyTest2 = nip44v2.computeConversationKey(senderPrivKey, receiverPrivKey.publicKey())
    println("Conversation key (sender's perspective): ${conversationKeyTest2.joinToString("") { "%02x".format(it) }}")

    // 2b. get conversation key from the other side
    val conversationKeyTest3 = nip44v2.computeConversationKey(receiverPrivKey, senderPrivKey.publicKey())
    println("Conversation key (receiver's perspective): ${conversationKeyTest3.joinToString("") { "%02x".format(it) }}")

    if (conversationKeyTest2.contentEquals(conversationKeyTest3)) {
        println("\nTest passed: Keys are identical!")
    } else {
        println("\nTest failed: Keys are different!")
    }
    println("\n")

    // 3. get conversation key using the new function in the crypto module
    val conversationKey2 = nip44v2.computeConversationKey(senderPrivKey, receiverPubKey)
    println("Conversation key 2 (hex): ${conversationKey2.joinToString("") { "%02x".format(it) }}")

    if (conversationKeyTest2.contentEquals(conversationKey2)) {
        println("Test passed!")
    } else {
        println("Test failed!")
    }

    // 4. Derive the final conversation key using HKDF-extract, as per NIP-44
    val salt = "nip44-v2".toByteArray(Charsets.UTF_8)
    val hkdf = Hkdf()
    val finalConversationKey = hkdf.extract(conversationKey2, salt)
    println("\nFinal NIP-44 Conversation key (hex): ${finalConversationKey.joinToString("") { "%02x".format(it) }}")
  
   // 5. encrypt and decrypt a message using the final conversation key
    val nip44v2_test2 = Nip44v2()
    val messageTest2 = "Hello, World! This is a test message for NIP-44 encryption."

    val encryptedTest2 = nip44v2_test2.encrypt(messageTest2, finalConversationKey)
    val decryptedInfo2 = nip44v2_test2.decrypt(encryptedTest2, finalConversationKey)
    val decryptedTest2 = nip44v2_test2.unpad(decryptedInfo2.ciphertext)

    println("\nOriginal message: $messageTest2")
    println("Encrypted message: $encryptedTest2")
    println("Decrypted message: $decryptedTest2")

    if (messageTest2 == decryptedTest2) {
        println("Test passed!")
    } else {
        println("Test failed!")
    }
    
    // TEST3: Verify computeConversationKey with known test vectors
    // step 1
    println("\n--- TEST 3: computeConversationKey Verification ---")
    val nip44v2Crypto = Nip44v2()

    val sec1Hex = "0000000000000000000000000000000000000000000000000000000000000001"
    val sec2Hex = "0000000000000000000000000000000000000000000000000000000000000002"
    val expectedConversationKeyHex = "c41c775356fd92eadc63ff5a0dc1da211b268cbea22316767095b2871ea1412d"

    val (privateKey1, publicKey1) = importKeyPairFromHex(sec1Hex)
    val (privateKey2, publicKey2) = importKeyPairFromHex(sec2Hex)

    val calculatedKey = nip44v2Crypto.computeConversationKey(privateKey1, publicKey2)
    val calculatedKeyHex = calculatedKey.joinToString("") { "%02x".format(it) }

    println("Expected Key: $expectedConversationKeyHex")
    println("Calculated Key: $calculatedKeyHex")

    if (expectedConversationKeyHex == calculatedKeyHex) {
        println("Test 3 PASSED!")
    } else {
        println("Test 3 FAILED!")
    }
    // full test vectors run here ./gradlew :crypto:jvmTest
    // crypto/src/jvmTest/kotlin/com/example/crypto/Nip44ConversationKeyJvmTest.kt
 
    // step 2
    println("\n--- TEST 3 STEP 2: NIP-44 encrypt test vector ---")
    val testNonceHex = "0000000000000000000000000000000000000000000000000000000000000001"
    val testNonce = testNonceHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val testPlaintext = "a"
    val expectedPayload = "AgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABee0G5VSK0/9YypIObAtDKfYEAjD35uVkHyB0F4DwrcNaCXlCWZKaArsGrY6M9wnuTMxWfp1RTN9Xga8no+kF5Vsb"

    val encryptedInfo = nip44v2Crypto.encryptWithNonce(testPlaintext, calculatedKey, testNonce)
    val actualPayload = encryptedInfo.encodePayload()

    println("Expected payload: $expectedPayload")
    println("Actual payload:   $actualPayload")

    if (expectedPayload == actualPayload) {
        println("Test 3 STEP 2 PASSED!")
    } else {
        println("Test 3 STEP 2 FAILED!")
    }
    // step 3 decrypt
    println("\n--- TEST 3 STEP 3: NIP-44 decrypt test vector ---")
    val decryptedInfo3 = nip44v2Crypto.decrypt(expectedPayload, calculatedKey)
    val decryptedText = nip44v2Crypto.unpad(decryptedInfo3.ciphertext)
    println("Decrypted text: $decryptedText")
    if (decryptedText == testPlaintext) {
        println("Test 3 STEP 3 PASSED!")
    } else {
        println("Test 3 STEP 3 FAILED!")
    }
}