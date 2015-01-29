package com.google.javascript.jscomp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates a compiler feature which we believe works correctly with
 * EcmaScript 6 language constructs.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Es6Compatible {
}
