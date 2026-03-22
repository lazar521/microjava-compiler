package rs.ac.bg.etf.pp1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
  Logger log = Logger.getLogger(Compiler.class);
  Stack<Integer> conditionalJumps = new Stack<>();
  Program program;
  File outFile;
  boolean errorsFound = false;
  int globalVariableCnt;
  int mainAddr = 0;

  public CodeGenerator(Program prog, int globalVariableCnt, File outFile) {
    this.program = prog;
    this.outFile = outFile;
    this.globalVariableCnt = globalVariableCnt;
  }

  public void execute() throws FileNotFoundException {
    this.program.traverseBottomUp(this);
    if (!errorsFound) {
      Code.dataSize = this.globalVariableCnt;
      Code.mainPc = this.mainAddr;
      Code.write(new FileOutputStream(this.outFile));
    }
  }

  public boolean getErrorsFound() {
    return this.errorsFound;
  }

  ///////////////////////////////////////
  /////////////// Logging ///////////////
  ///////////////////////////////////////

  private void error(SyntaxNode info, String msg, String... args) {
    String formatted = String.format(msg, (Object[]) args);
    String s = formatted;

    if (info != null)
      s = String.format("Line %d: %s", info.getLine(), formatted);

    log.error(s);
    this.errorsFound = true;
  }

  private void info(SyntaxNode info, String msg, String... args) {
    String formatted = String.format(msg, (Object[]) args);
    String s = formatted;

    if (info != null)
      s = String.format("Line %d: %s", info.getLine(), formatted);

    log.info(s);
  }

  private void info(String msg, String... args) {
    info(null, msg, args);
  }

  private void debug(SyntaxNode info, String msg, String... args) {
    String formatted = String.format(msg, (Object[]) args);
    String s = formatted;

    if (info != null)
      s = String.format("Line %d: %s", info.getLine(), formatted);

    log.debug(s);
  }

  //////////////////////////////////////////
  /////////////// Generation ///////////////
  //////////////////////////////////////////

  private void buildOrd() {
    Obj ord = Tab.find("ord");
    ord.setAdr(Code.pc);

    Code.put(Code.return_);
  }

  private void buildChr() {
    Obj chr = Tab.find("chr");
    chr.setAdr(Code.pc);

    // We're doing ((num << 8) >> 8) to clear the top bits
    Code.loadConst(24);
    Code.put(Code.shl);
    Code.loadConst(24);
    Code.put(Code.shr);
    Code.put(Code.return_);
  }

  private void buildLen() {
    Obj len = Tab.find("len");
    len.setAdr(Code.pc);

    Code.put(Code.arraylength);
    Code.put(Code.return_);
  }

  @Override
  public void visit(ProgramName progName) {
    buildChr();
    buildLen();
    buildOrd();
  }

  @Override
  public void visit(MainName mainName) {
    Obj main = mainName.obj;

    this.mainAddr = Code.pc;
    main.setAdr(this.mainAddr);

    Code.put(Code.enter);
    Code.put(main.getLevel());
    Code.put(main.getLocalSymbols().size());
  }

  @Override
  public void visit(MainMethod mainMehtod) {
    Code.put(Code.exit);
    Code.put(Code.return_);
  }

  @Override
  public void visit(PrintExprStmt print) {
    Struct type = print.getExpr().struct;

    Code.loadConst(0);
    if (type.equals(Tab.charType))
      Code.put(Code.bprint);
    else
      Code.put(Code.print);
  }

  @Override
  public void visit(PrintExprConstStmt print) {
    Struct type = print.getExpr().struct;

    Code.loadConst(print.getNumConst());
    if (type.equals(Tab.charType))
      Code.put(Code.bprint);
    else
      Code.put(Code.print);
  }

  @Override
  public void visit(ReadStmt read) {
    Obj obj = read.getDesignator().obj;
    Struct type = obj.getType();

    if (type.equals(Tab.charType))
      Code.put(Code.bread);
    else
      Code.put(Code.read);

    Code.store(obj);
  }

  // Designator statement
  @Override
  public void visit(Assignment ass) {
    Code.store(ass.getDesignator().obj);
  }

  @Override
  public void visit(Condition cond) {
    Code.loadConst(1);
    Code.putFalseJump(Code.eq, 0); // Will be patched in ThenExpr
    conditionalJumps.add(Code.pc - 2);
  }

  @Override
  public void visit(ThenExpr expr) {
    Code.putJump(0); // Will be patched in ElseExpr

    // Now PC is at the start of the ElseExpr block
    int falseJumpAddr = conditionalJumps.pop();

    conditionalJumps.add(Code.pc - 2);

    Code.fixup(falseJumpAddr); // Patch the ternary operator to find ElseExpr block
  }

  @Override
  public void visit(ElseExpr expr) {
    // Now PC is at the end of ElseExpr block
    int elseExprSkipper = conditionalJumps.pop();
    Code.fixup(elseExprSkipper); // Patch the final jump in ThenExpr to skip ElseExpr block
  }

  @Override
  public void visit(Incdec incdec) {
    Obj obj = incdec.getDesignator().obj;
    String op = incdec.getOp();

    int opCode;
    switch (op) {
    case "++": {
      opCode = Code.add;
      break;
    }
    case "--": {
      opCode = Code.sub;
      break;
    }
    default: {
      error(incdec, "Unknown incdec operation '%s'", op);
      opCode = -1;
    }
    }

    if (obj.getKind() == Obj.Elem) {
      Code.put(Code.dup2);
    }

    Code.load(obj);
    Code.loadConst(1);
    Code.put(opCode);
    Code.store(obj);
  }

  // BoolCondition
  @Override
  public void visit(ComparisonCondition cond) {
    String op = cond.getOp();
    int opCode;

    switch (op) {
    case "==": {
      opCode = Code.eq;
      break;
    }
    case "!=": {
      opCode = Code.ne;
      break;
    }
    case ">": {
      opCode = Code.gt;
      break;
    }
    case "<": {
      opCode = Code.lt;
      break;
    }
    case ">=": {
      opCode = Code.ge;
      break;
    }
    case "<=": {
      opCode = Code.le;
      break;
    }
    default: {
      opCode = -1;
      error(cond, "Unknown relation operator '%s'", op);
      break;
    }
    }

    Code.putFalseJump(opCode, Code.pc + 7);
    Code.loadConst(1);
    Code.putJump(Code.pc + 4);
    Code.loadConst(0);
  }

  // Addition
  @Override
  public void visit(Add add) {
    String op = add.getOp();
    switch (op) {
    case "+":
      Code.put(Code.add);
      break;
    case "-":
      Code.put(Code.sub);
      break;
    default:
      error(add, "Addition: Unrecognized operator '%s'", op);
      break;
    }
  }

  // Multiplication
  @Override
  public void visit(Multiply mul) {
    String op = mul.getOp();
    switch (op) {
    case "*":
      Code.put(Code.mul);
      break;
    case "/":
      Code.put(Code.div);
      break;
    case "%":
      Code.put(Code.rem);
      break;
    default:
      error(mul, "Multiplication: Unrecognized operator '%s'", op);
      break;
    }
  }

  int cnt = 0;

  private Obj newVar(Struct type) {
    return Tab.insert(Obj.Var, "var" + Integer.toString(cnt++), type);
  }


  // Factor
  @Override
  public void visit(Negation neg) {
    Code.put(Code.neg);
  }

  // Term
  @Override
  public void visit(TermConstInt num) {
    Code.loadConst(num.getVal());
  }

  @Override
  public void visit(TermConstChar ch) {
    Code.loadConst(ch.getVal());
  }

  @Override
  public void visit(TermConstBool bool) {
    Code.loadConst(bool.getVal());
  }

  @Override
  public void visit(TermToDesignator term) {
    Code.load(term.getDesignator().obj);
  }

  @Override
  public void visit(TermNewTypeArray term) {
    Struct type = term.struct;

    Code.put(Code.newarray);
    if (type.equals(Tab.charType))
      Code.put(0);
    else
      Code.put(1);
  }

  // Other
  @Override
  public void visit(LengthAccess access) {
    Code.load(access.obj);
    Code.put(Code.arraylength);
  }

  @Override
  public void visit(FunctionCall call) {
    Obj funcObj = Tab.find(call.getName());

    if (funcObj.getKind() != Obj.Meth) {
      error(call, "Calling a non-method '%s'", call.getName());
      return;
    }

    Code.put(Code.call);
    Code.put2(funcObj.getAdr() - Code.pc + 1);
  }

  // Designator
  @Override
  public void visit(ArrayName array) {
    Code.load(array.obj);
  }

}