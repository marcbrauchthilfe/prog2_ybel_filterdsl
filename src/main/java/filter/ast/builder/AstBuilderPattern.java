package filter.ast.builder;

import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.List;

public class AstBuilderPattern {

  public Expr translate(FilterParser.QueryContext ctx) {
    return buildExpr(ctx.expr());
  }

  private Expr buildExpr(FilterParser.ExprContext ctx) {
    return buildOrExpr(ctx.orExpr());
  }

  private Expr buildOrExpr(FilterParser.OrExprContext ctx) {
    List<FilterParser.AndExprContext> parts = ctx.andExpr();
    Expr result = buildAndExpr(parts.getFirst());
    for (int i = 1; i < parts.size(); i++) {
      result = new Expr.Or(result, buildAndExpr(parts.get(i)));
    }
    return result;
  }

  private Expr buildAndExpr(FilterParser.AndExprContext ctx) {
    List<FilterParser.NotExprContext> parts = ctx.notExpr();
    Expr result = buildNotExpr(parts.getFirst());
    for (int i = 1; i < parts.size(); i++) {
      result = new Expr.And(result, buildNotExpr(parts.get(i)));
    }
    return result;
  }

  private Expr buildNotExpr(FilterParser.NotExprContext ctx) {
    if (ctx.NOT() != null) {
      return new Expr.Not(buildNotExpr(ctx.notExpr()));
    } else {
      return buildPrimary(ctx.primary());
    }
  }

  private Expr buildPrimary(FilterParser.PrimaryContext ctx) {
    if (ctx.comparison() != null) {
      return buildComparison(ctx.comparison());
    } else {
      return buildExpr(ctx.expr());
    }
  }

  private Expr buildComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.IDENTIFIER().getText();
    if (ctx.IN() != null) {
      List<Value> values = buildLiteralList(ctx.literalList());
      return new Expr.InList(field, values);
    } else {
      CompOp op = CompOp.fromSymbol(ctx.op.getText());
      Value val = buildLiteral(ctx.value);
      return new Expr.Comparison(field, op, val);
    }
  }

  private List<Value> buildLiteralList(FilterParser.LiteralListContext ctx) {
    return ctx.literal().stream().map(this::buildLiteral).toList();
  }

  private Value buildLiteral(FilterParser.LiteralContext ctx) {
    if (ctx.STRING() != null) {
      String raw = ctx.STRING().getText();
      return new Value.Str(raw.substring(1, raw.length() - 1));
    } else {
      return new Value.Num(Integer.parseInt(ctx.NUMBER().getText()));
    }
  }
}
