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

import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import static com.google.javascript.rhino.Node.DECLARED_TYPE_EXPR;

/**
 * Copies type declarations from the JSDoc (possibly of a parent node)
 * to a property on a node which represents a typed language element.
 */
public class Es6TypeDeclarations extends AbstractPostOrderCallback
    implements HotSwapCompilerPass {

  private final AbstractCompiler compiler;

  public Es6TypeDeclarations(AbstractCompiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void visit(NodeTraversal t, Node n, Node parent) {

    switch (n.getType()) {
      case Token.NAME:
        if (parent == null) {
          break;
        }
        JSDocInfo parentJSDoc = NodeUtil.getBestJSDocInfo(parent);
        if (parentJSDoc == null) {
          break;
        }
        if (parent.isVar()) {
          n.putProp(DECLARED_TYPE_EXPR, parentJSDoc.getType());
        }
        if (parent.isFunction()) {
          n.putProp(DECLARED_TYPE_EXPR, parentJSDoc.getReturnType());
        }
        if (parent.isParamList()) {
          JSTypeExpression parameterType = parentJSDoc.getParameterType(n.getString());
          if (parameterType != null) {
            n.putProp(DECLARED_TYPE_EXPR, parameterType);
          }
        }
    }
  }

  @Override
  public void hotSwapScript(Node scriptRoot, Node originalRoot) {
    NodeTraversal.traverseRoots(compiler, this, scriptRoot, originalRoot);
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverseRoots(compiler, this, externs, root);
  }
}
