package com.example.XBRL_validation_with_xsd_no_xerces;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SpringBootApplication
public class XbrlValidationWithXsdNoXercesApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(XbrlValidationWithXsdNoXercesApplication.class, args);
    }

    @Override
    public void run(final String... args) throws Exception {
        // Compute path to the XBRL payload to be validated.
        final Path xbrlPath = Path.of("src/main/resources/xbrl/xbrl_001_APRA-valid.xml");
        final Source xmlStreamSource = new StreamSource(xbrlPath.toFile());

        // Create objects to do the validation.
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Path xsdPath = Path.of("src/main/resources/xsd/sbr.gov.au/taxonomy/sbr_au_reports/sprstrm/sprcnt/sprcnt_0001/sprcnt.0001.conttrans.request.02.02.report.xsd");
        final Schema schema = schemaFactory.newSchema(xsdPath.toFile());

        // Create the validator.
        javax.xml.validation.Validator validator = schema.newValidator();

        // Set up a catalog so that use the local XSD files and not look them up from the internet.
        final ClassLoader classLoader = XbrlValidationWithXsdNoXercesApplication.class.getClassLoader();
        final String catalogPath = Objects
                .requireNonNull(classLoader.getResource("xsd/catalog.xml"))
                .toURI()
                .toString();
        System.out.println("Catalog path: " + catalogPath);
        validator.setProperty("javax.xml.catalog.files", catalogPath);

        // Capture the XSD errors so that we can report them back.
        final XsdErrorHandler errorHandler = new XsdErrorHandler(xbrlPath);
        validator.setErrorHandler(errorHandler);

        // Validate and extract errors.
        validator.validate(xmlStreamSource);
        if (errorHandler.getErrors().isEmpty()) {
            System.out.println("'" + xbrlPath
                    + "' is valid against '" + xsdPath
                    + "'.");
        }
        for (SAXParseException error : errorHandler.getErrors()) {
            System.err.println("Error: " + error.toString());
        }
    }

    public static final class XsdErrorHandler implements ErrorHandler {

        /**
         * List of errors from parsing an XML file.
         */
        private final List<SAXParseException> errors = new ArrayList<>();

        /**
         * The XSD file being validated.
         */
        private final Path xmlFile;

        /**
         * Instantiates a new XSD error handler.
         *
         * @param xmlFile the XML file being validated.
         */
        public XsdErrorHandler(final Path xmlFile) {
            this.xmlFile = xmlFile;
        }

        /**
         * Gets errors.
         *
         * @return the errors
         */
        public List<SAXParseException> getErrors() {
            return errors;
        }

        @Override
        public void warning(final SAXParseException exception) {
            System.err.println("Warning occurred during validation of XML file '" + xmlFile.getFileName() + "': '"
                    + exception.getMessage() + "'.");
            errors.add(exception);
        }

        @Override
        public void error(final SAXParseException exception) {
            System.err.println("Error occurred during validation of XML file '" + xmlFile.getFileName() + "': '"
                    + exception.getMessage() + "'.");
            errors.add(exception);
        }

        @Override
        public void fatalError(final SAXParseException exception) {
            System.err.println("Fatal error occurred during validation of XML file '" + xmlFile.getFileName() + "': '"
                    + exception.getMessage() + "'.");
            errors.add(exception);
        }
    }
}
