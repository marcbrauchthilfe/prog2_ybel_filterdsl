package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import org.junit.jupiter.api.Test;

public class AstTest {

  private void check(String query, String expected) {
    Expr resultPattern = AstBuilders.fromQuery(query, new AstBuilderPattern()::translate);
    Expr resultVisitor = AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate);

    assertEquals(expected, AstPrinter.toString(resultPattern));
    assertEquals(expected, AstPrinter.toString(resultVisitor));
  }

  @Test
  void simple_comparison() {
    check("age > 18", "(age > 18)");
  }

  @Test
  void string_comparison() {
    check("name == \"Bob\"", "(name == \"Bob\")");
  }

  @Test
  void and_expression() {
    check("age > 18 and name == \"Bob\"", "((age > 18) and (name == \"Bob\"))");
  }

  @Test
  void or_expression() {
    check("age > 18 or age < 2", "((age > 18) or (age < 2))");
  }

  @Test
  void not_expression() {
    check("not age > 18", "(not (age > 18))");
  }

  @Test
  void double_not() {
    check("not not active == \"true\"", "(active == \"true\")");
  }

  @Test
  void parentheses_override_precedence() {
    check(
        "(age > 18 or age < 2) and active == \"true\"",
        "(((age > 18) or (age < 2)) and (active == \"true\"))");
  }

  @Test
  void and_binds_stronger_than_or() {
    check("a == 1 or b == 2 and c == 3", "((a == 1) or ((b == 2) and (c == 3)))");
  }

  @Test
  void in_list_with_strings() {
    check(
        "status in (\"active\", \"pending\", \"closed\")",
        "(status in (\"active\", \"pending\", \"closed\"))");
  }

  @Test
  void in_list_with_numbers() {
    check("code in (1, 2, 3)", "(code in (1, 2, 3))");
  }
}
