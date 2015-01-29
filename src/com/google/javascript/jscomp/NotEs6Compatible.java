package com.google.javascript.jscomp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface NotEs6Compatible {

  String issue();
}
