package rs.ac.bg.etf.pp1.analysis;

import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class DumpSymbolTableVisitorWrapper extends DumpSymbolTableVisitor {

  @Override
  public void visitObjNode(Obj objToVisit) {
    switch (objToVisit.getKind()) {
    case 0: {
      this.output.append("Con ");
      break;
    }
    case 1: {
      this.output.append("Var ");
      break;
    }
    case 2: {
      this.output.append("Type ");
      break;
    }
    case 3: {
      this.output.append("Meth ");
      break;
    }
    case 4: {
      this.output.append("Fld ");
      break;
    }
    case 6: {
      this.output.append("Prog ");
    }
    }
    this.output.append(objToVisit.getName());
    this.output.append(": ");
    if (1 == objToVisit.getKind() && "this".equalsIgnoreCase(objToVisit.getName())) {
      this.output.append("");
    } else if (objToVisit.getKind() != Obj.Con) {
      objToVisit.getType().accept(this);
    }
    this.output.append(", ");
    this.output.append(objToVisit.getAdr());
    this.output.append(", ");
    this.output.append(String.valueOf(objToVisit.getLevel()) + "  ;  ");
    if (objToVisit.getKind() == 6 || objToVisit.getKind() == 3) {
      this.output.append("\n");
      this.nextIndentationLevel();
    }
    for (Obj o : objToVisit.getLocalSymbols()) {
      this.output.append(this.currentIndent.toString());
      o.accept(this);
      this.output.append("\n");
    }
    if (objToVisit.getKind() == 6 || objToVisit.getKind() == 3) {
      this.previousIndentationLevel();
    }
  }

  public void visitStructNode(Struct structToVisit) {
    switch (structToVisit.getKind()) {
    case Struct.None: {
      this.output.append("notype");
      break;
    }
    case Struct.Int: {
      this.output.append("int");
      break;
    }
    case Struct.Char: {
      this.output.append("char");
      break;
    }
    case Struct.Bool: {
      this.output.append("bool");
      break;
    }
    case Struct.Array: {
      this.output.append("Arr of ");
      switch (structToVisit.getElemType().getKind()) {
      case Struct.None: {
        this.output.append("notype");
        break;
      }
      case Struct.Int: {
        this.output.append("int");
        break;
      }
      case Struct.Char: {
        this.output.append("char");
        break;
      }
      case Struct.Class: {
        this.output.append("Class");
        break;
      }
      case Struct.Bool: {
        this.output.append("bool");
        break;
      }
      case Struct.Enum: {
        this.output.append("Enum");
        break;
      }
      }
      break;
    }
    case Struct.Class: {
      this.output.append("Class [");
      for (Obj obj : structToVisit.getMembers()) {
        obj.accept(this);
      }
      this.output.append("]");
      break;
    }
    case Struct.Enum: {
      this.output.append("Enum [");
      for (Obj obj : structToVisit.getMembers()) {
        this.visitObjNode(obj);
      }
      this.output.append("]");
      break;
    }

    }
  }
}
