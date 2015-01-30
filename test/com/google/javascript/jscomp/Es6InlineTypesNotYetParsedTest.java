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

import static com.google.common.truth.Truth.THROW_ASSERTION_ERROR;
import static java.util.Arrays.asList;

import com.google.common.base.Joiner;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.rhino.Node;

import java.util.Arrays;

/**
 * This test is temporary. It asserts that the CodeGenerator produces the
 * right ES6_TYPED sources, even though those sources don't yet parse.
 * All these test cases should be migrated to a round-trip test as the parser
 * catches up.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Es6InlineTypesNotYetParsedTest extends CompilerTestCase {

  private Compiler compiler;

  @Override
  public void setUp() {
    setAcceptedLanguage(LanguageMode.ECMASCRIPT6);
    enableAstValidation(true);
    compiler = createCompiler();
  }

  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    options.setLanguageOut(LanguageMode.ECMASCRIPT6_TYPED);
    return options;
  }

  @Override
  public CompilerPass getProcessor(Compiler compiler) {
    return new ConvertToTypedES6(compiler);
  }

  @Override
  protected int getNumRepetitions() {
    return 1;
  }

  public void testNullType() throws Exception {
    assertSource("/** @type {null} */ var n;")
        .transpilesTo("var n: null;");
  }

  public void testUntypedVarargs() throws Exception {
    assertSource("/** @param {function(this:T, ...)} fn */ function f(fn) {}")
        .transpilesTo("function f(fn: (...p1) => any) {\n}\n;");
  }

  public void testAnyTypeVarargsParam() throws Exception {
    assertSource("/** @param {...*} v */ function f(v){}")
        .transpilesTo("function f(...v: any) {\n}\n;");
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
    assertSource("/** @type {Object.<string, number>}  */ var s;")
        .transpilesTo("var s: Object<string, number>;");

  }

  private SourceTranslationSubject assertSource(String... s) {
    return new SourceTranslationSubject(THROW_ASSERTION_ERROR, s);
  }

  private class SourceTranslationSubject
      extends Subject<SourceTranslationSubject, String[]> {

    public SourceTranslationSubject(FailureStrategy failureStrategy, String[] s)
    {
      super(failureStrategy, s);
    }

    private String doCompile(String... lines) {
      compiler.init(externsInputs,
          asList(SourceFile.fromCode("expected", Joiner.on("\n").join(lines))),
          getOptions());
      Node root = compiler.parseInputs();
      assertEquals("Parsing error: " + Arrays.toString(compiler.getErrors()),
          0, compiler.getErrorCount());
      getProcessor(compiler).process(root.getFirstChild(), root.getLastChild());
      return compiler.toSource();
    }

    public void transpilesTo(String... lines) {
      Truth.assertThat(doCompile(getSubject()).trim())
          .is("'use strict';" + Joiner.on("\n").join(lines));
    }
  }
}
