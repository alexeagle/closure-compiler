package com.google.javascript.jscomp;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import junit.framework.TestCase;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class Es6TypeDeclarationsTest extends TestCase {

  public void testVar() throws Exception {
    assertAbout(compile("/** @type {string} */ var s;"))
        .that("s").hasType("string");
  }

  public void testFunction() throws Exception {
    assertAbout(compile("/** @return {boolean} */ function b(){}"))
        .that("b").hasType("boolean");
  }

  public void testFunctionParameters() throws Exception {
    assertAbout(compile("/** @param {number} n @param {string} s */ function t(n,s){}"))
        .that("n").hasType("number");
  }

  public SubjectFactory<JSTypeExprSubject, String> compile(String js) {
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

    return new SubjectFactory<JSTypeExprSubject, String>() {
      @Override
      public JSTypeExprSubject getSubject(FailureStrategy failureStrategy, String identifier) {
        FindNode visitor = new FindNode(identifier);
        Node root = compiler.getRoot().getLastChild();
        NodeTraversal.traverse(compiler, root, visitor);
        assertWithMessage("Did not find a node named " + identifier + " in " + root.toStringTree())
            .that(visitor.foundNode).isNotNull();
        JSTypeExpression declaredType = visitor.foundNode.getJSTypeExpression();
        assertWithMessage(identifier + " missing DECLARED_TYPE_EXPR in " + root.toStringTree())
            .that(declaredType).isNotNull();

        return new JSTypeExprSubject(failureStrategy, identifier, declaredType);
      }
    };
  }

  private class JSTypeExprSubject extends Subject<JSTypeExprSubject, String> {
    private final JSTypeExpression typeExpr;

    public JSTypeExprSubject(FailureStrategy fs, String identifier, JSTypeExpression typeExpr) {
      super(fs, identifier);
      this.typeExpr = typeExpr;
    }

    public void hasType(String type) {
      assertTrue(getSubject() + " is of type " + typeExpr + " not of type " + type,
          Node.newString(type).isEquivalentTo(typeExpr.getRoot()));
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
