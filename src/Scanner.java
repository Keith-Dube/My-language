
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;


//-----------------------------------------------------------------------------------
// Scanner
//-----------------------------------------------------------------------------------
public class Scanner {
	static final char EOL = '\n';
	static final int  eofSym = 0;
	static final int maxT = 64;
	static final int noSym = 64;
	char valCh;       // current input character (for token.val)

	public Buffer buffer; // scanner buffer

	Token t;           // current token
	int ch;            // current input character
	int pos;           // byte position of current character
	int charPos;       // position by unicode characters starting with 0
	int col;           // column number of current character
	int line;          // line number of current character
	int oldEols;       // EOLs that appeared in a comment;
	static final StartStates start; // maps initial token character to start state
	static final Map literals;      // maps literal strings to literal kinds

	Token tokens;      // list of tokens already peeked (first token is a dummy)
	Token pt;          // current peek token
	
	char[] tval = new char[16]; // token text used in NextToken(), dynamically enlarged
	int tlen;          // length of current token


	static {
		start = new StartStates();
		literals = new HashMap();
		for (int i = 97; i <= 122; ++i) start.set(i, 1);
		for (int i = 48; i <= 57; ++i) start.set(i, 8);
		start.set(39, 4); 
		start.set(34, 6); 
		start.set(59, 9); 
		start.set(46, 10); 
		start.set(40, 11); 
		start.set(41, 12); 
		start.set(60, 35); 
		start.set(44, 13); 
		start.set(62, 36); 
		start.set(58, 14); 
		start.set(61, 37); 
		start.set(123, 15); 
		start.set(125, 16); 
		start.set(124, 38); 
		start.set(95, 18); 
		start.set(43, 39); 
		start.set(45, 40); 
		start.set(42, 41); 
		start.set(47, 42); 
		start.set(37, 43); 
		start.set(38, 44); 
		start.set(94, 26); 
		start.set(33, 45); 
		start.set(Buffer.EOF, -1);
		literals.put("package", 6);
		literals.put("import", 8);
		literals.put("let", 15);
		literals.put("var", 16);
		literals.put("fun", 19);
		literals.put("class", 20);
		literals.put("extends", 21);
		literals.put("implements", 22);
		literals.put("operator", 25);
		literals.put("record", 26);
		literals.put("interface", 27);
		literals.put("return", 28);
		literals.put("if", 29);
		literals.put("else", 30);
		literals.put("while", 31);
		literals.put("for", 32);
		literals.put("when", 33);
		literals.put("is", 34);
		literals.put("true", 38);
		literals.put("false", 39);
		literals.put("null", 40);

	}
	
	public Scanner (String fileName) {
		buffer = new Buffer(fileName);
		Init();
	}
	
	public Scanner(InputStream s) {
		buffer = new Buffer(s);
		Init();
	}
	
	void Init () {
		pos = -1; line = 1; col = 0; charPos = -1;
		oldEols = 0;
		NextCh();
		if (ch == 0xEF) { // check optional byte order mark for UTF-8
			NextCh(); int ch1 = ch;
			NextCh(); int ch2 = ch;
			if (ch1 != 0xBB || ch2 != 0xBF) {
				throw new FatalError("Illegal byte order mark at start of file");
			}
			buffer = new UTF8Buffer(buffer); col = 0; charPos = -1;
			NextCh();
		}
		pt = tokens = new Token();  // first token is a dummy
	}
	
	void NextCh() {
		if (oldEols > 0) { ch = EOL; oldEols--; }
		else {
			pos = buffer.getPos();
			// buffer reads unicode chars, if UTF8 has been detected
			ch = buffer.Read(); col++; charPos++;
			// replace isolated '\r' by '\n' in order to make
			// eol handling uniform across Windows, Unix and Mac
			if (ch == '\r' && buffer.Peek() != '\n') ch = EOL;
			if (ch == EOL) { line++; col = 0; }
		}
		if (ch != Buffer.EOF) {
			valCh = (char) ch;
			ch = Character.toLowerCase(ch);
		}

	}
	
