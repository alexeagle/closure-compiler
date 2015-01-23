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

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.Arrays.asList;

import com.google.common.base.Joiner;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;
import com.google.common.truth.Truth;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.rhino.Node;

import java.util.Arrays;

public class Es6InlineTypesTest extends CompilerTestCase {

  private Compiler compiler;

  @Override
  public void setUp() {
    setAcceptedLanguage(LanguageMode.ECMASCRIPT6);
    enableAstValidation(true);
    runTypeCheckAfterProcessing = true;
    compiler = createCompiler();
  }

  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    options.setLanguageOut(LanguageMode.ECMASCRIPT6_TYPED);
    options.setPreferSingleQuotes(true);
    return options;
  }

  @Override
  public CompilerPass getProcessor(Compiler compiler) {
    return new Es6TypeDeclarations(compiler);
  }

  @Override
  protected int getNumRepetitions() {
    return 1;
  }

  public void testVariableDeclaration() {
    assertSource("/** @type {string} */ var print;")
        .transpilesTo("var print: string;");
  }

  public void testVariableDeclarationWithoutDeclaredType() throws Exception {
    assertSource("var print;")
        .transpilesTo("var print;");
  }

  public void testFunctionReturnType() throws Exception {
    assertSource("/** @return {boolean} */ function b(){}")
        .transpilesTo(
            "function b(): boolean {",
            "}",
            ";");
  }

  public void testFunctionParameterTypes() throws Exception {
    assertSource(
        "/** @param {number} n @param {string} s */",
        "function t(n,s){}")
        .transpilesTo(
            "function t(n: number, s: string) {",
            "}",
            ";");
  }

  public void testFunctionInsideAssignment() throws Exception {
    assertSource(
        "/** @param {boolean} b @return {boolean} */",
        "var f = function(b){return !b};")
        .transpilesTo(
            "var f = function(b: boolean): boolean {",
            "  return !b;",
            "};");
  }

  public void testNestedFunctions() throws Exception {
    assertSource("/**@param {boolean} b*/",
        "var f = function(b){var t = function(l) {}; t();};")
        .transpilesTo(
            "var f = function(b: boolean) {",
            "  var t = function(l) {",
            "  };",
            "  t();",
            "};");
  }

  public void testNullable() throws Exception {
    assertSource(
        "/** @param {!number} n @return {?string}*/",
        "function s(n) { return ''; };")
        .transpilesTo(
            "function s(n: number): null | string {",
            "  return '';",
            "}",
            ";");
  }

  public void testOptionalParameter() throws Exception {
    assertSource("/** @param {goog.dom.Foo=} n */ function s(n) { };")
        .transpilesTo(
            "function s(n?: goog.dom.Foo) {",
            "}",
            ";");
  }

  public void testStarType() throws Exception {
    assertSource("/** @type {*} */ var n;")
        .transpilesTo("var n: Object | number | string | boolean | null | undefined;");
  }

  public void testUnknownType() throws Exception {
    assertSource("/** @type {?} */ var n;")
        .transpilesTo("var n: any;");
  }

  public void testUndefinedType() throws Exception {
    assertSource("/** @type {undefined} */ var n;")
        .transpilesTo("var n: undefined;");
  }

  public void testNullType() throws Exception {
    assertSource("/** @type {null} */ var n;")
        .transpilesTo("var n: null;");
  }

  public void testFunctionType() throws Exception {
    assertSource("/** @type {function(string,number):boolean} */ var n;")
        .transpilesTo("var n: (p1: string, p2: number) => boolean;");
  }

  public void testTypeUnion() throws Exception {
    assertSource("/** @type {(number|boolean)} */ var n;")
        .transpilesTo("var n: number | boolean;");
  }

  public void testArrayType() throws Exception {
    assertSource("/** @type {Array.<string>} */ var s;")
        .transpilesTo("var s: string[];");
    assertSource("/** @type {!Array.<!$jscomp.typecheck.Checker>} */ var s;")
        .transpilesTo("var s: $jscomp.typecheck.Checker[];");
  }

  public void testRecordType() throws Exception {
    assertSource("/** @type {{myNum: number, myObject}} */ var s;")
        .transpilesTo("var s: {myNum: number; myObject};");
  }

  public void testParameterizedType() throws Exception {
    assertSource("/** @type {MyCollection.<string>} */ var s;")
        .transpilesTo("var s: MyCollection<string>;");
  }

  private SourceTranslationSubject assertSource(String... s) {
    return assertAbout(SOURCE).that(s);
  }

  private final SubjectFactory<SourceTranslationSubject, String[]> SOURCE =
      new SubjectFactory<SourceTranslationSubject, String[]>() {
    @Override
    public SourceTranslationSubject getSubject(FailureStrategy fs, String[] o) {
      return new SourceTranslationSubject(fs, o);
    }};

  private class SourceTranslationSubject
      extends Subject<SourceTranslationSubject, String[]> {

    public SourceTranslationSubject(FailureStrategy failureStrategy, String[] s) {
      super(failureStrategy, s);
    }

    private String doCompile(String... lines) {
      compiler.init(externsInputs,
          asList(SourceFile.fromCode("expected", Joiner.on("\n").join(lines))), getOptions());
      Node root = compiler.parseInputs();
      assertWithMessage("Parsing error: " + Arrays.toString(compiler.getErrors()))
          .that(compiler.getErrorCount()).is(0);
      getProcessor(compiler).process(root.getFirstChild(), root.getLastChild());
      return compiler.toSource();
    }

    public void transpilesTo(String... lines) {
      Truth.assertThat(doCompile(getSubject()).trim()).is("'use strict';" + Joiner.on("\n").join(lines));
    }
  }
}
