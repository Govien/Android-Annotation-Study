package com.goven.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解Activity，注入布局文件
 * @author Created by Goven on 16/11/30 下午10:11
 * @email gxl3999@gmail.com
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EActivity {
    int value() default -1;
}