	void AddCh() {
		if (tlen >= tval.length) {
			char[] newBuf = new char[2 * tval.length];
			System.arraycopy(tval, 0, newBuf, 0, tval.length);
			tval = newBuf;
		}
		if (ch != Buffer.EOF) {
			tval[tlen++] = valCh; 

			NextCh();
		}

	}
	

	boolean Comment0() {
		int level = 1, pos0 = pos, line0 = line, col0 = col, charPos0 = charPos;
		NextCh();
		if (ch == '/') {
			NextCh();
			for(;;) {
				if (ch == 10) {
					level--;
					if (level == 0) { oldEols = line - line0; NextCh(); return true; }
					NextCh();
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
		} else {
			buffer.setPos(pos0); NextCh(); line = line0; col = col0; charPos = charPos0;
		}
		return false;
	}

	boolean Comment1() {
		int level = 1, pos0 = pos, line0 = line, col0 = col, charPos0 = charPos;
		NextCh();
		if (ch == '*') {
			NextCh();
			for(;;) {
				if (ch == '*') {
					NextCh();
					if (ch == '/') {
						level--;
						if (level == 0) { oldEols = line - line0; NextCh(); return true; }
						NextCh();
					}
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
		} else {
			buffer.setPos(pos0); NextCh(); line = line0; col = col0; charPos = charPos0;
		}
		return false;
	}


	void CheckLiteral() {
		String val = t.val;
		val = val.toLowerCase();

		Object kind = literals.get(val);
		if (kind != null) {
			t.kind = ((Integer) kind).intValue();
		}
	}

	Token NextToken() {
		while (ch == ' ' ||
			ch >= 9 && ch <= 10 || ch == 13
		) NextCh();
		if (ch == '/' && Comment0() ||ch == '/' && Comment1()) return NextToken();
		int recKind = noSym;
		int recEnd = pos;
		t = new Token();
		t.pos = pos; t.col = col; t.line = line; t.charPos = charPos;
		int state = start.state(ch);
		tlen = 0; AddCh();

		loop: for (;;) {
			switch (state) {
				case -1: { t.kind = eofSym; break loop; } // NextCh already done 
				case 0: {
					if (recKind != noSym) {
						tlen = recEnd - t.pos;
						SetScannerBehindT();
					}
					t.kind = recKind; break loop;
				} // NextCh already done
				case 1:
					recEnd = pos; recKind = 1;
					if (ch >= '0' && ch <= '9' || ch == '_' || ch >= 'a' && ch <= 'z') {AddCh(); state = 1; break;}
					else {t.kind = 1; t.val = new String(tval, 0, tlen); CheckLiteral(); return t;}
				case 2:
					if (ch >= '0' && ch <= '9') {AddCh(); state = 3; break;}
					else {state = 0; break;}
				case 3:
					recEnd = pos; recKind = 3;
					if (ch >= '0' && ch <= '9') {AddCh(); state = 3; break;}
					else {t.kind = 3; break loop;}
				case 4:
					if (ch <= 9 || ch >= 11 && ch <= 12 || ch >= 14 && ch <= '&' || ch >= '(' && ch <= '[' || ch >= ']' && ch <= 65535) {AddCh(); state = 4; break;}
					else if (ch == 39) {AddCh(); state = 5; break;}
					else {state = 0; break;}
				case 5:
					{t.kind = 4; break loop;}
				case 6:
					if (ch <= 9 || ch >= 11 && ch <= 12 || ch >= 14 && ch <= '!' || ch >= '#' && ch <= '[' || ch >= ']' && ch <= 65535) {AddCh(); state = 6; break;}
					else if (ch == '"') {AddCh(); state = 7; break;}
					else {state = 0; break;}
				case 7:
					{t.kind = 5; break loop;}
				case 8:
					recEnd = pos; recKind = 2;
					if (ch >= '0' && ch <= '9') {AddCh(); state = 8; break;}
					else if (ch == '.') {AddCh(); state = 2; break;}
					else {t.kind = 2; break loop;}
				case 9:
					{t.kind = 7; break loop;}
				case 10:
					{t.kind = 9; break loop;}
				case 11:
					{t.kind = 10; break loop;}
				case 12:
					{t.kind = 11; break loop;}
				case 13:
					{t.kind = 13; break loop;}
				case 14:
					{t.kind = 17; break loop;}
				case 15:
					{t.kind = 23; break loop;}
				case 16:
					{t.kind = 24; break loop;}
				case 17:
					{t.kind = 36; break loop;}
				case 18:
					{t.kind = 37; break loop;}
				case 19:
					{t.kind = 41; break loop;}
				case 20:
					{t.kind = 42; break loop;}
				case 21:
					{t.kind = 43; break loop;}
				case 22:
					{t.kind = 44; break loop;}
				case 23:
					{t.kind = 45; break loop;}
				case 24:
					{t.kind = 46; break loop;}
				case 25:
					{t.kind = 47; break loop;}
				case 26:
					{t.kind = 48; break loop;}
				case 27:
					{t.kind = 50; break loop;}
				case 28:
					{t.kind = 51; break loop;}
				case 29:
					{t.kind = 52; break loop;}
				case 30:
					{t.kind = 53; break loop;}
				case 31:
					{t.kind = 54; break loop;}
				case 32:
					{t.kind = 55; break loop;}
				case 33:
					{t.kind = 61; break loop;}
				case 34:
					{t.kind = 62; break loop;}
				case 35:
					recEnd = pos; recKind = 12;
					if (ch == '=') {AddCh(); state = 29; break;}
					else if (ch == '<') {AddCh(); state = 31; break;}
					else {t.kind = 12; break loop;}
				case 36:
					recEnd = pos; recKind = 14;
					if (ch == '=') {AddCh(); state = 30; break;}
					else if (ch == '>') {AddCh(); state = 32; break;}
					else {t.kind = 14; break loop;}
				case 37:
					recEnd = pos; recKind = 18;
					if (ch == '>') {AddCh(); state = 17; break;}
					else if (ch == '=') {AddCh(); state = 27; break;}
					else {t.kind = 18; break loop;}
				case 38:
					recEnd = pos; recKind = 35;
					if (ch == '|') {AddCh(); state = 24; break;}
					else {t.kind = 35; break loop;}
				case 39:
					recEnd = pos; recKind = 56;
					if (ch == '=') {AddCh(); state = 19; break;}
					else if (ch == '+') {AddCh(); state = 33; break;}
					else {t.kind = 56; break loop;}
				case 40:
					recEnd = pos; recKind = 57;
					if (ch == '=') {AddCh(); state = 20; break;}
					else if (ch == '-') {AddCh(); state = 34; break;}
					else {t.kind = 57; break loop;}
				case 41:
					recEnd = pos; recKind = 58;
					if (ch == '=') {AddCh(); state = 21; break;}
					else {t.kind = 58; break loop;}
				case 42:
					recEnd = pos; recKind = 59;
					if (ch == '=') {AddCh(); state = 22; break;}
					else {t.kind = 59; break loop;}
				case 43:
					recEnd = pos; recKind = 60;
					if (ch == '=') {AddCh(); state = 23; break;}
					else {t.kind = 60; break loop;}
				case 44:
					recEnd = pos; recKind = 49;
					if (ch == '&') {AddCh(); state = 25; break;}
					else {t.kind = 49; break loop;}
				case 45:
					recEnd = pos; recKind = 63;
					if (ch == '=') {AddCh(); state = 28; break;}
					else {t.kind = 63; break loop;}

			}
		}
		t.val = new String(tval, 0, tlen);
		return t;
	}
	
	private void SetScannerBehindT() {
		buffer.setPos(t.pos);
		NextCh();
		line = t.line; col = t.col; charPos = t.charPos;
		for (int i = 0; i < tlen; i++) NextCh();
	}
	
	// get the next token (possibly a token already seen during peeking)
	public Token Scan () {
		if (tokens.next == null) {
			return NextToken();
		} else {
			pt = tokens = tokens.next;
			return tokens;
		}
	}

	// get the next token, ignore pragmas
	public Token Peek () {
		do {
			if (pt.next == null) {
				pt.next = NextToken();
			}
			pt = pt.next;
		} while (pt.kind > maxT); // skip pragmas

		return pt;
	}

	// make sure that peeking starts at current scan position
	public void ResetPeek () { pt = tokens; }

} // end Scanner
