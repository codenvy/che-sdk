package org.eclipse.che.plugin.languageserver.ide.editor.codeassist.snippet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.eclipse.che.ide.api.editor.link.HasLinkedMode;
import org.eclipse.che.ide.api.editor.link.LinkedModel;
import org.eclipse.che.ide.api.editor.link.LinkedModelData;
import org.eclipse.che.ide.api.editor.link.LinkedModelGroup;
import org.eclipse.che.ide.api.editor.text.Position;
import org.eclipse.che.ide.util.Pair;

public class SnippetResolver {

  private VariableResolver varResolver;

  public SnippetResolver(VariableResolver varResolver) {
    this.varResolver = varResolver;
  }

  private static class TabStop {
    List<Position> positions = new ArrayList<>();
    List<String> values = new ArrayList<>();

    public void addPosition(Position p) {
      positions.add(p);
    }

    public List<Position> getPositions() {
      return positions;
    }

    public void setValues(List<String> values) {
      this.values = new ArrayList<>(values);
    }

    public List<String> getValues() {
      return values;
    }
  }

  public Pair<String, LinkedModel> resolve(
      String snippetText, HasLinkedMode editor, int startPosition) {
    StringBuilder b = new StringBuilder();
    Snippet p = new SnippetParser(snippetText).parse();
    Map<Integer, TabStop> groups = new HashMap<>();
    p.accept(
        new ExpressionVisitor() {
          private int nextArtificialGroup = Integer.MAX_VALUE / 2;
          private Stack<TabStop> currentGroup = new Stack<>();

          @Override
          public void visit(Variable e) {
            if (varResolver.isVar(e.getName())) {
              String v = varResolver.resolve(e.getName());
              if (v == null) {
                if (e.getValue() != null) {
                  e.getValue().accept(this);
                } else {
                  b.append("");
                }
              } else {
                b.append(v);
              }
            } else {
              TabStop group = new TabStop();
              group.addPosition(new Position(startPosition + b.length(), e.getName().length()));
              groups.put(nextArtificialGroup++, group);
              b.append(e.getName());
            }
          }

          @Override
          public void visit(Text e) {
            b.append(e.getValue());
          }

          @Override
          public void visit(Snippet e) {
            for (Expression expr : e.getExpressions()) {
              expr.accept(this);
            }
          }

          @Override
          public void visit(Placeholder e) {
            TabStop group = groups.get(e.getId());
            if (group == null) {
              group = new TabStop();
              groups.put(e.getId(), group);
            }
            int start = b.length();
            currentGroup.push(group);
            if (e.getValue() != null) {
              e.getValue().accept(this);
            }
            group.addPosition(new Position(startPosition + start, b.length() - start));
            currentGroup.pop();
          }

          @Override
          public void visit(Choice e) {
            TabStop group = currentGroup.peek();
            group.setValues(e.getChoices());
          }

          @Override
          public void visit(DollarExpression e) {
            e.getValue().accept(this);
          }
        });

    if (!groups.isEmpty()) {
      LinkedModel model = editor.createLinkedModel();
      List<LinkedModelGroup> modeGroups = new ArrayList<>();
      groups
          .entrySet()
          .stream()
          .sorted((left, right) -> left.getKey() - right.getKey())
          .forEach(
              (entry) -> {
                if (entry.getKey() == 0) {
                  model.setEscapePosition(entry.getValue().getPositions().get(0).getOffset());
                } else {
                  LinkedModelGroup group = editor.createLinkedGroup();
                  group.setPositions(entry.getValue().getPositions());
                  LinkedModelData data = editor.createLinkedModelData();
                  data.setType("link");
                  data.setValues(entry.getValue().getValues());
                  group.setData(data);
                  modeGroups.add(group);
                }
              });
      model.setGroups(modeGroups);
      return Pair.of(b.toString(), model);
    } else {
      return Pair.of(b.toString(), null);
    }
  }
}
