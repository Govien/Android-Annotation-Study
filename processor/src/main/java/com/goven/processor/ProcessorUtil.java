package com.goven.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.PUBLIC;

/**
 * @author Created by Goven on 16/12/1 上午11:13
 * @email gxl3999@gmail.com
 */
public class ProcessorUtil {

    private Messager messager;

    public ProcessorUtil(Messager messager) {
        this.messager = messager;
    }

    public boolean isPublic(TypeElement element) {
        return element.getModifiers().contains(PUBLIC);
    }

    public boolean isAbstract(TypeElement element) {
        return element.getModifiers().contains(ABSTRACT);
    }

    public String getPackageName(Elements elements, TypeElement typeElement) throws NoPackageNameException {
        PackageElement pkg = elements.getPackageOf(typeElement);
        if (pkg.isUnnamed()) {
            throw new NoPackageNameException(typeElement);
        }
        return pkg.getQualifiedName().toString();
    }

    public void printMessage(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    public void printMessage(String message, Element element) {
        printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    public void printMessage(Diagnostic.Kind level, String message, Element element) {
        messager.printMessage(level, message, element);
    }

}
