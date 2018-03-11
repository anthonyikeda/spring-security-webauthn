package net.sharplab.springframework.security.webauthn.sample.app.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.sharplab.springframework.security.webauthn.sample.app.web.AttestationObjectForm;
import net.sharplab.springframework.security.webauthn.util.jackson.WebAuthnModule;
import org.apache.commons.codec.binary.Base64;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Converter which converts from {@link AttestationObjectForm} to {@link String}
 */
public class AttestationObjectFormToBase64StringConverter implements Converter<AttestationObjectForm, String> {

    private ObjectMapper objectMapper;

    public AttestationObjectFormToBase64StringConverter(){
        this.objectMapper = new ObjectMapper(new CBORFactory());
        this.objectMapper.registerModule(new WebAuthnModule());
    }

    @Override
    public String convert(AttestationObjectForm source) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(source);
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}