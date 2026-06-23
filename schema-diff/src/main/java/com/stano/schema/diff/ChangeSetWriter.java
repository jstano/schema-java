package com.stano.schema.diff;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.model.Column;
import java.io.PrintWriter;

public class ChangeSetWriter {

  public void write(ChangeSet changeSet, PrintWriter writer) {
    writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.println("<changeset>");

    for (SchemaChange change : changeSet.getChanges()) {
      writeChange(change, writer);
    }

    writer.println("</changeset>");
    writer.flush();
  }

  private void writeChange(SchemaChange change, PrintWriter writer) {
    if (change instanceof AddTableChange c) {
      writeAddTable(c, writer);
    } else if (change instanceof DropTableChange c) {
      writeDropTable(c, writer);
    } else if (change instanceof RenameTableChange c) {
      writeRenameTable(c, writer);
    } else if (change instanceof AddColumnChange c) {
      writeAddColumn(c, writer);
    } else if (change instanceof DropColumnChange c) {
      writeDropColumn(c, writer);
    } else if (change instanceof RenameColumnChange c) {
      writeRenameColumn(c, writer);
    } else if (change instanceof ModifyColumnChange c) {
      writeModifyColumn(c, writer);
    } else if (change instanceof AddKeyChange c) {
      writeAddKey(c, writer);
    } else if (change instanceof DropKeyChange c) {
      writeDropKey(c, writer);
    } else if (change instanceof AddConstraintChange c) {
      writeAddConstraint(c, writer);
    } else if (change instanceof DropConstraintChange c) {
      writeDropConstraint(c, writer);
    } else if (change instanceof AddRelationChange c) {
      writeAddRelation(c, writer);
    } else if (change instanceof DropRelationChange c) {
      writeDropRelation(c, writer);
    } else if (change instanceof AddViewChange c) {
      writeAddView(c, writer);
    } else if (change instanceof DropViewChange c) {
      writeDropView(c, writer);
    }
  }

  private void writeAddTable(AddTableChange change, PrintWriter writer) {
    writer.println(String.format("    <add-table name=\"%s\"/>", xmlEscape(change.getTableName())));
  }

  private void writeDropTable(DropTableChange change, PrintWriter writer) {
    writer.println(
        String.format("    <drop-table name=\"%s\"/>", xmlEscape(change.getTableName())));
  }

