package cino;

import static java.lang.Long.rotateLeft;
import static java.lang.Long.rotateRight;

public class VM {

	public static void main(String[] args) {
//		run("[(1>){(1-)(f,(1-)f+)}].f 3f");
//		run("[(1>){((1-)f,(2-)f+)}].f 7f(2f+)");
//		run("2(1<){(3+)}(2+){(1+)}");
		run("[(1>){(1-)(@,(1-)@+)}] 7z");
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
		final long[] cs; int lp;
		final int[]  rs;
		final int[]  nb;
		final int[]  bs;

		Process(byte[] pb, int stack) {
			this.pb = pb;
			this.cs = new long[stack];
			this.rs = new  int[stack];
			this.bs = new  int[stack];
			this.nb = new int[26];
		}

	}

	private static final class CPU {

		// stacks
		long[] cs; int lp; // computing stack; pointer to L (PC is lp+1)
		int[]  rs, bs; int cp = -1; // return stack; block stack, continue pointer (top on RS, BS)
		
		// registers
		long l;	// left  (next on CS)
		long r;	// right (top on CS)
		int  b; // block (a PC index)
		boolean  t, e;	// test, else

		// buffers
		byte[] pb; // program buffer
		int[]  nb; // name buffer (name to index in PB map)

		public void resume(Process p) {
			// static state (re-reference for better readability in java)
			pb  = p.pb;
			nb	= p.nb;
			// execution state
			cs = p.cs;
			rs = p.rs;
			bs = p.bs;
			lp = p.lp;
			l = cs[lp];
			r = cs[lp+1];

			resumeAt(p.pc);
		}

		private void resumeAt(int pcc) {
			int pc = pcc;
			while (pc < pb.length) {
				printStack();
				final int i = pb[pc++];
				System.out.println((char)i);
				switch(i) {
				// arithmetic
				case '+' : l+=r; break;
				case '-' : l-=r; break;
				case '*' : l*=r; break;
				case '/' : l/=r; break;
				case '%' : l=l%r; break;
				case '_' : r=-r; break;
				case 'x' : r++; break;
				case 'y' : r--; break;
				// bitwise
				case '&' : l&=r; break;
				case '|' : l|=r; break;
				case '~' : r=~r;  break;
				case '^' : l=l^r; break;
				case '`' : l=rotateRight(l, (int)r); break;
				case '´' : l=rotateLeft(l, (int)r); break;
				// tests
				case '=' : t |= l==r; e=false; break;
				case '!' : t |= l!=r; e=false; break;
				case '<' : t |= l<r;  e=false; break;
				case '>' : t |= l>r;  e=false; break;
				// branching
				case '{' : if (!(t || e)) { pc = afterNext(pc, '{', '}'); } e=!t; t=false; break;
				case '}' : e=false; break;
				// stack action
				case '(' : cs[lp++]=l; l=r; break;
				case ')' : r=l; l= lp < 0 ? 0 : cs[--lp]; break;
				case ',' : l=l^r; r=l^r; l=l^r; break;
				case ':' : r=l; lp--; l=cs[lp]; break;
				// no-ops
				case ' ' :
				case '\n':
				case '\r': break;
				// blocks
				case '[' : b=pc; pc=afterNext(pc, '[', ']'); break;
				case ']' : pc=rs[cp--]; break;
				case '.' : nb[pb[pc++]-'a']=b; break;
				case '$' : b=nb[pb[pc++]-'a']; break;
				case '@' : bs[cp+1]=bs[cp]; rs[cp+1]=pc; pc=bs[cp++]; break;
				case 'z' : bs[++cp]=b; rs[cp]=pc; pc=b; break;
				// vector
				case '\'':
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
						int bs0 = nb[i-'a'];
						rs[++cp] = pc;
						bs[cp] = bs0;
						pc=bs0;
					}
					else
						notDefined(' ',i);
				}
			}
			printStack();
			System.out.println();
		}

		private void printStack() {
			System.out.print(String.format("%s %s | ", t?"t":"f", e?"t":"f"));
			for (int i = 0; i < lp; i++) {
				System.out.print(String.format("%d ", cs[i]));
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