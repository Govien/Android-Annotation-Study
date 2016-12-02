package com.goven.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控件注解完成后，执行初始化
 * @author Created by Goven on 16/11/30 下午10:16
 * @email gxl3999@gmail.com
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface AfterViews {
}
