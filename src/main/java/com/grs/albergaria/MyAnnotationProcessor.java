package com.grs.albergaria; // Altere para o pacote apropriado

import jakarta.inject.Named;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SupportedAnnotationTypes({"jakarta.inject.Named"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MyAnnotationProcessor extends AbstractProcessor {

    private static final Logger logger = Logger.getLogger(MyAnnotationProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        logger.log(Level.INFO, "Processing annotations... Total annotations to process: {0}", annotations.size());

        for (TypeElement annotation : annotations) {
            logger.log(Level.INFO, "Processing annotation: {0}", annotation.getQualifiedName());
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Named.class)) {
            logger.log(Level.INFO, "Found element annotated with @Named: {0}", element.getSimpleName());

            Named namedAnnotation = element.getAnnotation(Named.class);
            String beanName = namedAnnotation.value();
            String className = element.getSimpleName().toString();

            logger.log(Level.INFO, "Annotation details - Element: {0}, Annotation value: {1}",
                    new Object[]{className, beanName});

            if (isKafkaPublisher(element)) {
                logger.log(Level.INFO, "Identified as KafkaPublisher - Element: {0}, Bean Name: {1}",
                        new Object[]{className, beanName});
                createKafkaPublisherFactory(element, beanName, className);
            } else {
                logger.log(Level.WARNING, "Element {0} is not a KafkaPublisher. Skipping.", className);
            }
        }

        logger.log(Level.INFO, "Annotation processing completed.");
        return true;
    }

    private boolean isKafkaPublisher(Element element) {
        String elementType = element.asType().toString();
        logger.log(Level.INFO, "Checking if element type {0} is a KafkaPublisher...", elementType);
        boolean isKafkaPublisher = elementType.equals("com.grs.albergaria.case1.KafkaPublisher");
        logger.log(Level.INFO, "Result of KafkaPublisher check for element {0}: {1}", new Object[]{element.getSimpleName(), isKafkaPublisher});
        return isKafkaPublisher;
    }

    private void createKafkaPublisherFactory(Element element, String beanName, String className) {
        logger.log(Level.INFO, "Creating KafkaPublisherFactory for element: {0}", className);

        String factoryClassName = "KafkaPublisherFactory";
        String packageName = getPackageName(element);

        logger.log(Level.INFO, "Determined package name: {0}", packageName);

        String fullClassName = element.asType().toString();
        String importStatement = "import " + fullClassName + ";\n";

        String factoryContent =
                "package " + packageName + ";\n\n" +
                        importStatement +
                        "import jakarta.inject.Named;\n" +
                        "import io.micronaut.context.annotation.Bean;\n" +
                        "import io.micronaut.context.annotation.Factory;\n\n" +
                        "@Factory\n" +
                        "public class " + factoryClassName + " {\n\n" +
                        "    @Bean\n" +
                        "    @Named(\"" + beanName + "\")\n" +
                        "    public KafkaPublisher " + beanName + "() {\n" +
                        "        return new KafkaPublisher(); \n" +
                        "    }\n" +
                        "}\n";

        try {
            writeToFile(packageName, factoryClassName, factoryContent);
            logger.log(Level.INFO, "Factory class {0} created successfully in package {1}", new Object[]{factoryClassName, packageName});
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write factory class {0} in package {1}", new Object[]{factoryClassName, packageName});
            logger.log(Level.SEVERE, "IOException occurred", e);
        }
    }

    private String getPackageName(Element element) {
        logger.log(Level.INFO, "Retrieving package name for element: {0}", element.getSimpleName());
        String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
        logger.log(Level.INFO, "Package name found: {0}", packageName);
        return packageName;
    }

    private void writeToFile(String packageName, String className, String content) throws IOException {
        logger.log(Level.INFO, "Attempting to write file for class {0} in package {1}", new Object[]{className, packageName});

        // Use the project build directory property, default to target/generated-sources/factories
        String outputDir = System.getProperty("maven.project.build.directory", "target/generated-sources/factories/");

        // Build the directory path for the package, including the 'src/main/java' part
        String dirPath = outputDir + "src/main/java/" + packageName.replace('.', '/') + "/";
        Path path = Paths.get(dirPath);

        // Create directories if they do not exist
        if (!Files.exists(path)) {
            logger.log(Level.INFO, "Directory {0} does not exist. Creating directories...", dirPath);
            Files.createDirectories(path);
        }

        // Calculate the file path for the new class
        String filePath = dirPath + className + ".java"; // Append class name to the path
        logger.log(Level.INFO, "Calculated file path: {0}", filePath);

        // Write the content to the file
        try (Writer writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
            logger.log(Level.INFO, "Factory class content written to file: {0}", filePath);
        }

        logger.log(Level.INFO, "Finished writing file for class {0} at path {1}", new Object[]{className, filePath});
    }
}
