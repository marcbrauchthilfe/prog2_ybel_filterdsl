package filter.ast.builder;

import filter.FilterBaseVisitor;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class AstBuilderVisitor extends FilterBaseVisitor<Void> {

  private final Deque<Expr> exprs = new ArrayDeque<>();
  private final Deque<Value> values = new ArrayDeque<>();

  public Expr translate(FilterParser.QueryContext ctx) {
    exprs.clear();
    values.clear();
    visit(ctx);
    return exprs.pop();
  }

  @Override
  public Void visitQuery(FilterParser.QueryContext ctx) {
    visit(ctx.expr());
    return null;
  }

  @Override
  public Void visitExpr(FilterParser.ExprContext ctx) {
    visit(ctx.orExpr());
    return null;
  }

  @Override
  public Void visitOrExpr(FilterParser.OrExprContext ctx) {
    List<FilterParser.AndExprContext> andExprs = ctx.andExpr();
    visit(andExprs.get(0));
    for (int i = 1; i < andExprs.size(); i++) {
      visit(andExprs.get(i));
      Expr right = exprs.pop();
      Expr left = exprs.pop();
      exprs.push(new Expr.Or(left, right));
    }
    return null;
  }

  @Override
  public Void visitAndExpr(FilterParser.AndExprContext ctx) {
    List<FilterParser.NotExprContext> notExprs = ctx.notExpr();
    visit(notExprs.get(0));
    for (int i = 1; i < notExprs.size(); i++) {
      visit(notExprs.get(i));
      Expr right = exprs.pop();
      Expr left = exprs.pop();
      exprs.push(new Expr.And(left, right));
    }
    return null;
  }

  @Override
  public Void visitNotExpr(FilterParser.NotExprContext ctx) {
    if (ctx.NOT() != null) {
      visit(ctx.notExpr());
      exprs.push(new Expr.Not(exprs.pop()));
    } else {
      visit(ctx.primary());
    }
    return null;
  }

  @Override
  public Void visitPrimary(FilterParser.PrimaryContext ctx) {
    if (ctx.comparison() != null) {
      visit(ctx.comparison());
    } else {
      visit(ctx.expr());
    }
    return null;
  }

  @Override
  public Void visitComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.IDENTIFIER().getText();
    if (ctx.IN() != null) {
      visit(ctx.literalList());
      // collect all values that were pushed
      int count = ctx.literalList().literal().size();
      Value[] vals = new Value[count];
      for (int i = count - 1; i >= 0; i--) {
        vals[i] = values.pop();
      }
      exprs.push(new Expr.InList(field, List.of(vals)));
    } else {
      visit(ctx.value);
      Value val = values.pop();
      CompOp op = CompOp.fromSymbol(ctx.op.getText());
      exprs.push(new Expr.Comparison(field, op, val));
    }
    return null;
  }

  @Override
  public Void visitLiteralList(FilterParser.LiteralListContext ctx) {
    for (FilterParser.LiteralContext lit : ctx.literal()) {
      visit(lit);
    }
    return null;
  }

  @Override
  public Void visitLiteral(FilterParser.LiteralContext ctx) {
    if (ctx.STRING() != null) {
      String raw = ctx.STRING().getText();
      String text = raw.substring(1, raw.length() - 1);
      values.push(new Value.Str(text));
    } else {
      values.push(new Value.Num(Integer.parseInt(ctx.NUMBER().getText())));
    }
    return null;
  }
}
