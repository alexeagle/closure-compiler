/*
 * Copyright 2015 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import com.google.common.base.Joiner;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;

/**
 * Verifies that the compiler pass can convert to the ES6 class syntax.
 * These tests are generally the inverse of those in
 * {@link com.google.javascript.jscomp.Es6ToEs3ConverterTest}
 */
public class ConvertToTypedES6ClassesTest extends CompilerTestCase {

  public void setUp() throws Exception {
    super.setUp();
    setAcceptedLanguage(LanguageMode.ECMASCRIPT6);
    // TODO(alexeagle): preserve JSDoc in this pass, when we expect humans to see the result
    compareJsDoc = false;
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new ConvertToTypedES6(compiler);
  }

  public void testConvertToClassSyntax() throws Exception {
    test(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "var C = function() {};"
    ), "class C { }");
  }

  public void testConvertClassWithConstructorBody() throws Exception {
    test(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "var C = function(a) { this.a = a; };"
    ), "class C { constructor(a) { this.a = a; } }");
  }

  public void testConvertClassWithMethod() throws Exception {
    test(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "var C = function() {};",
        "C.prototype.method = function() {};"
    ), "class C { method() {} }");
  }

  public void testConvertClassWithMethodAndConstructor() throws Exception {
    test(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "var C = function(a) { this.a = a; };",
        "C.prototype.foo = function() { console.log(this.a); };",
        "C.prototype.bar = function() { alert(this.a); };"
    ), Joiner.on('\n').join(
        "class C {",
        "  constructor(a) { this.a = a; }",
        "  foo() { console.log(this.a); }",
        "  bar() { alert(this.a); }",
        "}"
    ));
  }

  public void testInterface() {
    test(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " */",
        "var Converter = function() { };",
        "",
        "/**",
        " * @param {X} x",
        " * @return {Y}",
        " */",
        "Converter.prototype.convert = function(x) {};"
    ), Joiner.on('\n').join(
        "/**",
        " * @interface",
        " */",
        "class Converter {",
        "  convert(x: X): Y {}",
        "}"
    ));
  }

  public void testClassDeclaredWithoutAssignment() throws Exception {
    test(Joiner.on('\n').join("/**",
        " * A shape.",
        " * @interface",
        " */",
        "function Shape() {}",
        "Shape.prototype.draw = function() {};"),
        "/** @interface */ class Shape { draw() {} }");

  }
}
