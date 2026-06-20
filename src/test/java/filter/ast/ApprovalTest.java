package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

public class ApprovalTest {

  private final AstBuilderVisitor visitor = new AstBuilderVisitor();
  private final AstBuilderPattern pattern = new AstBuilderPattern();

  private String printV(String query) {
    Expr e = visitor.translate(AstBuilders.parse(query));
    return AstPrinter.toString(e);
  }

  private String printP(String query) {
    Expr e = pattern.translate(AstBuilders.parse(query));
    return AstPrinter.toString(e);
  }

  @Test
  void approval_simple_comparison_visitor() {
    Approvals.verify(printV("artist == \"Beatles\""));
  }

  @Test
  void approval_simple_comparison_pattern() {
    Approvals.verify(printP("artist == \"Beatles\""));
  }

  @Test
  void approval_and_expression_visitor() {
    Approvals.verify(printV("artist == \"Beatles\" and year == 1965"));
  }

  @Test
  void approval_and_expression_pattern() {
    Approvals.verify(printP("artist == \"Beatles\" and year == 1965"));
  }

  @Test
  void approval_or_expression_visitor() {
    Approvals.verify(printV("artist == \"Beatles\" or year <= 1965"));
  }

  @Test
  void approval_not_expression_visitor() {
    Approvals.verify(printV("not artist == \"Beatles\""));
  }

  @Test
  void approval_in_list_visitor() {
    Approvals.verify(printV("genre in (\"rock\", \"jazz\", \"metal\")"));
  }

  @Test
  void approval_in_list_pattern() {
    Approvals.verify(printP("genre in (\"rock\", \"jazz\", \"metal\")"));
  }

  @Test
  void approval_complex_query_visitor() {
    Approvals.verify(
        printV("genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\""));
  }

  @Test
  void approval_complex_query_pattern() {
    Approvals.verify(
        printP("genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\""));
  }

  @Test
  void approval_parenthesized_visitor() {
    Approvals.verify(printV("(year <= 1990 or artist == \"Beatles\") and year > 1960"));
  }

  @Test
  void approval_parenthesized_pattern() {
    Approvals.verify(printP("(year <= 1990 or artist == \"Beatles\") and year > 1960"));
  }

  @Test
  void approval_both_builders_same_output() {
    String query = "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"";
    Approvals.verify(printV(query) + "\n" + printP(query));
  }
}
