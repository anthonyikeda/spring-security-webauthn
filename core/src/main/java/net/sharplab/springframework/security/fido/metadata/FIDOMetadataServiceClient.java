package net.sharplab.springframework.security.fido.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import net.sharplab.springframework.security.fido.metadata.structure.MetadataStatement;
import net.sharplab.springframework.security.fido.metadata.structure.MetadataTOCPayload;
import net.sharplab.springframework.security.fido.metadata.structure.MetadataTOCPayloadEntry;
import net.sharplab.springframework.security.fido.metadata.structure.StatusReport;
import net.sharplab.springframework.security.webauthn.util.jackson.WebAuthnModule;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Client for FIDO Metadata Service
 */
public class FIDOMetadataServiceClient {

    RestTemplate restTemplate;

    JWSVerifier jwsVerifier;

    ObjectMapper objectMapper;

    private static final String DEFAULT_FIDO_METADATA_SERVICE_ENDPOINT = "https://mds.fidoalliance.org/";

    private String fidoMetadataServiceEndpoint;

    public FIDOMetadataServiceClient(RestTemplate restTemplate, JWSVerifier jwsVerifier, String fidoMetadataServiceEndpoint){
        this.restTemplate = restTemplate;
        this.jwsVerifier = jwsVerifier;
        this.fidoMetadataServiceEndpoint = fidoMetadataServiceEndpoint;

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new WebAuthnModule());
    }

    public FIDOMetadataServiceClient(RestTemplate restTemplate, JWSVerifier jwsVerifier){
        this(restTemplate, jwsVerifier, DEFAULT_FIDO_METADATA_SERVICE_ENDPOINT);
    }

    public FIDOMetadataServiceClient(RestTemplate restTemplate, ResourceLoader resourceLoader){
        this(restTemplate, new CertPathJWSVerifier(resourceLoader));
    }

    public MetadataTOCPayload retrieveMetadataTOC() {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(fidoMetadataServiceEndpoint, String.class);
        SignedJWT jwt;
        try {
            jwt = (SignedJWT) JWTParser.parse(responseEntity.getBody());
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }

        //Verify JWS Signature
        jwsVerifier.verify(jwt);

        String payloadString = jwt.getPayload().toString();
        MetadataTOCPayload payload;
        try {
            payload = objectMapper.readValue(payloadString, MetadataTOCPayload.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return payload;
    }

    public MetadataStatement retrieveMetadataStatement(URI uri){
        ResponseEntity<String> responseEntity;
        try{
            responseEntity = restTemplate.getForEntity(uri, String.class);
        }
        catch (RuntimeException e){
            throw e;
        }
        String decoded = new String(Base64Utils.decodeFromString(responseEntity.getBody()), StandardCharsets.UTF_8);
        try {
            MetadataStatement metadataStatement = objectMapper.readValue(decoded, MetadataStatement.class);
            return metadataStatement;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getFidoMetadataServiceEndpoint() {
        return fidoMetadataServiceEndpoint;
    }

    public void setFidoMetadataServiceEndpoint(String fidoMetadataServiceEndpoint) {
        this.fidoMetadataServiceEndpoint = fidoMetadataServiceEndpoint;
    }



}