  private void writeRenameTable(RenameTableChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <rename-table old-name=\"%s\" new-name=\"%s\"/>",
            xmlEscape(change.getOldName()), xmlEscape(change.getNewName())));
  }

  private void writeRenameColumn(RenameColumnChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <rename-column table=\"%s\" old-name=\"%s\" new-name=\"%s\"/>",
            xmlEscape(change.getTableName()),
            xmlEscape(change.getOldName()),
            xmlEscape(change.getNewName())));
  }

  private void writeAddColumn(AddColumnChange change, PrintWriter writer) {
    Column col = change.getColumn();
    StringBuilder sb = new StringBuilder();
    sb.append("    <add-column table=\"").append(xmlEscape(change.getTableName())).append("\"");
    sb.append(" name=\"").append(xmlEscape(col.getName())).append("\"");
    sb.append(" type=\"").append(col.getType().name().toLowerCase()).append("\"");
    if (col.getLength() > 0) {
      sb.append(" length=\"").append(col.getLength()).append("\"");
    }
    if (col.getScale() > 0) {
      sb.append(" scale=\"").append(col.getScale()).append("\"");
    }
    sb.append(" required=\"").append(col.isRequired()).append("\"");
    if (col.getDefaultConstraint() != null) {
      sb.append(" default=\"").append(xmlEscape(col.getDefaultConstraint())).append("\"");
    }
    sb.append("/>");
    writer.println(sb.toString());
  }

  private void writeDropColumn(DropColumnChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <drop-column table=\"%s\" name=\"%s\"/>",
            xmlEscape(change.getTableName()), xmlEscape(change.getColumnName())));
  }

  private void writeModifyColumn(ModifyColumnChange change, PrintWriter writer) {
    Column oldCol = change.getOldColumn();
    Column newCol = change.getNewColumn();
    StringBuilder sb = new StringBuilder();
    sb.append("    <modify-column table=\"").append(xmlEscape(change.getTableName())).append("\"");
    sb.append(" name=\"").append(xmlEscape(newCol.getName())).append("\"");
    sb.append(" old-type=\"").append(oldCol.getType().name().toLowerCase()).append("\"");
    if (oldCol.getLength() > 0) {
      sb.append(" old-length=\"").append(oldCol.getLength()).append("\"");
    }
    if (oldCol.getScale() > 0) {
      sb.append(" old-scale=\"").append(oldCol.getScale()).append("\"");
    }
    sb.append(" old-required=\"").append(oldCol.isRequired()).append("\"");
    if (oldCol.getDefaultConstraint() != null) {
      sb.append(" old-default=\"").append(xmlEscape(oldCol.getDefaultConstraint())).append("\"");
    }
    sb.append(" new-type=\"").append(newCol.getType().name().toLowerCase()).append("\"");
    if (newCol.getLength() > 0) {
      sb.append(" new-length=\"").append(newCol.getLength()).append("\"");
    }
    if (newCol.getScale() > 0) {
      sb.append(" new-scale=\"").append(newCol.getScale()).append("\"");
    }
    sb.append(" new-required=\"").append(newCol.isRequired()).append("\"");
    if (newCol.getDefaultConstraint() != null) {
      sb.append(" new-default=\"").append(xmlEscape(newCol.getDefaultConstraint())).append("\"");
    }
    sb.append("/>");
    writer.println(sb.toString());
  }

  private void writeAddKey(AddKeyChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <add-key table=\"%s\" type=\"%s\" columns=\"%s\"/>",
            xmlEscape(change.getTableName()),
            change.getKey().getType().name().toLowerCase(),
            xmlEscape(change.getKey().getColumnsAsString())));
  }

  private void writeDropKey(DropKeyChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <drop-key table=\"%s\" type=\"%s\" columns=\"%s\"/>",
            xmlEscape(change.getTableName()),
            change.getKey().getType().name().toLowerCase(),
            xmlEscape(change.getKey().getColumnsAsString())));
  }

  private void writeAddConstraint(AddConstraintChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <add-constraint table=\"%s\" name=\"%s\" sql=\"%s\"/>",
            xmlEscape(change.getTableName()),
            xmlEscape(change.getConstraint().getName()),
            xmlEscape(change.getConstraint().getSql())));
  }

  private void writeDropConstraint(DropConstraintChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <drop-constraint table=\"%s\" name=\"%s\"/>",
            xmlEscape(change.getTableName()), xmlEscape(change.getConstraintName())));
  }

  private void writeAddRelation(AddRelationChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <add-relation from-table=\"%s\" from-column=\"%s\" to-table=\"%s\""
                + " to-column=\"%s\" type=\"%s\"/>",
            xmlEscape(change.getRelation().getFromTableName()),
            xmlEscape(change.getRelation().getFromColumnName()),
            xmlEscape(change.getRelation().getToTableName()),
            xmlEscape(change.getRelation().getToColumnName()),
            change.getRelation().getType().name().toLowerCase()));
  }

  private void writeDropRelation(DropRelationChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <drop-relation from-table=\"%s\" from-column=\"%s\" to-table=\"%s\""
                + " to-column=\"%s\"/>",
            xmlEscape(change.getRelation().getFromTableName()),
            xmlEscape(change.getRelation().getFromColumnName()),
            xmlEscape(change.getRelation().getToTableName()),
            xmlEscape(change.getRelation().getToColumnName())));
  }

  private void writeAddView(AddViewChange change, PrintWriter writer) {
    writer.println(
        String.format(
            "    <add-view name=\"%s\" sql=\"%s\"/>",
            xmlEscape(change.getView().getName()), xmlEscape(change.getView().getSql())));
  }

  private void writeDropView(DropViewChange change, PrintWriter writer) {
    writer.println(String.format("    <drop-view name=\"%s\"/>", xmlEscape(change.getViewName())));
  }

  private String xmlEscape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
