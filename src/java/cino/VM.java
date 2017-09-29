package cino;

import static java.lang.Long.rotateLeft;
import static java.lang.Long.rotateRight;

public class VM {

	// new ideas:
	// vectors have info about their elements (muti-dim-arrays och arrays of records)
	// functions/blocks should "compose" like the computation does with L and R registers (==> pushing multiple block indexes onto the stack should require explicit stack duplication as with a.-stack)
	
	
	public static void main(String[] args) {
//		run("[(1>){(1-)(f,(1-)f+)}]:f 7f");
//		run("[(1>){((1-)f,(2-)f+)}]:f 7f(2f+)");
//		run("2(1<){(3+)}(2+){(1+)}");
//		run("[(1>){(1-)(@,(1-)@+)}]:f 7f");
//		run("3(2>){(3=){(5+)}}");
		run("3:3<(2<(4<)){+}");
		
		System.out.println("done");
	}

	public static void run(String program) {
		run(program.getBytes());
	}

	public static void run(byte[] pb) {
		new CPU().resume(new Process(pb, 128));
	}

	static final class Program {

		final byte[] pb;

		Program(byte[] pb) {
			this.pb = pb;
		}

	}


	static final class Process {

		final byte[] pb; int pc;
		final long[] as; int lp;
		final int[]  bs;
		final int[]  rs;
		final int[]  bb;

		Process(byte[] pb, int stack) {
			this.pb = pb;
			this.as = new long[stack];
			this.rs = new  int[stack];
			this.bs = new  int[stack];
			this.bb = new int[127];
		}

	}

	private static final class CPU {

		// stacks
		long[] as; int lp; // arithmetic stack; pointer to L (R - top of cs - is lp+1)
		int[]  rs; int cp = -1; // return stack; continue pointer (top on RS, BS)
		int[]  bs; int bp = -1; // block stack, block pointer
		
		// registers
		long l;	// left  (next on AS)
		long r;	// right (top on AS)
		boolean  t;	// test

		// buffers
		byte[] pb; // program buffer
		int[]  bb; // block buffer (named blocks to index into PB)

		public void resume(Process p) {
			// static state (re-reference for better readability in java)
			pb  = p.pb;
			bb	= p.bb;
			// execution state
			as = p.as;
			rs = p.rs;
			bs = p.bs;
			lp = p.lp;
			l = as[lp];
			r = as[lp+1];

			resumeAt(p.pc);
		}

		private void resumeAt(int pcc) {
			int pc = pcc;
			while (pc < pb.length) {
				printStack();
				final int i = pb[pc++];
				System.out.println(String.format("| %s", String.valueOf((char)i)));
				switch(i) {
				// arithmetic
				case '+' : l+=r; break;
				case '-' : l-=r; break;
				case '*' : l*=r; break;
				case '/' : l/=r; break;
				case '%' : l=l%r; break;
				case '_' : r=-r; break; // neg 
				case 'x' : r++; break; // inc (no real symbol yet)
				case 'y' : r--; break; // dec (no real symbol yet)
				// bitwise
				case '&' : l&=r; break;
				case '|' : l|=r; break;
				case '~' : r=~r;  break;
				case '^' : l=l^r; break;
				case '`' : l=rotateRight(l, (int)r); break;
				case '´' : l=rotateLeft(l, (int)r); break;
				// tests
				case '=' : t = l==r; break;
				case '<' : t = l<r;  break;
				case '>' : t = l>r;  break;
				case '!' : t = !t; break;
				// branching
				case '{' : if (!t) { pc = afterNext(pc, '{', '}'); } break;
				case '}' : t=true; break;
				case '(' : if (t) { pc = afterNext(pc, '(', ')'); } break;
				case ')' : break; // this is a NOOP
				// stack action
				case ':' : as[lp++]=l; l=r; break;
				case '.' : r=l; l= lp < 0 ? 0 : as[--lp]; break;
				case ',' : l=l^r; r=l^r; l=l^r; break; // swap l/r
				case ';' : r=l; lp--; l=as[lp]; break; // dup r
				// no-ops
				case ' ' :
				case '\n':
				case '\r': break;
				// blocks
				case '[' : bs[++bp]=pc; pc=afterNext(pc, '[', ']'); break;
				case ']' : pc=rs[cp--]; break;
				case '@' : rs[++cp]=pc; pc=bs[bp]; break; // call and pop top fn on BS
				case '\'' : bb[pb[pc++]]=bs[bp--]; break; // set named index to BS
				case '$' : bs[++bp]=bb[pb[pc++]]; break; // get named index to BS
				// vector
				case '#':
					switch (pb[pc++]) {
					case '*':
					default: notDefined(' ', i);
					}
				default:
					// load constants
					if (i >= '0' && i <= '9') {
						r=i-'0';
					} else
					if (i >= 'a' && i <= 'z') {
						rs[++cp] = pc;
						pc=bb[i];
						bs[++bp] = pc;
					}
					else
						notDefined(' ',i);
				}
			}
			printStack();
			System.out.println();
		}

		private void printStack() {
			System.out.print(String.format("%s ", t?"T":" "));
			for (int i = 0; i < lp; i++) {
				System.out.print(String.format("%d ", as[i]));
			}
			System.out.print(String.format("%d %d \t", l, r));
		}

		private void notDefined(char group, int inst) {
			throw new RuntimeException("Instruction not defined: "+group+new String(new byte[]{(byte) inst}));
		}

		private int afterNext(int pc, char inc, char dec) {
			int c = 1;
			while (c > 0) {
				if (pb[pc] == inc) {
					c++;
				}
				if (pb[pc] == dec) {
					c--;
				}
				pc++;
			}
			return pc;
		}
		
		@Override
		public String toString() {
			return new String(pb)+"\n";
		}

	}
	
}