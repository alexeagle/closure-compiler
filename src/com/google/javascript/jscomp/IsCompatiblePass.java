package com.google.javascript.jscomp;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.javascript.rhino.Node;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Wraps a PassFactory with a guard for whether the pass is compatible
 * with the current compiler options.
 * Incompatible passes will warn the user, and are disabled.
 * The intent is to allow a degraded compiler experience while an
 * experimental, breaking language feature is under development.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class IsCompatiblePass implements Function<PassFactory, PassFactory> {

  private static final Logger logger =
      Logger.getLogger(IsCompatiblePass.class.getName());

  private CompilerOptions options;

  public IsCompatiblePass(CompilerOptions options) {
    this.options = options;
  }

  @Override
  public PassFactory apply(final PassFactory passFactory) {
    if (options.getLanguageOut().isEs6OrHigher()) {
      return new PassFactory(passFactory.getName() + " [isCompatiblePass?]",
          passFactory.isOneTimePass()) {
        @Override
        CompilerPass create(AbstractCompiler compiler) {
          CompilerPass compilerPass = passFactory.create(compiler);
          Optional<NotEs6Compatible> notEs6Compatible =
              findAnnotation(compilerPass.getClass(), NotEs6Compatible.class);
          if (notEs6Compatible.isPresent()) {
            logger.warning("Skipping pass " + passFactory.getName()
                + " because it is annotated with @NotEs6Compatible. See issue "
                + notEs6Compatible.get().issue());
            return new CompilerPass() {
              @Override
              public void process(Node externs, Node root) {
                // do nothing
              }
            };
          }
          return compilerPass;
        }};
    } else {
      return passFactory;
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Annotation> Optional<T> findAnnotation(
      Class clazz, final Class<T> annotationCls) {
    Optional<Annotation> found =
        Iterables.tryFind(Arrays.asList(clazz.getDeclaredAnnotations()),
            new Predicate<Annotation>() {
              @Override
              public boolean apply(Annotation annotation) {
                return annotation.annotationType().equals(annotationCls);
              }
            });
    if (found.isPresent()) {
      return Optional.of((T) found.get());
    } else {
      return Optional.absent();
    }
  }
}
