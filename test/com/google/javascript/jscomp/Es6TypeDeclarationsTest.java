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
import static com.google.javascript.rhino.Node.TypeDeclarationNode;
import static com.google.javascript.jscomp.parsing.TypeDeclarationsIRFactory.booleanType;
import static com.google.javascript.jscomp.parsing.TypeDeclarationsIRFactory.numberType;
import static com.google.javascript.jscomp.parsing.TypeDeclarationsIRFactory.stringType;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;
import com.google.javascript.rhino.Node;

import junit.framework.TestCase;

public class Es6TypeDeclarationsTest extends TestCase {

  public void testVar() throws Exception {
    assertAbout(compile("/** @type {string} */ var s;"))
        .that("s").hasType(stringType());
  }

  public void testFunction() throws Exception {
    assertAbout(compile("/** @return {boolean} */ function b(){}"))
        .that("b").hasType(booleanType());
  }

  public void testFunctionParameters() throws Exception {
    assertAbout(compile("/** @param {number} n @param {string} s */ function t(n,s){}"))
        .that("n").hasType(numberType());
  }

  public SubjectFactory<TypeExprSubject, String> compile(String js) {
    final Compiler compiler = new Compiler();

    SourceFile input = SourceFile.fromCode("js", js);
    CompilerOptions options = new CompilerOptions();
    options.setRenamingPolicy(VariableRenamingPolicy.OFF, PropertyRenamingPolicy.OFF);
    compiler.init(ImmutableList.<SourceFile>of(), ImmutableList.of(input), options);
    compiler.parseInputs();

    CompilerPass pass = new Es6TypeDeclarations(compiler);
    pass.process(
        compiler.getRoot().getFirstChild(),
        compiler.getRoot().getLastChild());

    return new SubjectFactory<TypeExprSubject, String>() {
      @Override
      public TypeExprSubject getSubject(FailureStrategy failureStrategy, String identifier) {
        FindNode visitor = new FindNode(identifier);
        Node root = compiler.getRoot().getLastChild();
        NodeTraversal.traverse(compiler, root, visitor);
        assertWithMessage("Did not find a node named " + identifier + " in " + root.toStringTree())
            .that(visitor.foundNode).isNotNull();
        TypeDeclarationNode declaredType = visitor.foundNode.getDeclaredTypeExpression();
        assertWithMessage(identifier + " missing DECLARED_TYPE_EXPR in " + root.toStringTree())
            .that(declaredType).isNotNull();

        return new TypeExprSubject(failureStrategy, identifier, declaredType);
      }
    };
  }

  private class TypeExprSubject extends Subject<TypeExprSubject, String> {
    private final TypeDeclarationNode typeExpr;

    public TypeExprSubject(FailureStrategy fs, String identifier, TypeDeclarationNode typeExpr) {
      super(fs, identifier);
      this.typeExpr = typeExpr;
    }

    public void hasType(TypeDeclarationNode type) {
      assertTrue(getSubject() + " is of type " + typeExpr + " not of type " + type,
          type.isEquivalentTo(typeExpr));
    }
  }

  private static class FindNode extends NodeTraversal.AbstractPostOrderCallback {
    final String name;
    Node foundNode;

    FindNode(String name) {
      this.name = name;
    }

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      // Return type is attached to FUNCTION node, but the qualifiedName is on the child NAME node.
      if (parent != null && parent.isFunction() && n.matchesQualifiedName(name)) {
        foundNode = parent;
      } else if (n.matchesQualifiedName(name)) {
        foundNode = n;
      }
    }
  }
}
