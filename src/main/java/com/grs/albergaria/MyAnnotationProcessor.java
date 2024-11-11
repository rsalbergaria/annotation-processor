package com.grs.albergaria;

import jakarta.inject.Named;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SupportedAnnotationTypes("jakarta.inject.Named")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MyAnnotationProcessor extends AbstractProcessor {

    private static final Logger logger = Logger.getLogger(MyAnnotationProcessor.class.getName());
    private final Set<String> beanDefinitions = new HashSet<>();
    private String packageName = null;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Named.class)) {
            if (isKafkaPublisher(element)) {
                Named namedAnnotation = element.getAnnotation(Named.class);
                String beanName = namedAnnotation.value();
                String className = element.asType().toString();

                if (packageName == null) {
                    packageName = getPackageName(element);
                }

                beanDefinitions.add(createBeanDefinition(beanName, className));
            }
        }

        if (!beanDefinitions.isEmpty() && packageName != null) {
            try {
                writeKafkaPublisherFactory();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to write KafkaPublisherFactory class", e);
            }
        }

        return true;
    }

    private boolean isKafkaPublisher(Element element) {
        return element.asType().toString().equals("com.grs.albergaria.case1.KafkaPublisher");
    }

    private String createBeanDefinition(String beanName, String className) {
        return "    @Bean\n" +
                "    @Named(\"" + beanName + "\")\n" +
                "    public " + className + " " + beanName + "() {\n" +
                "        return new " + className + "();\n" +
                "    }\n";
    }

    private String getPackageName(Element element) {
        return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    private void writeKafkaPublisherFactory() throws IOException {
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + ".KafkaPublisherFactory");

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.print("package " + packageName + ";\n\n");
            out.print("import io.micronaut.context.annotation.Bean;\n");
            out.print("import io.micronaut.context.annotation.Factory;\n");
            out.print("import jakarta.inject.Named;\n");
            out.print("import com.grs.albergaria.case1.KafkaPublisher;\n\n");
            out.print("@Factory\n");
            out.print("public class KafkaPublisherFactory {\n\n");

            for (String beanDefinition : beanDefinitions) {
                out.print(beanDefinition);
            }

            out.print("}\n");
        }
    }
}
