package net.sharplab.springframework.security.webauthn.util.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.sharplab.springframework.security.webauthn.attestation.authenticator.*;
import net.sharplab.springframework.security.webauthn.attestation.authenticator.extension.Extension;
import net.sharplab.springframework.security.webauthn.util.UnsignedNumberUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson Deserializer for WebAuthnAuthenticatorData
 */
public class WebAuthnAuthenticatorDataDeserializer extends StdDeserializer<WebAuthnAuthenticatorData> {

    private ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());

    //TODO: make protected
    public WebAuthnAuthenticatorDataDeserializer() {
        super(WebAuthnAuthenticatorData.class);
    }

    @Override
    public WebAuthnAuthenticatorData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        byte[] value = p.getBinaryValue();
        return deserialize(value);
    }

    public WebAuthnAuthenticatorData deserialize(byte[] value) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(value);
        WebAuthnAuthenticatorData webAuthnAuthenticatorData = new WebAuthnAuthenticatorData();

        byte[] rpIdHash = new byte[32];
        byteBuffer.get(rpIdHash, 0, 32);
        byte flags = byteBuffer.get();
        long counter = UnsignedNumberUtil.getUnsignedInt(byteBuffer);

        webAuthnAuthenticatorData.setRpIdHash(rpIdHash);
        webAuthnAuthenticatorData.setFlags(flags);
        webAuthnAuthenticatorData.setCounter(counter);

        WebAuthnAttestedCredentialData attestationData;
        List<Extension> extensions;
        if (webAuthnAuthenticatorData.isFlagAT()) {
            attestationData = deserializeAttestedCredentialData(byteBuffer);
        }
        else {
            attestationData = null;
        }
        if (webAuthnAuthenticatorData.isFlagED()) {
            extensions = deserializeExtensions(byteBuffer);
        } else {
            extensions = null;
        }

        webAuthnAuthenticatorData.setAttestationData(attestationData);
        webAuthnAuthenticatorData.setExtensions(extensions);

        return webAuthnAuthenticatorData;
    }

    WebAuthnAttestedCredentialData deserializeAttestedCredentialData(ByteBuffer byteBuffer) {
        byte[] aaGuid = new byte[16];
        byteBuffer.get(aaGuid, 0, 16);
        int length = UnsignedNumberUtil.getUnsignedShort(byteBuffer);
        byte[] credentialId = new byte[length];
        byteBuffer.get(credentialId, 0, length);
        byte[] remaining = new byte[byteBuffer.remaining()];
        byteBuffer.get(remaining);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(remaining);
        AbstractCredentialPublicKey credentialPublicKey = deserializeCredentialPublicKey(byteArrayInputStream);
        WebAuthnAttestedCredentialData attestationData = new WebAuthnAttestedCredentialData();
        attestationData.setAaGuid(aaGuid);
        attestationData.setCredentialId(credentialId);
        attestationData.setCredentialPublicKey(credentialPublicKey);
        int extensionsBufferLength = byteArrayInputStream.available();
        byteBuffer.position(byteBuffer.position() - extensionsBufferLength);
        return attestationData;
    }

    AbstractCredentialPublicKey deserializeCredentialPublicKey(InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, ESCredentialPublicKey.class); //TODO
        } catch (IOException e) {
            throw new IllegalArgumentException(e);//TODO
        }
    }

    List<Extension> deserializeExtensions(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() == 0) {
            return new ArrayList<>();
        }
        byte[] remaining = new byte[byteBuffer.remaining()];
        byteBuffer.get(remaining);
        try {
            return objectMapper.readValue(remaining, new TypeReference<List<Extension>>(){});
        } catch (IOException e) {
            throw new IllegalArgumentException(e);//TODO
        }
    }

}