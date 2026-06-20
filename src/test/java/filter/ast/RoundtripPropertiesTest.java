package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import net.jqwik.api.*;

public class RoundtripPropertiesTest {

  private final AstBuilderVisitor visitor = new AstBuilderVisitor();
  private final AstBuilderPattern pattern = new AstBuilderPattern();

  @Property
  boolean roundtrip_pattern(@ForAll("simpleQueries") String query) {
    Expr first = pattern.translate(AstBuilders.parse(query));
    String printed = AstPrinter.toString(first);
    // The printer wraps everything in parens, which is valid syntax
    Expr second = pattern.translate(AstBuilders.parse(printed));
    return first.equals(second);
  }

  @Property
  boolean roundtrip_visitor(@ForAll("simpleQueries") String query) {
    Expr first = visitor.translate(AstBuilders.parse(query));
    String printed = AstPrinter.toString(first);
    Expr second = visitor.translate(AstBuilders.parse(printed));
    return first.equals(second);
  }

  @Property
  boolean crossRoundtrip_patternThenVisitor(@ForAll("simpleQueries") String query) {
    Expr fromPattern = pattern.translate(AstBuilders.parse(query));
    String printed = AstPrinter.toString(fromPattern);
    Expr fromVisitor = visitor.translate(AstBuilders.parse(printed));
    return fromPattern.equals(fromVisitor);
  }

  @Property
  boolean bothBuildersSameResult(@ForAll("simpleQueries") String query) {
    Expr fromPattern = pattern.translate(AstBuilders.parse(query));
    Expr fromVisitor = visitor.translate(AstBuilders.parse(query));
    return fromPattern.equals(fromVisitor);
  }

  @Property
  boolean simplifyIdempotent(@ForAll("simpleQueries") String query) {
    Expr e = pattern.translate(AstBuilders.parse(query));
    Expr once = AstBuilders.simplify(e);
    Expr twice = AstBuilders.simplify(once);
    return once.equals(twice);
  }

  @Property
  boolean doubleNegationEliminated(@ForAll("simpleQueries") String query) {
    Expr e = pattern.translate(AstBuilders.parse(query));
    Expr doubleNot = new filter.ast.nodes.Expr.Not(new filter.ast.nodes.Expr.Not(e));
    Expr simplified = AstBuilders.simplify(doubleNot);
    return simplified.equals(AstBuilders.simplify(e));
  }

  @Provide
  Arbitrary<String> fields() {
    return Arbitraries.of("title", "artist", "genre", "year");
  }

  @Provide
  Arbitrary<String> stringLiterals() {
    return Arbitraries.strings()
        .withChars("abcxyz")
        .ofMinLength(1)
        .ofMaxLength(5)
        .map(s -> "\"" + s + "\"");
  }

  @Provide
  Arbitrary<String> numberLiterals() {
    return Arbitraries.integers().between(1900, 2025).map(Object::toString);
  }

  @Provide
  Arbitrary<String> comparisons() {
    Arbitrary<String> ops = Arbitraries.of("==", "!=", "<", "<=", ">", ">=");

    Arbitrary<String> stringComp =
        Combinators.combine(fields(), ops, stringLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    Arbitrary<String> numberComp =
        Combinators.combine(Arbitraries.of("year"), ops, numberLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    return Arbitraries.oneOf(stringComp, numberComp);
  }

  @Provide
  Arbitrary<String> simpleQueries() {
    return comparisons()
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(
            list -> {
              if (list.size() == 1) return list.getFirst();
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                  String conn = Arbitraries.of(" and ", " or ").sample();
                  sb.append(conn);
                }
                sb.append(list.get(i));
              }
              return sb.toString();
            });
  }
}
