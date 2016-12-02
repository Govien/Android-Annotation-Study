package com.goven.processor;

import android.os.Bundle;

import com.google.auto.service.AutoService;
import com.goven.api.AfterViews;
import com.goven.api.EActivity;
import com.goven.api.ViewById;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@AutoService(Processor.class)
public class MyProcessor extends AbstractProcessor {

    private ProcessorUtil util;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Messager messager = processingEnv.getMessager();
        util = new ProcessorUtil(messager);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // 设置可支持的注解
        Set<String> types = new HashSet<>();
        types.add(EActivity.class.getCanonicalName());
        types.add(ViewById.class.getCanonicalName());
        types.add(AfterViews.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 遍历被@EActivity注解的Activity
        List<ActivityAnnotation> activitys = null;
        for (Element element : roundEnv.getElementsAnnotatedWith(EActivity.class)) {
            if (element instanceof TypeElement) {
                TypeElement activityElement = (TypeElement) element;
                ActivityAnnotation activityAnnotation = new ActivityAnnotation();
                activityAnnotation.setActivityElement(activityElement);

                List<VariableElement> viewElements = null;
                ExecutableElement initElement = null;
                // 遍历Activity元素的所有子元素，包括常量、变量、块、构造器、方法等等
                for (Element subElement : activityElement.getEnclosedElements()) {
                    // 只取出被@ViewById注解的变量元素
                    if (subElement instanceof VariableElement && subElement.getAnnotation(ViewById.class) != null) {
                        if (viewElements == null) {
                            viewElements = new ArrayList<>();
                        }
                        VariableElement viewElement = (VariableElement) subElement;
                        viewElements.add(viewElement);
                    } else if (subElement instanceof ExecutableElement && subElement.getAnnotation(AfterViews.class) != null) {
                        // 只取出被@AfterViews注解的方法元素
                        initElement = (ExecutableElement) subElement;
                    }
                }
                activityAnnotation.setViewElements(viewElements);
                activityAnnotation.setInitElement(initElement);
                if (activitys == null) {
                    activitys = new ArrayList<>();
                }
                activitys.add(activityAnnotation);
            }

        }
        if (activitys != null) {
            try {
                generateCode(activitys);
            } catch (NoPackageNameException | IOException e) {
                util.printMessage("Couldn't generate class");
            }
        }
        return true;
    }

    /**
     * 生成源代码
     * @param activitys 被注解的Activity集合
     */
    private void generateCode(List<ActivityAnnotation> activitys) throws NoPackageNameException, IOException {
        for (ActivityAnnotation annotation : activitys) {
            TypeElement activityElement = annotation.getActivityElement();
            // 从类型元素中获取注解对象
            EActivity activity = activityElement.getAnnotation(EActivity.class);
            // 取出Activity类名称
            String activityName = activityElement.getSimpleName().toString();
            // 取出布局ID
            int layoutId = activity.value();
            // 包名
            String packageName = util.getPackageName(processingEnv.getElementUtils(), activityElement);

            // javapoet的类型构建工具，构建Activity类
            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(activityName + "_").addModifiers(Modifier.PUBLIC);
            typeBuilder.superclass(TypeName.get(activityElement.asType()));
            // 方法构建，构建onCreate方法
            MethodSpec.Builder createBuilder = MethodSpec.methodBuilder("onCreate")
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(Bundle.class, "savedInstanceState")
                    .addStatement("super.onCreate(savedInstanceState)")
                    .addStatement("setContentView($L)", layoutId);
            // 构建View控件
            if (annotation.getViewElements() != null) {
                for (VariableElement viewElement : annotation.getViewElements()) {
                    String viewName = viewElement.getSimpleName().toString();
                    ViewById viewById = viewElement.getAnnotation(ViewById.class);
                    // 被注解的View是否设置了value值，如果设置直接使用此id，否则以view的变量名当作id
                    if (viewById.value() != -1) {
                        createBuilder.addStatement("$N = ($T) findViewById($L)", viewName, TypeName.get(viewElement.asType()), viewById.value());
                    } else {
                        createBuilder.addStatement("$N = ($T) findViewById(R.id.$N)", viewName, TypeName.get(viewElement.asType()), viewName);
                    }
                }
            }
            if (annotation.getInitElement() != null) {
                createBuilder.addStatement("$N()", annotation.getInitElement().getSimpleName());
            }
            typeBuilder.addMethod(createBuilder.build());
            JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build()).build();
            // 在app module/build/generated/source/apt 生成一份源代码
            javaFile.writeTo(processingEnv.getFiler());
            javaFile.writeTo(System.out);// 输出到终端
        }
    }

    /**
     * Activity注解封装类，对应生成一个Activity源文件
     */
    private static class ActivityAnnotation {

        private TypeElement activityElement;
        private List<VariableElement> viewElements;
        private ExecutableElement initElement;

        public TypeElement getActivityElement() {
            return activityElement;
        }

        public void setActivityElement(TypeElement activityElement) {
            this.activityElement = activityElement;
        }

        public List<VariableElement> getViewElements() {
            return viewElements;
        }

        public void setViewElements(List<VariableElement> viewElements) {
            this.viewElements = viewElements;
        }

        public ExecutableElement getInitElement() {
            return initElement;
        }

        public void setInitElement(ExecutableElement initElement) {
            this.initElement = initElement;
        }

    }

}
