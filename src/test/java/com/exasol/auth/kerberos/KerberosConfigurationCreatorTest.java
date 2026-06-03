package com.exasol.auth.kerberos;

import static com.exasol.auth.kerberos.KerberosConfigurationCreator.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KerberosConfigurationCreatorTest {
    private static final String KEY_TAB_CONTENT = "ktbname";
    private static final String KERBEROS_CONFIG_CONTENT = "kbcname";

    private static final String USER = "kerberos_user";
    private static final String KERBEROS_PASSWORD = "ExaAuthType=Kerberos;"
            + Base64.getEncoder().encodeToString(KERBEROS_CONFIG_CONTENT.getBytes(StandardCharsets.UTF_8)) + ";"
            + Base64.getEncoder().encodeToString(KEY_TAB_CONTENT.getBytes(StandardCharsets.UTF_8));
    private KerberosConfigurationCreator creator;

    @BeforeEach
    void beforeEach() {
        this.creator = new KerberosConfigurationCreator();
    }

    @Test
    void testIsKerberosAuthenticationTrue() {
        assertThat(KerberosConfigurationCreator.isKerberosAuthentication(KERBEROS_PASSWORD), equalTo(true));
    }

    @Test
    void testIsKerberosAuthenticationFalse() {
        assertThat(KerberosConfigurationCreator.isKerberosAuthentication("not a kerberose password"), equalTo(false));
    }

    @Test
    void testWriteKerberosConfigurationFiles() {
        this.creator.writeKerberosConfigurationFiles(USER, KERBEROS_PASSWORD);
        assertAll(
                this::assertJaasConfigurationPathProperty,
                this::assertKerberosConfigurationPathProperty,
                this::assertUseSubjectCredentialsProperty,
                () -> assertJaasConfigurationFileContent(getJaasConfigPathFromProperty()),
                () -> assertKerberosFileContent(KERBEROS_CONFIG_CONTENT),
                () -> assertKeyTableFileContent(getJaasConfigPathFromProperty()),
                this::assertTemporaryDirectoryPermissions);
    }

    @Test
    void base64() {
        final byte[] raw = "some string äöüß".getBytes(StandardCharsets.UTF_8);
        final byte[] encoded = Base64.getEncoder().encode(raw);
        final byte[] decoded = Base64.getDecoder().decode(encoded);
        assertThat(decoded, equalTo(raw));
    }

    @Test
    void testWriteKerberosConfigurationFilesEscapesPrincipal() throws IOException {
        final String principal = "user\\name\"quoted";
        this.creator.writeKerberosConfigurationFiles(principal, KERBEROS_PASSWORD);

        final String content = getJaasConfigContent(getJaasConfigPathFromProperty());

        assertAll(
                () -> assertThat(content, containsString("principal=\"user\\\\name\\\"quoted\"")),
                () -> assertThat(content, not(containsString("principal=\"user\\name\"quoted\""))));
    }

    @ParameterizedTest
    @ValueSource(strings = { "user\nname", "user\rname" })
    void testWriteKerberosConfigurationFilesRejectsPrincipalsWithLineBreaks(final String principal) {
        final KerberosConfigurationCreatorException exception = assertThrows(
                KerberosConfigurationCreatorException.class,
                () -> this.creator.writeKerberosConfigurationFiles(principal, KERBEROS_PASSWORD));

        assertAll(() -> assertThat(exception.getMessage(),
                equalTo("E-VSCJDBC-52: Kerberos principal must not contain line breaks.")));
    }

    private String getJaasConfigPathFromProperty() {
        return System.getProperty(LOGIN_CONFIG_PROPERTY);
    }

    private void assertJaasConfigurationPathProperty() {
        assertThat("JAAS configuration path", getJaasConfigPathFromProperty(),
                matchesPattern(FilePatterns.JAAS_CONFIG_PATTERN));
    }

    private void assertKerberosConfigurationPathProperty() {
        assertThat("Kerberos configuration path", getKerberosConfigFromProperty(), //
                matchesPattern(FilePatterns.KERBEROS_CONFIG_PATTERN));
    }

    private String getKerberosConfigFromProperty() {
        return System.getProperty(KERBEROS_CONFIG_PROPERTY);
    }

    private void assertUseSubjectCredentialsProperty() {
        assertThat("Use subject credentials", System.getProperty(USE_SUBJECT_CREDENTIALS_ONLY_PROPERTY),
                equalTo("false"));
    }

    private void assertJaasConfigurationFileContent(final String jaasConfigurationPath) throws IOException {
        final String content = getJaasConfigContent(jaasConfigurationPath);
        assertAll(() -> assertThat(content, startsWith("Client {")), //
                () -> assertThat(content, containsString("principal=\"" + USER + "\"")));
    }

    private String getJaasConfigContent(final String jaasConfigurationPath) throws IOException {
        return Files.readString(Paths.get(jaasConfigurationPath), StandardCharsets.UTF_8);
    }

    private void assertKerberosFileContent(final String expectedContent) {
        final File kerberosFile = new File(getKerberosConfigFromProperty());
        assertAll(
                () -> assertThat(kerberosFile, anExistingFile()),
                () -> assertThat(Files.readString(kerberosFile.toPath(), StandardCharsets.UTF_8),
                        equalTo(expectedContent)));
    }

    private void assertKeyTableFileContent(final String jaasConfigurationPath) throws IOException {
        final String jaasConfigContent = getJaasConfigContent(jaasConfigurationPath);
        final String keyTabPath = extractKeyTabPath(jaasConfigContent);
        final File keyTabFile = new File(keyTabPath);
        assertAll(
                () -> assertThat("Key tab file: " + keyTabPath, keyTabFile, anExistingFile()),
                () -> assertThat(Files.readString(keyTabFile.toPath(), StandardCharsets.UTF_8),
                        equalTo(KEY_TAB_CONTENT)));
    }

    private void assertTemporaryDirectoryPermissions() throws IOException {
        final Set<PosixFilePermission> expectedPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
        final Set<PosixFilePermission> actualPermissions = Files
                .getPosixFilePermissions(Paths.get(getKerberosConfigFromProperty()).getParent());

        assertThat("Temporary Kerberos directory permissions", actualPermissions, equalTo(expectedPermissions));
    }

    private String extractKeyTabPath(final String jaasConfigContent) {
        final String keyTabPathAndSuffix = jaasConfigContent.substring(jaasConfigContent.indexOf("keyTab=\"") + 8);
        return keyTabPathAndSuffix.substring(0, keyTabPathAndSuffix.indexOf("\""));
    }

    @ValueSource(strings = { "", "missing preamble;foo;bar", "ExaAuthType=Kerberos;missing next part",
            "ExaAuthType=Kerberos;too;many;parts" })
    @ParameterizedTest
    void testIllegalKerberosPasswordThrowsException(final String password) {
        final KerberosConfigurationCreatorException exception = assertThrows(
                KerberosConfigurationCreatorException.class,
                () -> this.creator.writeKerberosConfigurationFiles("anyone", password));
        assertThat(exception.getMessage(), equalTo("E-VSCJDBC-32: Syntax error in Kerberos password. Must conform to: "
                + "'ExaAuthType=Kerberos;<base 64 kerberos config>;<base 64 key tab>'"));
    }
}
