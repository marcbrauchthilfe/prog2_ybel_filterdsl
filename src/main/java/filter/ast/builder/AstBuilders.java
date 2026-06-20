package filter.ast.builder;

import filter.FilterLexer;
import filter.FilterParser;
import filter.ast.nodes.Expr;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class AstBuilders {

  public static Expr fromQuery(String query, Function<FilterParser.QueryContext, Expr> translator) {
    return simplify(translator.apply(parse(query)));
  }

  public static Expr simplify(Expr e) {
    return switch (e) {
      case Expr.Not(Expr.Not(var inner)) -> simplify(inner);
      case Expr.Not(var inner) -> new Expr.Not(simplify(inner));
      case Expr.And(var left, var right) -> {
        Expr sLeft = simplify(left);
        Expr sRight = simplify(right);
        yield sLeft.equals(sRight) ? sLeft : new Expr.And(sLeft, sRight);
      }
      case Expr.Or(var left, var right) -> {
        Expr sLeft = simplify(left);
        Expr sRight = simplify(right);
        yield sLeft.equals(sRight) ? sLeft : new Expr.Or(sLeft, sRight);
      }
      case Expr.Comparison c -> c;
      case Expr.InList iL -> iL;
    };
  }

  public static FilterParser.QueryContext parse(String query) {
    var cs = CharStreams.fromString(query);
    var lexer = new FilterLexer(cs);
    var tokens = new CommonTokenStream(lexer);
    var parser = new FilterParser(tokens);

    var ctx = parser.query();
    if (parser.getNumberOfSyntaxErrors() > 0)
      throw new IllegalStateException("Syntax errors in query: " + query);

    return ctx;
  }
}
