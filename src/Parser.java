

public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _number = 2;
	public static final int _float = 3;
	public static final int _char = 4;
	public static final int _string = 5;
	public static final int maxT = 64;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void K() {
		if (la.kind == 6) {
			PackageDecl();
		}
		while (la.kind == 8) {
			ImportDecl();
		}
		while (StartOf(1)) {
			TopLevelDecl();
		}
	}

	void PackageDecl() {
		Expect(6);
		QualifiedName();
		Expect(7);
	}

	void ImportDecl() {
		Expect(8);
		QualifiedName();
		Expect(7);
	}

	void TopLevelDecl() {
		if (la.kind == 20) {
			ClassDecl();
		} else if (la.kind == 26) {
			RecordDecl();
		} else if (la.kind == 27) {
			InterfaceDecl();
		} else if (la.kind == 19) {
			FunDecl();
		} else if (la.kind == 15 || la.kind == 16) {
			VarDecl();
			Expect(7);
		} else SynErr(65);
	}

	void QualifiedName() {
		Expect(1);
		while (la.kind == 9) {
			Get();
			Expect(1);
		}
	}

	void ClassDecl() {
		Expect(20);
		Expect(1);
		if (la.kind == 12) {
			GenericArgs();
		}
		if (la.kind == 21) {
			Get();
			Type();
		}
		if (la.kind == 22) {
			Get();
			Type();
			while (la.kind == 13) {
				Get();
				Type();
			}
		}
		ClassBody();
	}

	void RecordDecl() {
		Expect(26);
		Expect(1);
		Expect(10);
		FieldList();
		Expect(11);
		RecordBody();
	}

	void InterfaceDecl() {
		Expect(27);
		Expect(1);
		if (la.kind == 12) {
			GenericArgs();
		}
		if (la.kind == 21) {
			Get();
			Type();
			while (la.kind == 13) {
				Get();
				Type();
			}
		}
		Expect(23);
		while (la.kind == 19) {
			InterfaceMember();
		}
		Expect(24);
	}

	void FunDecl() {
		Expect(19);
		Expect(1);
		Expect(10);
		if (la.kind == 1) {
			ParamList();
		}
		Expect(11);
		if (la.kind == 17) {
			Get();
			Type();
		}
		Block();
	}

	void VarDecl() {
		if (la.kind == 15) {
			Get();
		} else if (la.kind == 16) {
			Get();
		} else SynErr(66);
		Expect(1);
		if (la.kind == 17) {
			Get();
			Type();
		}
		if (la.kind == 18) {
			Get();
			Expression();
		}
	}

	void Type() {
		BasicType();
		if (la.kind == 12) {
			GenericArgs();
		}
	}

	void BasicType() {
		if (la.kind == 1) {
			Get();
		} else if (la.kind == 10) {
			Get();
			Type();
			Expect(11);
		} else SynErr(67);
	}

	void GenericArgs() {
		Expect(12);
		Type();
		while (la.kind == 13) {
			Get();
			Type();
		}
		Expect(14);
	}

	void Expression() {
		Assignment();
	}

	void ParamList() {
		Param();
		while (la.kind == 13) {
			Get();
			Param();
		}
	}

	void Block() {
		Expect(23);
		while (StartOf(2)) {
			Statement();
		}
		Expect(24);
	}

	void Param() {
		Expect(1);
		if (la.kind == 17) {
			Get();
			Type();
		}
	}

	void ClassBody() {
		Expect(23);
		while (StartOf(3)) {
			ClassMember();
		}
		Expect(24);
	}

	void ClassMember() {
		if (la.kind == 15 || la.kind == 16) {
			VarDecl();
			Expect(7);
		} else if (la.kind == 19) {
			FunDecl();
		} else if (la.kind == 25) {
			OperatorDecl();
		} else if (StartOf(2)) {
			Statement();
		} else SynErr(68);
	}

	void OperatorDecl() {
		Expect(25);
		FunDecl();
	}

	void Statement() {
		switch (la.kind) {
		case 15: case 16: {
			VarDecl();
			Expect(7);
			break;
		}
		case 1: case 2: case 3: case 4: case 5: case 10: case 38: case 39: case 40: case 56: case 57: case 61: case 62: case 63: {
			ExprStmt();
			Expect(7);
			break;
		}
		case 28: {
			ReturnStmt();
			Expect(7);
			break;
		}
		case 29: {
			IfStmt();
			break;
		}
		case 31: {
			WhileStmt();
			break;
		}
		case 32: {
			ForStmt();
			break;
		}
		case 33: {
			WhenStmt();
			break;
		}
		case 23: {
			Block();
			break;
		}
		case 7: {
			Get();
			break;
		}
		default: SynErr(69); break;
		}
	}

	void FieldList() {
		Field();
		while (la.kind == 13) {
			Get();
			Field();
		}
	}

	void RecordBody() {
		if (la.kind == 7) {
			Get();
		} else if (la.kind == 23) {
			Get();
			while (StartOf(3)) {
				ClassMember();
			}
			Expect(24);
		} else SynErr(70);
	}

	void Field() {
		Expect(1);
		if (la.kind == 17) {
			Get();
			Type();
		}
	}

	void InterfaceMember() {
		Expect(19);
		Expect(1);
		Expect(10);
		if (la.kind == 1) {
			ParamList();
		}
		Expect(11);
		if (la.kind == 17) {
			Get();
			Type();
		}
		Expect(7);
	}

	void ExprStmt() {
		Expression();
	}

	void ReturnStmt() {
		Expect(28);
		if (StartOf(4)) {
			Expression();
		}
	}

	void IfStmt() {
		Expect(29);
		Expect(10);
		Expression();
		Expect(11);
		Statement();
		if (la.kind == 30) {
			Get();
			Statement();
		}
	}

	void WhileStmt() {
		Expect(31);
		Expect(10);
		Expression();
		Expect(11);
		Statement();
	}

	void ForStmt() {
		Expect(32);
		Expect(10);
		if (StartOf(5)) {
			ForInit();
		}
		Expect(7);
		if (StartOf(4)) {
			Expression();
		}
		Expect(7);
		if (StartOf(4)) {
			Expression();
		}
		Expect(11);
		Statement();
	}

	void WhenStmt() {
		Expect(33);
		Expect(10);
		Expression();
		Expect(11);
		Expect(23);
		while (la.kind == 34) {
			Case();
		}
		if (la.kind == 30) {
			Else();
		}
		Expect(24);
	}

	void ForInit() {
		if (la.kind == 15 || la.kind == 16) {
			VarDecl();
		} else if (StartOf(4)) {
			Expression();
		} else SynErr(71);
	}

	void Case() {
		Expect(34);
		Pattern();
		while (la.kind == 35) {
			Get();
			Pattern();
		}
		Expect(36);
		Statement();
	}

	void Else() {
		Expect(30);
		Expect(36);
		Statement();
	}

	void Pattern() {
		if (StartOf(6)) {
			Literal();
		} else if (la.kind == 1) {
			Get();
			Expect(10);
			if (StartOf(7)) {
				PatternList();
			}
			Expect(11);
		} else if (la.kind == 1) {
			Get();
		} else if (la.kind == 37) {
			Get();
		} else SynErr(72);
	}

	void Literal() {
		switch (la.kind) {
		case 2: {
			Get();
			break;
		}
		case 3: {
			Get();
			break;
		}
		case 5: {
			Get();
			break;
		}
		case 4: {
			Get();
			break;
		}
		case 38: {
			Get();
			break;
		}
		case 39: {
			Get();
			break;
		}
		case 40: {
			Get();
			break;
		}
		default: SynErr(73); break;
		}
	}

	void PatternList() {
		Pattern();
		while (la.kind == 13) {
			Get();
			Pattern();
		}
	}

	void Assignment() {
		LogicOr();
		if (StartOf(8)) {
			AssignmentOp();
			Assignment();
		}
	}

	void LogicOr() {
		LogicAnd();
		while (la.kind == 46) {
			Get();
			LogicAnd();
		}
	}

	void AssignmentOp() {
		switch (la.kind) {
		case 18: {
			Get();
			break;
		}
		case 41: {
			Get();
			break;
		}
		case 42: {
			Get();
			break;
		}
		case 43: {
			Get();
			break;
		}
		case 44: {
			Get();
			break;
		}
		case 45: {
			Get();
			break;
		}
		default: SynErr(74); break;
		}
	}

	void LogicAnd() {
		BitOr();
		while (la.kind == 47) {
			Get();
			BitOr();
		}
	}

	void BitOr() {
		BitXor();
		while (la.kind == 35) {
			Get();
			BitXor();
		}
	}

	void BitXor() {
		BitAnd();
		while (la.kind == 48) {
			Get();
			BitAnd();
		}
	}

	void BitAnd() {
		Equality();
		while (la.kind == 49) {
			Get();
			Equality();
		}
	}

	void Equality() {
		Relational();
		while (la.kind == 50 || la.kind == 51) {
			if (la.kind == 50) {
				Get();
			} else {
				Get();
			}
			Relational();
		}
	}

	void Relational() {
		Shift();
		while (StartOf(9)) {
			if (la.kind == 12) {
				Get();
			} else if (la.kind == 52) {
				Get();
			} else if (la.kind == 14) {
				Get();
			} else {
				Get();
			}
			Shift();
		}
	}

	void Shift() {
		Additive();
		while (la.kind == 54 || la.kind == 55) {
			if (la.kind == 54) {
				Get();
			} else {
				Get();
			}
			Additive();
		}
	}

	void Additive() {
		Multiplicative();
		while (la.kind == 56 || la.kind == 57) {
			if (la.kind == 56) {
				Get();
			} else {
				Get();
			}
			Multiplicative();
		}
	}

	void Multiplicative() {
		Unary();
		while (la.kind == 58 || la.kind == 59 || la.kind == 60) {
			if (la.kind == 58) {
				Get();
			} else if (la.kind == 59) {
				Get();
			} else {
				Get();
			}
			Unary();
		}
	}

	void Unary() {
		if (StartOf(10)) {
			if (la.kind == 61) {
				Get();
			} else if (la.kind == 62) {
				Get();
			} else if (la.kind == 56) {
				Get();
			} else if (la.kind == 57) {
				Get();
			} else {
				Get();
			}
			Unary();
		} else if (StartOf(11)) {
			Postfix();
		} else SynErr(75);
	}

	void Postfix() {
		Primary();
		while (StartOf(12)) {
			PostOp();
		}
	}

	void Primary() {
		if (StartOf(6)) {
			Literal();
		} else if (la.kind == 1) {
			Get();
		} else if (la.kind == 1) {
			Get();
			Expect(10);
			if (StartOf(4)) {
				ArgList();
			}
			Expect(11);
		} else if (la.kind == 10) {
			Get();
			Expression();
			Expect(11);
		} else SynErr(76);
	}

	void PostOp() {
		if (la.kind == 10) {
			Get();
			if (StartOf(4)) {
				ArgList();
			}
			Expect(11);
		} else if (la.kind == 9) {
			Get();
			Expect(1);
		} else if (la.kind == 61) {
			Get();
		} else if (la.kind == 62) {
			Get();
		} else SynErr(77);
	}

	void ArgList() {
		Expression();
		while (la.kind == 13) {
			Get();
			Expression();
		}
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		K();
		Expect(0);

		scanner.buffer.Close();
	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_T, _T,_x,_x,_T, _T,_x,_x,_x, _x,_x,_T,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x},
		{_x,_T,_T,_T, _T,_T,_x,_T, _x,_x,_T,_x, _x,_x,_x,_T, _T,_x,_x,_x, _x,_x,_x,_T, _x,_x,_x,_x, _T,_T,_x,_T, _T,_T,_x,_x, _x,_x,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_T,_T,_T, _x,_x},
		{_x,_T,_T,_T, _T,_T,_x,_T, _x,_x,_T,_x, _x,_x,_x,_T, _T,_x,_x,_T, _x,_x,_x,_T, _x,_T,_x,_x, _T,_T,_x,_T, _T,_T,_x,_x, _x,_x,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_T,_T,_T, _x,_x},
		{_x,_T,_T,_T, _T,_T,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_T,_T,_T, _x,_x},
		{_x,_T,_T,_T, _T,_T,_x,_x, _x,_x,_T,_x, _x,_x,_x,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_T,_T,_T, _x,_x},
		{_x,_x,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x},
		{_x,_T,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_T,_T,_T, _x,_x},
		{_x,_T,_T,_T, _T,_T,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_T,_x, _x,_x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "number expected"; break;
			case 3: s = "float expected"; break;
			case 4: s = "char expected"; break;
			case 5: s = "string expected"; break;
			case 6: s = "\"package\" expected"; break;
			case 7: s = "\";\" expected"; break;
			case 8: s = "\"import\" expected"; break;
			case 9: s = "\".\" expected"; break;
			case 10: s = "\"(\" expected"; break;
			case 11: s = "\")\" expected"; break;
			case 12: s = "\"<\" expected"; break;
			case 13: s = "\",\" expected"; break;
			case 14: s = "\">\" expected"; break;
			case 15: s = "\"let\" expected"; break;
			case 16: s = "\"var\" expected"; break;
			case 17: s = "\":\" expected"; break;
			case 18: s = "\"=\" expected"; break;
			case 19: s = "\"fun\" expected"; break;
			case 20: s = "\"class\" expected"; break;
			case 21: s = "\"extends\" expected"; break;
			case 22: s = "\"implements\" expected"; break;
			case 23: s = "\"{\" expected"; break;
			case 24: s = "\"}\" expected"; break;
			case 25: s = "\"operator\" expected"; break;
			case 26: s = "\"record\" expected"; break;
			case 27: s = "\"interface\" expected"; break;
			case 28: s = "\"return\" expected"; break;
			case 29: s = "\"if\" expected"; break;
			case 30: s = "\"else\" expected"; break;
			case 31: s = "\"while\" expected"; break;
			case 32: s = "\"for\" expected"; break;
			case 33: s = "\"when\" expected"; break;
			case 34: s = "\"is\" expected"; break;
			case 35: s = "\"|\" expected"; break;
			case 36: s = "\"=>\" expected"; break;
			case 37: s = "\"_\" expected"; break;
			case 38: s = "\"true\" expected"; break;
			case 39: s = "\"false\" expected"; break;
			case 40: s = "\"null\" expected"; break;
			case 41: s = "\"+=\" expected"; break;
			case 42: s = "\"-=\" expected"; break;
			case 43: s = "\"*=\" expected"; break;
			case 44: s = "\"/=\" expected"; break;
			case 45: s = "\"%=\" expected"; break;
			case 46: s = "\"||\" expected"; break;
			case 47: s = "\"&&\" expected"; break;
			case 48: s = "\"^\" expected"; break;
			case 49: s = "\"&\" expected"; break;
			case 50: s = "\"==\" expected"; break;
			case 51: s = "\"!=\" expected"; break;
			case 52: s = "\"<=\" expected"; break;
			case 53: s = "\">=\" expected"; break;
			case 54: s = "\"<<\" expected"; break;
			case 55: s = "\">>\" expected"; break;
			case 56: s = "\"+\" expected"; break;
			case 57: s = "\"-\" expected"; break;
			case 58: s = "\"*\" expected"; break;
			case 59: s = "\"/\" expected"; break;
			case 60: s = "\"%\" expected"; break;
			case 61: s = "\"++\" expected"; break;
			case 62: s = "\"--\" expected"; break;
			case 63: s = "\"!\" expected"; break;
			case 64: s = "??? expected"; break;
			case 65: s = "invalid TopLevelDecl"; break;
			case 66: s = "invalid VarDecl"; break;
			case 67: s = "invalid BasicType"; break;
			case 68: s = "invalid ClassMember"; break;
			case 69: s = "invalid Statement"; break;
			case 70: s = "invalid RecordBody"; break;
			case 71: s = "invalid ForInit"; break;
			case 72: s = "invalid Pattern"; break;
			case 73: s = "invalid Literal"; break;
			case 74: s = "invalid AssignmentOp"; break;
			case 75: s = "invalid Unary"; break;
			case 76: s = "invalid Primary"; break;
			case 77: s = "invalid PostOp"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
