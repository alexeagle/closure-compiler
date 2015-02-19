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

import static com.google.javascript.jscomp.NodeUtil.getPrototypeClassName;
import static com.google.javascript.jscomp.NodeUtil.getPrototypePropertyName;
import static com.google.javascript.jscomp.NodeUtil.isPrototypePropertyDeclaration;
import static com.google.javascript.jscomp.parsing.TypeDeclarationsIRFactory.convert;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.IR;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Node.TypeDeclarationNode;
import com.google.javascript.rhino.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts JS with types in jsdocs to an extended JS syntax that includes types.
 * (Still keeps the jsdocs intact.)
 *
 * @author alexeagle@google.com (Alex Eagle)
 *
 * TODO(alexeagle): handle inline-style JSDoc annotations as well.
 */
public class ConvertToTypedES6
    extends AbstractPostOrderCallback implements CompilerPass {

  private final AbstractCompiler compiler;
  private final Map<String, Node> classMemberRoots = new HashMap<>();

  public ConvertToTypedES6(AbstractCompiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverse(compiler, root, this);
  }

  @Override
  public void visit(NodeTraversal t, Node n, Node parent) {
    JSDocInfo bestJSDocInfo = NodeUtil.getBestJSDocInfo(n);
    switch (n.getType()) {
      case Token.FUNCTION:
        Node anonymousCopy = IR.function(
            IR.name(""),
            n.getChildAtIndex(1).cloneTree(),
            n.getLastChild().cloneTree());

        if (bestJSDocInfo != null) {
          setTypeExpression(n, bestJSDocInfo.getReturnType());
          setTypeExpression(anonymousCopy, bestJSDocInfo.getReturnType());
          if (bestJSDocInfo.isConstructor() || bestJSDocInfo.isInterface()) {
            visitConstructor(n, parent, anonymousCopy);
          }
        }
        if (isPrototypePropertyDeclaration(parent.getParent())) {
          visitClassMember(parent, anonymousCopy,
              getPrototypeClassName(parent.getFirstChild()).getQualifiedName(),
              getPrototypePropertyName(parent.getFirstChild()));
        }
        break;
      case Token.NAME:
      case Token.GETPROP:
        if (parent == null) {
          break;
        }
        if (parent.isVar() || parent.isAssign() || parent.isExprResult()) {
          if (bestJSDocInfo != null) {
            setTypeExpression(n, bestJSDocInfo.getType());
          }
        } else if (parent.isParamList()) {
          JSDocInfo parentDocInfo = NodeUtil.getBestJSDocInfo(parent);
          if (parentDocInfo == null) {
            break;
          }
          JSTypeExpression parameterType =
              parentDocInfo.getParameterType(n.getString());
          if (parameterType != null) {
            Node attachTypeExpr = n;
            // Modify the primary AST to represent a function parameter as a
            // REST node, if the type indicates it is a rest parameter.
            if (parameterType.getRoot().getType() == Token.ELLIPSIS) {
              attachTypeExpr = Node.newString(Token.REST, n.getString());
              n.getParent().replaceChild(n, attachTypeExpr);
              compiler.reportCodeChange();
            }
            setTypeExpression(attachTypeExpr, parameterType);
          }
        }
        break;
      default:
        break;
    }
  }

  private void visitConstructor(Node originalFunc, Node parent, Node anonymousCopy) {
    Node toReplace;
    Node name;
    if (parent.isName() && parent.getParent().isVar()) {
      toReplace = parent.getParent();
      name = parent;
    } else {
      toReplace = originalFunc;
      name = originalFunc.getFirstChild();
    }
    Node members = new Node(Token.CLASS_MEMBERS);
    boolean hasConstructorStatements = originalFunc.getLastChild().hasChildren();
    if (hasConstructorStatements) {
      members.addChildToBack(IR.memberFunctionDef("constructor", anonymousCopy));
    }
    Node asClass = new Node(Token.CLASS, name.cloneNode(), IR.empty(), members);
    classMemberRoots.put(name.getQualifiedName(), members);

    asClass.useSourceInfoIfMissingFromForTree(toReplace);
    toReplace.getParent().replaceChild(toReplace, asClass);
    compiler.reportCodeChange();
  }

  private void visitClassMember(Node parent, Node memberFunc, String owningClass, String memberName) {
    Node members = classMemberRoots.get(owningClass);
    Preconditions.checkNotNull(members, "Didn't previously create class " + owningClass);
    members.addChildToBack(IR.memberFunctionDef(memberName, memberFunc));
    members.useSourceInfoIfMissingFromForTree(parent.getParent());
    parent.getParent().getParent().removeChild(parent.getParent());
    compiler.reportCodeChange();
  }

  private void setTypeExpression(Node n, JSTypeExpression type) {
    TypeDeclarationNode node = convert(type);
    if (node != null) {
      n.setDeclaredTypeExpression(node);
      compiler.reportCodeChange();
    }
  }
}
