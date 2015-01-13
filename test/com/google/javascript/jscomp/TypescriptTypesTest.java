/*
 * Copyright 2014 The Closure Compiler Authors.
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
import com.google.javascript.rhino.Node;

import static java.util.Arrays.asList;

/**
 *
 */
public class TypescriptTypesTest extends CompilerTestCase {

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
    options.setLanguageOut(LanguageMode.ATSCRIPT);
    // Note that in this context, turning on the checkTypes option won't
    // actually cause the type check to run.
    options.checkTypes = parseTypeInfo;

    return options;
  }

  @Override
  public CompilerPass getProcessor(Compiler compiler) {
    PhaseOptimizer optimizer = new PhaseOptimizer(compiler, null, null);
    return optimizer;

  }

  @Override
  protected int getNumRepetitions() {
    return 1;
  }

  public void testVariableDeclarationAddsType() {
    assertCompiled("/** @type {string} */ var print; print = \"hello\";",
        "var print:string;",
        "print = \"hello\";");
  }

  public void testVariableDeclarationWithoutDeclaredType() throws Exception {
    assertCompiled("var print; print = \"hello\";",
        "var print;",
        "print = \"hello\";");
  }

  public void testFunctionReturnType() throws Exception {
    assertCompiled("/** @return {boolean} */ function b(){}",
        "function b():boolean {",
        "}",
        ";");
  }

  public void testFunctionParameterTypes() throws Exception {
    assertCompiled("/** @param {number} n @param {string} s */ function t(n,s){}",
        "function t(n:number, s:string) {",
        "}",
        ";");
  }

  public void testFunctionInsideAssignment() throws Exception {
    assertCompiled("/** @param {boolean} b @return {boolean} */ var f = function(b){return !b};",
        "var f = function(b:boolean):boolean {",
        "  return!b;",
        "};");
  }

  private void assertCompiled(String source, String... expected) {
    compiler.init(externsInputs, asList(SourceFile.fromCode("expected", source)), getOptions());
    Node root = compiler.parseInputs();
    assertEquals(Joiner.on("\n").join(expected), compiler.toSource().trim());
  }
}
