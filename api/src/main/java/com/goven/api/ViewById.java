package com.goven.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解控件
 * @author Created by Goven on 16/11/30 下午10:14
 * @email gxl3999@gmail.com
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ViewById {
    int value() default -1;
}
