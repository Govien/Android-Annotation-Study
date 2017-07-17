# Android-Annotation-Study
Android注解生成代码
> Android注解生成代码老早前就开始用了，在以前Eclipse ADT时代就已经开始使用AvailableAnnotations注解框架，极大简化了Android开发。只是一开始并不了解其原理，感觉这个黑科技还是蛮厉害的，但每次遇到问题总要排查很久。如今Android Studio大行其道，对注解技术支持的更完善，看来有必要学学Java中注解到底是咋回事了。

# 注解流程
java是一门静态语言，语言层面缺乏灵活性，这使得我们项目中很容易出现大量重复的代码。java注解是java5引入的功能，我们能够经常看到,如@Override，但未必对其有深入的了解。了解这方面的知识有助于我们深入理解一些框架，下面就以模仿AndroidAnnotations中三个注解特性来详细说明。
* 注解布局文件，@EActivity(layout_id);
* 注解控件，@ViewById；
* Activity初始化，@AfterViews。

## 定义一个注解
```java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EActivity {
	int value() default -1;
}
```
1. @interface，定义一个注解类；
2. @Retention，注解的保存策略，有三种：
    * SOURCE，源码文件中；
    * CLASS，源码文件和class文件中；
    * RUNTIME，运行时，这类的注解一般是通过反射解析。
3. @Target，注解的目标，可以是 *TYPE、Field、METHOD、PARAMETER、CONSTRUCTOR、LOCALVARIABLE、ANNOTATIONTYPE、PACKAGE*。

## APT—annotation processing tool
在编译时，扫描和处理注解的一个构建工具，Java5出现，Java6开放，Java7被废弃。现在，该功能由javac来实现，主要由AbstractProcessor处理注解，这个类我们必须了解。在gradle项目中引入apt，只需在项目build.gradle中的dependencies加入依赖：
```java
classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
```
## AbstractProcessor
AbstractProcessor是javac扫描和处理注解的关键类。必须继承这个抽象类，解析注解，生成源代码。
```java
@AutoService(Processor.class)
public class MyProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment env){ }

    @Override
    public boolean process(Set<? extends TypeElement> annoations, RoundEnvironment env) { }

    @Override
    public Set<String> getSupportedAnnotationTypes() { }

    @Override
    public SourceVersion getSupportedSourceVersion() { }

}
```
MyProcessor的各个方法的作用：
* init(ProcessingEnvironment env)：javac会在Processor创建时调用并执行的初始化操作，该方法会传入一个参数ProcessingEnvironment env，通过env可以访问Elements、Types、Filer等工具类；
* getSupportedAnnotationTypes()：获取所有被注解的类型；
* getSupportedSourceVersion：获取源码支持的JDK版本，通常SourceVersion.latestSupported()；
* process(Set<? extends TypeElement> annotations, RoundEnvironment env)：处理注解的主方法，代码生成的逻辑，参数RoundEnvironment env可以访问任何一个被注解的元素。

生成代码的本质就是拼接字符串，而javapoet为我们提供了一个优雅的拼接方式，极大方便了代码生成。
```java
compile 'com.squareup:javapoet:1.8.0'
```
javapoet项目主页：https://github.com/square/javapoet

## Register Processor
如何让javac执行时调用我自定义的 *MyProcessor* 呢，这需要注册自定义的 *MyProcessor*
1. MyProcessor 需要打包到 jar 包中，就像其它普通的 .jar 文件一样，这里命名为 MyProcessor.jar
2. 但 MyProcessor.jar 中多了一个特殊的文件：javax.annotation.processing.Processor，它存储在 jar/META-INF/services/ 文件夹下，MyProcessor.jar 的结构是这样的：
```java
MyProcessor.jar
    - com
        - goven
            - processor
                - MyProcessor.class
    - META-INF
        - services
            - javax.annotation.processing.Processor
```
javax.annotation.processing.Processor 文件列出了要注册的 Processor，每个 Processor 逐行列出
```java
com.goven.annotation.processor.MyProcessor
com.goven.annotation.processor.OtherProcessor
...
```
构建项目时 javac 自动检测并读取 javax.annotation.processing.Processor 来注册列出来的 Processor，然后执行我们生成代码的逻辑。  

通过上面的注册流程，发现着实麻烦，还好大Google为我们提供了一个自动注册的库 auto-servic，只需在gradle配置中加入以下依赖：
```java
compile 'com.google.auto.service:auto-service:1.0-rc2'
```
然后在自定义的Processor前加上注解@AutoService(Processor.class)

