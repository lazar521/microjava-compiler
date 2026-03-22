package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.ast.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	Logger log = Logger.getLogger(Compiler.class);
	private boolean errorsFound = false;
	private Struct boolType = new Struct(Struct.Bool);
	private int globalVariableCnt;
	private Program prog;

	private Obj currentProgam = null;
	private Obj mainMethod = null;

	private Struct currentType = null;

	private Struct constType = null;
	private int constValue = 0;

	private Struct currentEnumType = null;
	private int currentEnumVal = 0;
	private ArrayList<Integer> currentEnumUsedVals = new ArrayList<Integer>();

	private static final HashMap<Integer, String> intToType = new HashMap<>();

	static {
		intToType.put(Struct.None, "None");
		intToType.put(Struct.Int, "Int");
		intToType.put(Struct.Char, "Char");
		intToType.put(Struct.Array, "Array");
		intToType.put(Struct.Class, "Class");
		intToType.put(Struct.Bool, "Bool");
		intToType.put(Struct.Enum, "Enum");
		intToType.put(Struct.Interface, "Interface");
	}

	public SemanticAnalyzer(Program prog) {
		this.prog = prog;
		Tab.init();
		Obj boolObj = Tab.insert(Obj.Type, "bool", this.boolType);
		boolObj.setAdr(-1);
		boolObj.setLevel(-1);
	}

	public void execute() {
		this.prog.traverseBottomUp(this);
	}

	public int getGlobalVariableCnt() {
		return this.globalVariableCnt;
	}

	private String typeName(Struct type) {
		int kind = type.getKind();
		String name = intToType.get(kind);
		if (name == null) {
			error(null, "Unknown Struct kind: " + kind);
		}
		return name;
	}

	public boolean getErrorsFound() {
		return this.errorsFound;
	}

	private boolean convertable(Struct from, Struct to) {
		return (from.getKind() == Struct.Enum) && to.equals(Tab.intType);
	}

	private boolean equalOrConvertable(Struct from, Struct to) {
		return convertable(from, to) || from.equals(to);
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

	////////////////////////////////////////
	/////////////// Analysis ///////////////
	////////////////////////////////////////
	@Override
	public void visit(Program prog) {
		debug(prog, "Visiting Program");

		this.globalVariableCnt = Tab.currentScope().getnVars();
		Tab.chainLocalSymbols(this.currentProgam);
		Tab.closeScope();

		if (this.mainMethod == null) {
			error(null, "Mehtod 'main' is missing");
		}
	}

	@Override
	public void visit(ProgramName pn) {
		debug(pn, "Visiting ProgramName");
		this.currentProgam = Tab.insert(Obj.Prog, pn.getName(), Tab.noType);
		Tab.openScope();
	}

	@Override
	public void visit(ConstDeclSingle decl) {
		debug(decl, "Visiting ConstDeclSingle");
		Obj obj = Tab.find(decl.getName());

		if (obj != Tab.noObj) {
			error(decl, "Cannot declare constant. Name '%s' already taken", decl.getName());
			return;
		}

		if (!this.constType.assignableTo(currentType)) {
			error(decl, "Incompatible type assignment for '%s'", decl.getName());
			return;
		}

		Tab.insert(Obj.Con, decl.getName(), this.currentType).setAdr(this.constValue);
	}

	@Override
	public void visit(Type type) {
		debug(type, "Visiting Type");
		Obj obj = Tab.find(type.getTypeName());

		if (obj == Tab.noObj) {
			error(type, "Type '%s' is not declared", type.getTypeName());
			this.currentType = Tab.noType;
			return;
		}
		if (obj.getKind() != Obj.Type) {
			error(type, "Identifier '%' does not represent a data type", type.getTypeName());
			this.currentType = Tab.noType;
			return;
		}

		this.currentType = obj.getType();
	}

	@Override
	public void visit(NumValue val) {
		debug(val, "Visiting NumValue");
		this.constValue = val.getVal();
		this.constType = Tab.intType;
		val.struct = Tab.intType;
	}

	@Override
	public void visit(CharValue val) {
		debug(val, "Visiting CharValue");
		this.constValue = val.getVal();
		this.constType = Tab.charType;
		val.struct = Tab.charType;
	}

	@Override
	public void visit(BoolValue val) {
		debug(val, "Visiting BoolValue");
		this.constValue = val.getVal();
		this.constType = this.boolType;
		val.struct = this.boolType;
	}

	@Override
	public void visit(VarDeclSingleIdent decl) {
		debug(decl, "Visiting VarDeclSingleIdent");
		Obj obj;
		if (this.mainMethod != null)
			obj = Tab.currentScope().findSymbol(decl.getName());
		else
			obj = Tab.find(decl.getName());

		if (obj != null && obj != Tab.noObj) {
			error(decl, "Cannot declare variable. Name '%s' already taken", decl.getName());
			return;
		}

		Tab.insert(Obj.Var, decl.getName(), this.currentType);
	}

	@Override
	public void visit(VarDeclSingleArr decl) {
		debug(decl, "Visiting VarDeclSingleArr");
		Obj obj;
		if (this.mainMethod != null)
			obj = Tab.currentScope().findSymbol(decl.getName());
		else
			obj = Tab.find(decl.getName());

		if (obj != null && obj != Tab.noObj) {
			error(decl, "Cannot declare array. Name '%s' already taken", decl.getName());
			return;
		}

		Tab.insert(Obj.Var, decl.getName(), new Struct(Struct.Array, this.currentType));
	}

	@Override
	public void visit(EnumName name) {
		debug(name, "Visiting EnumName");
		if (this.mainMethod != null) {
			error(name, "Enum declarations must appear before main function");
			return;
		}

		Obj obj = Tab.currentScope().findSymbol(name.getName());
		if (obj != null) {
			error(name, "Cannot declare enum. Name '%s' already taken", name.getName());
			return;
		}

		this.currentEnumVal = 0;
		this.currentEnumUsedVals.clear();
		this.currentEnumType = new Struct(Struct.Enum);
		Tab.insert(Obj.Type, name.getName(), this.currentEnumType);
		Tab.openScope();
	}

	@Override
	public void visit(EnumDeclSingleIdent decl) {
		debug(decl, "Visiting EnumDeclSingleIdent");
		Obj obj = Tab.currentScope().findSymbol(decl.getName());
		if (obj != null) {
			error(decl, "Cannot declare enum field. Name '%s' already taken", decl.getName());
			return;
		}

		while (this.currentEnumUsedVals.contains(this.currentEnumVal) == true) {
			this.currentEnumVal++;
		}

		Obj field = Tab.insert(Obj.Con, decl.getName(), this.currentEnumType);
		field.setAdr(this.currentEnumVal);
		field.setLevel(2);
		this.currentEnumUsedVals.add(currentEnumVal);
	}

	@Override
	public void visit(EnumDeclSingleAssign decl) {
		debug(decl, "Visiting EnumDeclSingleAssing");
		Obj obj = Tab.currentScope().findSymbol(decl.getName());
		if (obj != null) {
			error(decl, "Cannot declare enum field. Name '%s' already taken", decl.getName());
			return;
		}

		int desiredVal = decl.getNumValue();
		if (this.currentEnumUsedVals.contains(desiredVal)) {
			error(decl, "Cannot assign value to enum field '%s'. Value already taken", decl.getName());
			return;
		}

		Obj field = Tab.insert(Obj.Con, decl.getName(), this.currentEnumType);
		field.setAdr(desiredVal);
		field.setLevel(2);

		this.currentEnumUsedVals.add(desiredVal);
		this.currentEnumVal = desiredVal + 1;
	}

	@Override
	public void visit(EnumDecl decl) {
		debug(decl, "Visiting EnumDecl");
		Tab.chainLocalSymbols(this.currentEnumType);
		Tab.closeScope();
	}

	@Override
	public void visit(MainName name) {
		debug(name, "Visiting MainName");
		this.mainMethod = Tab.insert(Obj.Meth, "main", Tab.noType);
		name.obj = this.mainMethod;
		Tab.openScope();
	}

	@Override
	public void visit(MainMethod main) {
		debug(main, "Visiting MainMethod");
		Tab.chainLocalSymbols(this.mainMethod);
		Tab.closeScope();
	}

	// Designator statement
	@Override
	public void visit(Assignment ass) {
		debug(ass, "Visiting Assignment");
		Obj leftObj = ass.getDesignator().obj;

		if (leftObj.getKind() == Obj.Con) {
			error(ass, "Cannot assign to a constant");
			return;
		}

		int kind = leftObj.getKind();
		if (kind != Obj.Var && kind != Obj.Elem && kind != Obj.Fld) {
			error(ass, "Cannot assign to a non-var");
			return;
		}

		Struct left = leftObj.getType();
		Struct right = ass.getExpr().struct;

		if (!left.compatibleWith(right) && !convertable(right, left)) {
			if (right.getKind() == Struct.Enum && left.getKind() == Struct.Enum)
				error(ass, "Incompatible assignment types. Assigning wrong enum types.", leftObj.getName());
			if (right.getKind() == Struct.Array && left.getKind() == Struct.Array)
				error(ass, "Incompatible array type assignment: %s and %s", typeName(left.getElemType()), typeName(right.getElemType()));
			else
				error(ass, "Incompatible assignment types %s and %s", typeName(left), typeName(right));
			return;
		}

		if (left.equals(Tab.nullType)) {
			error(ass, "Cannot assign to null type");
			return;
		}

	}

	@Override
	public void visit(Incdec incdec) {
		debug(incdec, "Visiting Incdec");
		Obj obj = incdec.getDesignator().obj;

		if (obj.getKind() == Obj.Con) {
			error(incdec, "Cannot increment/decrement a constant");
			return;
		}

		Struct type = obj.getType();
		if (!type.equals(Tab.intType)) {
			error(incdec, "Cannot increment/decrement non-int type %s", typeName(type));
		}

	}

	// Read statement
	@Override
	public void visit(ReadStmt read) {
		debug(read, "Visiting ReadStmt");
		Obj obj = read.getDesignator().obj;
		Struct type = obj.getType();

		if (type.getKind() == Struct.Enum) {
			error(read, "Cannot put data into enum type %s", typeName(type));
			return;
		}

		if (type.getKind() == Struct.Array) {
			error(read, "Cannot put data into array variable directly", typeName(type));
			return;
		}

		if (!type.equals(Tab.charType) && !type.equals(Tab.intType) && !type.equals(this.boolType)) {
			error(read, "Cannot put data into type %s", typeName(type));
			return;
		}
	}

	// Print statement
	@Override
	public void visit(PrintExprStmt print) {
		debug(print, "Visiting PrintExprStmt");
		Struct type = print.getExpr().struct;

		if (!type.equals(Tab.charType) && !type.equals(Tab.intType) && !type.equals(this.boolType)) {
			error(print, "Cannot print type %s", typeName(type));
			return;
		}
	}

	@Override
	public void visit(PrintExprConstStmt print) {
		debug(print, "Visiting PrintExprConstStmt");
		Struct type = print.getExpr().struct;

		if (!type.equals(Tab.charType) && !type.equals(Tab.intType) && !type.equals(this.boolType)) {
			error(print, "Cannot print type %s", typeName(type));
			return;
		}
	}

	// Expr
	@Override
	public void visit(Ternary ternary) {
		debug(ternary, "Visiting Ternary");
		Struct boolType = this.boolType;
		Struct type = ternary.getCondition().struct;
		if (!type.equals(boolType)) {
			error(ternary, "Condition must be of type bool instead of %s", typeName(type));
			ternary.struct = Tab.noType;
			return;
		}

		Struct thenExpr = ternary.getThenExpr().struct;
		Struct elseExpr = ternary.getElseExpr().struct;
		if (!equalOrConvertable(thenExpr, elseExpr) && !equalOrConvertable(elseExpr, thenExpr)) {
			error(ternary, "Expressions in ternary operator aren't of the same type: %s and %s", typeName(thenExpr), typeName(elseExpr));
			ternary.struct = Tab.noType;
			return;
		}

		ternary.struct = thenExpr;
	}

	@Override
	public void visit(Condition cond) {
		debug(cond, "Visitng Condition");
		cond.struct = cond.getBoolCondition().struct;
	}

	@Override
	public void visit(ThenExpr expr) {
		debug(expr, "Visiting ThenExpr");
		expr.struct = expr.getExpr().struct;
	}

	@Override
	public void visit(ElseExpr expr) {
		debug(expr, "Visiting ElseExpr");
		expr.struct = expr.getExpr().struct;
	}

	@Override
	public void visit(ExprToAdd expr) {
		debug(expr, "Visiting ExprToAdd");
		expr.struct = expr.getBoolCondition().struct;
	}

	// BoolCondition
	@Override
	public void visit(ComparisonCondition cond) {
		debug(cond, "Visiting ComparisonCondition");
		Struct left = cond.getBoolCondition().struct;
		Struct right = cond.getAddition().struct;

		if (!left.compatibleWith(right)) {
			error(cond, "Operands of the '%s' operation are not compatible: %s and %s", cond.getOp(), typeName(left), typeName(right));
			cond.struct = Tab.noType;
			return;
		}

		boolean arrayComparison = (left.isRefType() || right.isRefType());
		boolean isEquals = cond.getOp().equals("==");
		boolean isNotEquals = cond.getOp().equals("!=");

		if (arrayComparison && !isEquals && !isNotEquals) {
			error(cond, "Cannot compare array and non-array: %s %s", typeName(left), typeName(right));
			cond.struct = Tab.noType;
			return;
		}

		cond.struct = this.boolType;
	}

	@Override
	public void visit(SimpleCondition cond) {
		debug(cond, "Visiting SimpleCondition");
		cond.struct = cond.getAddition().struct;
	}

	// Addition
	@Override
	public void visit(Add add) {
		debug(add, "Visiting Add");
		Struct left = add.getAddition().struct;
		Struct right = add.getMultiplication().struct;

		if (!equalOrConvertable(left, Tab.intType) || !equalOrConvertable(right, Tab.intType)) {
			error(add, "Cannot perform addition/subtraction between non-int types: %s and %s", typeName(left), typeName(right));
			add.struct = Tab.noType;
			return;
		}

		add.struct = Tab.intType;
	}

	@Override
	public void visit(AddToMultiplication atm) {
		debug(atm, "Visiting AddToMultiplication");
		atm.struct = atm.getMultiplication().struct;
	}

	// Multiplication
	@Override
	public void visit(Multiply mul) {
		debug(mul, "Visiting Multiply");
		Struct left = mul.getMultiplication().struct;
		Struct right = mul.getFactor().struct;

		if (!equalOrConvertable(left, Tab.intType) || !equalOrConvertable(right, Tab.intType)) {
			error(mul, "Cannot perform multiplication/division/mod between non-int types: %s and %s", typeName(left), typeName(right));
			mul.struct = Tab.noType;
			return;
		}

		mul.struct = Tab.intType;
	}

	@Override
	public void visit(MultiplyToFactor mtf) {
		debug(mtf, "Visiting MultiplyToFactor");
		mtf.struct = mtf.getFactor().struct;
	}

	// Factor
	@Override
	public void visit(FactorToTerm factor) {
		debug(factor, "Visiting FactorToTerm");
		factor.struct = factor.getTerm().struct;
	}

	@Override
	public void visit(Negation negation) {
		debug(negation, "Visiting Negation");
		String op = negation.getOp();
		if (!op.equals("-")) {
			error(negation, "The '%s' is not allowed as a unary operator", op);
			negation.struct = Tab.noType;
			return;
		}

		Struct type = negation.getTerm().struct;
		if (!equalOrConvertable(type, Tab.intType)) {
			error(negation, "Cannot perform negation on non-int type %s", typeName(type));
			negation.struct = Tab.noType;
			return;
		}

		negation.struct = Tab.intType;
	}

	// Term

	@Override
	public void visit(TermInParnetheses term) {
		debug(term, "Visiting TermInParentheses");
		term.struct = term.getExpr().struct;
	}

	@Override
	public void visit(TermToDesignator term) {
		debug(term, "Visiting TermToDesignator");
		term.struct = term.getDesignator().obj.getType();
	}

	@Override
	public void visit(TermFunctionCall term) {
		debug(term, "Visitnf TermFunctionCall");
		term.struct = term.getFunctionCall().obj.getType();
	}

	@Override
	public void visit(TermNewTypeArray term) {
		debug(term, "Visiting TermNewTypeArray");
		Struct indexType = term.getExpr().struct;

		if (!equalOrConvertable(indexType, Tab.intType)) {
			error(term, "Indexing error. Size of a new array must be an integer, not %s", typeName(indexType));
			term.struct = Tab.noType;
			return;
		}

		term.struct = new Struct(Struct.Array, this.currentType);
	}

	@Override
	public void visit(TermLengthAccess term) {
		debug(term, "Visiting TermLengthAccess");
		term.struct = Tab.intType;
	}

	@Override
	public void visit(TermConstInt term) {
		debug(term, "Visiting TermConstInt");
		term.struct = Tab.intType;
	}

	@Override
	public void visit(TermConstChar term) {
		debug(term, "Visiting TermConstChar");
		term.struct = Tab.charType;
	}

	@Override
	public void visit(TermConstBool term) {
		debug(term, "Visiting TermConstBool");
		term.struct = this.boolType;
	}

	// Additional
	@Override
	public void visit(FunctionCall funcCall) {
		debug(funcCall, "Visiting FunctionCall");
		Obj obj = Tab.find(funcCall.getName());
		if (obj == Tab.noObj) {
			error(funcCall, "Illegal access. Method '%s' not declared", funcCall.getName());
			funcCall.obj = Tab.noObj;
			return;
		}

		if (obj.getKind() != Obj.Meth) {
			error(funcCall, "Illegal access. '%s' is not a method", funcCall.getName());
			funcCall.obj = Tab.noObj;
			return;

		}

		ArrayList<Obj> params = new ArrayList<>(obj.getLocalSymbols());
		ArrayList<Struct> argTypes = new ArrayList<>();
		argTypes.add(funcCall.getExpr().struct);

		if (params.size() != argTypes.size()) {
			error(funcCall, "Wrong number of arguments when calling method '%s'", funcCall.getName());
			funcCall.obj = Tab.noObj;
			return;
		}

		boolean err = false;
		for (int i = 0; i < params.size(); i++) {
			Struct paramType = params.get(i).getType();
			Struct argType = argTypes.get(i);

			boolean acceptAnyArrayType = paramType.getKind() == Struct.Array && paramType.getElemType().equals(Tab.noType);
			boolean acceptArray = acceptAnyArrayType && argType.getKind() == Struct.Array;
			if (!equalOrConvertable(argType, paramType) && !acceptArray) {
				err = true;
				error(funcCall, "Wrong argument type when calling '%s' function. Argument number %s should be of type %s instead of %s", funcCall.getName(), Integer.toString(i + 1), typeName(paramType),
						typeName(argType));
			}
		}

		if (err) {
			funcCall.obj = Tab.noObj;
			return;
		}

		funcCall.obj = obj;
	}

	@Override
	public void visit(LengthAccess desig) {
		debug(desig, "Visiting LengthAccess: %s.length", desig.getName());
		Obj obj = Tab.find(desig.getName());

		if (obj == Tab.noObj) {
			error(desig, "Illegal access. Array '%s' not declared", desig.getName());
			desig.obj = Tab.noObj;
			return;
		}

		if (obj.getKind() != Obj.Var) {
			error(desig, "Illegal access. '%s' not a variable", desig.getName());
			desig.obj = Tab.noObj;
			return;
		}

		if (obj.getType().getKind() != Struct.Array) {
			error(desig, "Illegal access. '%s' not an array", desig.getName());
			desig.obj = Tab.noObj;
			return;
		}

		desig.obj = obj;
	}

	// Designator

	@Override
	public void visit(Ident desig) {
		debug(desig, "Visiting Ident: %s", desig.getName());
		Obj obj = Tab.find(desig.getName());

		if (obj == Tab.noObj) {
			error(desig, "Illegal access. Variable '%s' not declared", desig.getName());
			desig.obj = Tab.noObj;
			return;
		}

		if (obj.getKind() != Obj.Var && obj.getKind() != Obj.Con) {
			error(desig, "Usage error. Referncing '%s' as a variable/constant", desig.getName());
			desig.obj = Tab.noObj;
			return;
		}

		desig.obj = obj;
	}

	@Override
	public void visit(ArrayIndexing indexing) {
		debug(indexing, "Visiting ArrayIndexing");
		Struct type = indexing.getExpr().struct;
		if (!type.equals(Tab.intType) && type.getKind() != Struct.Enum) {
			error(indexing, "indexing error. indexing '%s' with a non-int type %s", indexing.getArrayName().obj.getName(), typeName(type));
			indexing.obj = Tab.noObj;
			return;
		}

		indexing.obj = new Obj(Obj.Elem, String.format("%s[EL]", indexing.getArrayName()), indexing.getArrayName().obj.getType().getElemType());
	}

	@Override
	public void visit(ArrayName array) {
		debug(array, "Visiting ArrayName");
		Obj obj = Tab.find(array.getName());

		if (obj == Tab.noObj) {
			error(array, "Illegal access. Array '%s' not declared", array.getName());
			array.obj = Tab.noObj;
			return;
		}

		if (obj == Tab.noObj) {
			error(array, "Illegal access. Array '%s' not declared", array.getName());
			array.obj = Tab.noObj;
			return;
		}

		if (obj.getType().getKind() != Struct.Array) {
			error(array, "Illegal access. '%s' is not an array", array.getName());
			array.obj = Tab.noObj;
			return;
		}

		array.obj = obj;
	}

	@Override
	public void visit(FieldAccess access) {
		debug(access, "Visiting FieldAccess");
		Obj obj = Tab.find(access.getName());

		if (obj == Tab.noObj) {
			error(access, "Illegal access. Enum '%s' not declared", access.getName());
			access.obj = Tab.noObj;
			return;
		}

		Struct type = obj.getType();
		if (type.getKind() != Struct.Enum) {
			error(access, "Illegal access. Type %s does not have field %s", typeName(type), access.getField());
			access.obj = Tab.noObj;
			return;
		}

		for (Obj field : obj.getType().getMembers()) {
			if (field.getName().equals(access.getField())) {
				access.obj = field;
				return;
			}
		}

		error(access, "Illegal access. Enum '%s' does not contain field '%s'", access.getName(), access.getField());
		access.obj = Tab.noObj;

	}
}