package com.exasol.auth.kerberos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.logging.Logger;

import com.exasol.errorreporting.ExaError;

/**
 * This class generates the necessary configuration for a successful Kerberos authentication.
 *
 * @see <a href="https://docs.exasol.com/sql/import.htm?Highlight=kerberos">Kerberos authentication for
 *      <code>IMPORT</code> (Exasol online documentation)</a>
 */
public class KerberosConfigurationCreator {
    /** useSubjectCredsOnly property */
    public static final String USE_SUBJECT_CREDENTIALS_ONLY_PROPERTY = "javax.security.auth.useSubjectCredsOnly";
    /** Kerberos config property */
    public static final String KERBEROS_CONFIG_PROPERTY = "java.security.krb5.conf";
    /** Login config property */
    public static final String LOGIN_CONFIG_PROPERTY = "java.security.auth.login.config";
    /** Kerberos authentication preamble */
    public static final String KERBEROS_AUTHENTICATION_PREAMBLE = "ExaAuthType=Kerberos";
    private static final Logger LOGGER = Logger.getLogger(KerberosConfigurationCreator.class.getName());

    /**
     * Check whether the given password contains Kerberos credentials.
     *
     * @param password password / credential string to be examined
     * @return <code>true</code> if the password is a Kerberos credential string, <code>false</code> otherwise
     */
    public static boolean isKerberosAuthentication(final String password) {
        return password.startsWith(KERBEROS_AUTHENTICATION_PREAMBLE);
    }

    /**
     * Create Kerberos configuration and system properties.
     *
     * @param user     Kerberos principal
     * @param password connection password containing kerberos configuration and key tab
     */
    public void writeKerberosConfigurationFiles(final String user, final String password) {
        final String[] tokens = password.split(";");
        final String preamble = tokens[0];
        if ((tokens.length == 3) && KERBEROS_AUTHENTICATION_PREAMBLE.equals(preamble)) {
            final String base64EncodedKerberosConfig = tokens[1];
            final String base64EncodedKeyTab = tokens[2];
            createKerberosConfiguration(user, base64EncodedKerberosConfig, base64EncodedKeyTab);
        } else {
            throw new KerberosConfigurationCreatorException(ExaError.messageBuilder("E-VSCJDBC-32")
                    .message("Syntax error in Kerberos password."
                            + " Must conform to: 'ExaAuthType=Kerberos;<base 64 kerberos config>;<base 64 key tab>'")
                    .toString());
        }
    }

    private void createKerberosConfiguration(final String user, final String base64EncodedKerberosConfig,
            final String base64EncodedKeyTab) {
        try {
            final Path temporaryDirectory = createCommonDirectoryForKerberosConfigurationFiles();
            final Path kerberosConfigPath = createTemporaryKerberosConfigFile(base64EncodedKerberosConfig,
                    temporaryDirectory);
            final Path keyTabPath = createTemporaryKeyTabFile(base64EncodedKeyTab, temporaryDirectory);
            final Path jaasConfigPath = createTemporaryJaasConfig(temporaryDirectory, user, keyTabPath);
            setKerberosSystemProperties(kerberosConfigPath, jaasConfigPath);
        } catch (final IOException exception) {
            throw new KerberosConfigurationCreatorException(ExaError.messageBuilder("E-VSCJDBC-33")
                    .message("Unable to create temporary Kerberos configuration file.").toString(), exception);
        }
    }

    private Path createCommonDirectoryForKerberosConfigurationFiles() throws IOException {
        final Path temporaryDirectory = Files.createTempDirectory("kerberos_");
        temporaryDirectory.toFile().deleteOnExit();
        LOGGER.finer(() -> "Created temporary directory \"" + temporaryDirectory
                + "\" to contain Kerberos authentication files.");
        return temporaryDirectory;
    }

    private Path createTemporaryKerberosConfigFile(final String base64EncodedKerberosConfig,
            final Path temporaryDirectory) throws IOException {
        return createTemporaryFile(temporaryDirectory, "krb_", ".conf",
                Base64.getDecoder().decode(base64EncodedKerberosConfig.getBytes()));
    }

    private Path createTemporaryKeyTabFile(final String base64EncodedKeyTab, final Path temporaryDirectory)
            throws IOException {
        return createTemporaryFile(temporaryDirectory, "kt_", ".keytab",
                Base64.getDecoder().decode(base64EncodedKeyTab.getBytes()));
    }

    private Path createTemporaryFile(final Path temporaryDirectory, final String prefix, final String suffix,
            final byte[] content) throws IOException {
        final Path temporaryFile = Files.createTempFile(temporaryDirectory, prefix, suffix);
        temporaryFile.toFile().deleteOnExit();
        Files.write(temporaryFile, content);
        LOGGER.finer(
                () -> "Wrote " + content.length + " bytes to Kerberos configuration file \"" + temporaryFile + "\".");
        return temporaryFile;
    }

    private Path createTemporaryJaasConfig(final Path temporaryDirectory, final String user, final Path keyTabPath)
            throws IOException {
        final byte[] content = ("Client {\n" //
                + "com.sun.security.auth.module.Krb5LoginModule required\n" //
                + "principal=\"" + user + "\"\n" //
                + "useKeyTab=true\n" //
                + "keyTab=\"" + keyTabPath + "\"\n" //
                + "doNotPrompt=true\n" //
                + "useTicketCache=false;\n" //
                + "};\n" //
                + "com.sun.security.jgss.initiate {\n" //
                + "com.sun.security.auth.module.Krb5LoginModule required\n" //
                + "principal=\"" + user + "\"\n" //
                + "useKeyTab=true\n" //
                + "keyTab=\"" + keyTabPath + "\"\n" //
                + "doNotPrompt=true\n" //
                + "useTicketCache=false;\n" //
                + "};\n").getBytes();
        return createTemporaryFile(temporaryDirectory, "jaas_", ".conf", content);
    }

    private void setKerberosSystemProperties(final Path kerberosConfigPath, final Path jaasConfigPath) {
        System.setProperty(KERBEROS_CONFIG_PROPERTY, kerberosConfigPath.toString());
        System.setProperty(LOGIN_CONFIG_PROPERTY, jaasConfigPath.toString());
        System.setProperty(USE_SUBJECT_CREDENTIALS_ONLY_PROPERTY, "false");
    }
}