# 注解实战
## 新建工程 AnnotationStudy
创建三个模块：
* app，主模块，Activity中演示注解效果
* api，定义注解类，app和processor模块中用到
* processor，解析及生成源代码，编译期用到，运行时不包含进去
## 配置android-apt
在工程根目录的build.gradle中加入apt依赖，构建工程时，它会辅助 javac 执行 processor
```java
dependencies {
    classpath 'com.android.tools.build:gradle:2.2.2'
    classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
}
```
## app模块
------------------------------------------------------------------
### 配置build.gradle
* 应用android-apt插件
* 依赖processor模块，生成源代码，编译期有效
* 依赖api模块

```java
// 应用android-apt插件
apply plugin: 'com.android.application'
apply plugin: 'com.neenbedankt.android-apt'
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    compile 'com.android.support:appcompat-v7:24.2.1'
    testCompile 'junit:junit:4.12'
    // 加入processor和api模块依赖
    apt project(':processor')
    compile project(':api')
}
```
### 编写布局文件
```xml
// activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <TextView
        android:id="@+id/tvAnnotation"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="我是一个被注解的控件" />
        
    <TextView
        android:id="@+id/tvOther"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="48dp"
        android:text="我是另外一个被注解的控件" />

</RelativeLayout>
```
### 编写被注解的Activity
```java
// MainActivity.java
package com.goven.annotation;

import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.goven.api.AfterViews;
import com.goven.api.EActivity;
import com.goven.api.ViewById;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    @ViewById TextView tvAnnotation;
    @ViewById(R.id.tvAnnotation) TextView textView;

    @AfterViews void initView() {
    }

}
```

## api模块
------------------------------------------------------------------
### 配置build.gradle
```java
// 普通的 java 模板
apply plugin: 'java'
```
### 创建EActivity注解类
注解Activity，注入布局ID，生成 Activity 及 onCreate 方法，注解目标是 *ElementType.Type*
```java
// EActivity.java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EActivity {
    int value() default -1;
}
```
### 创建ViewById注解类
注解控件，注入id，生成 findViewById() 方法，注解目标是 *ElementType.FIELD*
```java
// ViewById.java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ViewById {
    int value() default -1;
}
```
### 创建AfterViews注解类
注解初始化方法，控件注解完成后调用初始化方法，注解目标是 *ElementType.METHOD*
```java
// AfterViews.java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface AfterViews {
}
```

## processor模块
------------------------------------------------------------------
### 配置build.gradle
* 普通Java模板
* 依赖api模块
* 依赖auto-service，自动生成 javax.annotation.processing.Processor 文件
* 依赖javapoet，生成代码

```java
// 普通的 java 模板
apply plugin: 'java'
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    // 加入auto-service和javapoet依赖
    compile 'com.google.auto.service:auto-service:1.0-rc2'
    compile 'com.squareup:javapoet:1.8.0'
    // 依赖api模块
    compile project(':api')
}
sourceCompatibility = "1.7"
targetCompatibility = "1.7"

```
### 编写注解处理类
```java
@AutoService(Processor.class)
public class MyProcessor extends AbstractProcessor {

    private ProcessorUtil util;// 自定义的注解工具类

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
            // 在app/build/generated/source/apt 生成一份源代码
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
```
### 编译工程
编译完成后，在processor模块生成processor.jar ，其中关键文件javax.annotation.processing.Processor也被自动添加到jar包中

![](http://ohg2vhg1g.bkt.clouddn.com/public/16-12-2/95301563.jpg

在app模块的 build/generated/source/apt 下生成Activity源代码

![](http://ohg2vhg1g.bkt.clouddn.com/public/16-12-2/12456984.jpg)

MainActivity_代码内容：
```java
// MainActivity_.java
package com.goven.annotation;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity_ extends MainActivity {
  
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(2130968602);
    tvAnnotation = (TextView) findViewById(R.id.tvAnnotation);
    textView = (TextView) findViewById(2131427414);
    initView();
  }
  
}
```
最后一点，要在AndroidManifest.xml中注册生成的Activity_，而不是被注解的Activity，所有的操作和跳转都是针对Activity_
```xml
<activity android:name=".MainActivity_">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
<activity android:name=".SecondActivity_"/>
```
运行效果

![](http://ohg2vhg1g.bkt.clouddn.com/public/16-12-2/32361935.jpg)

至此，基础的注解功能已实现，如果想学习更多的注解，访问：
[AndroidAnnotations](https://github.com/androidannotations/androidannotations/wiki/AvailableAnnotations "AvailableAnnotations")

# 总结
1. 这里只是简单演示注解生成代码的特性，真实的注解框架远比这个要复杂、精细；
2. 演示代码中并未检查被注解元素的有效性，比如@EActivity是否真正注解了Activity组件；
3. Android环境未封装，未得到R文件，未检查Manifest文件中是否注册了Activity_；
4. 编译期注解被称为 *pre-compile*，不能直接访问 *.class，如果要访问类信息，使用 element.asType();
5. 如果想要**Debugging**注解处理过程，需要另外配置Gradle及AndroidStudio，稍后我会写一篇Debugging Annotation的教程。
