package cino;

import static java.lang.Long.rotateLeft;
import static java.lang.Long.rotateRight;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.copyOfRange;

import java.io.PrintStream;
import java.util.Arrays;

public class VM {

	public static void run(byte[] program) {
		CPU cpu = new CPU();
		Program prog = new Program(program);
		cpu.cont(new Process(prog, 20));
	}
	
	static final class Program {
		
		final byte[] pb;
		final int[]  eh  = new int[10];
		final int[]  a_z = new int[26];
		final int[]  A_Z = new int[26];

		Program(byte[] pb) { this.pb = pb; index(); }

		private void index() {
			boolean sol = true;
			for (int i = 0; i < pb.length; i++) {
				byte inst = pb[i];
				if (sol) {
					if (inst >= '0' && inst <= '9') {
						eh[inst-'0'] = i+1;
					} else
					if (inst >= 'a' && inst <= 'z') {
						a_z[inst-'a'] = i+1;
					} else
					if (inst >= 'A' && inst <= 'Z') {
						A_Z[inst-'A'] = i+1;
					}
				}
				sol = inst == '\n';
			}
		}
		
	}
	
	static final class Process {
		
		final Program prog;	int pi;
		final long[]   ws;  int li;
		final byte[][] vs;  int di;
		final int[]    is;  int ti;
		
		Process(Program prog, int stack) {	
			this.prog = prog;
			this.ws = new long[stack];
			this.vs = new byte[stack][];
			this.is = new  int[stack];			
		}
		
	}
	
	static final class CPU {

		boolean debug;
		
		long[]   ws; int li; long l; long r;
		byte[][] vs; int di;
		int[]    is; int ti;
		byte[]   pb; int pi;
		
		int[]    eh  = new int[10];
		int[]    a_z = new int[26];
		int[]    A_Z = new int[26];
		
		public void cont(Process p) {
			// static state (re-reference for better readability in java)
			pb  = p.prog.pb;
			eh  = p.prog.eh;
			a_z = p.prog.a_z;
			A_Z = p.prog.A_Z;
			// execution state
			ws = p.ws;
			vs = p.vs;
			is = p.is;
			pi = p.pi;
			li = p.li;
			di = p.di;
			ti = p.ti;
			l = ws[li];
			r = ws[li+1];
			
			main();
		}
		
		@Override
		public String toString() {
			return new String(pb);
		}
		
		void main() {
			while (pi < pb.length) {
				if (debug) {
					System.out.printf("%s l: %d r: %d li: %d\n", new String(new byte[] {pb[pi]}), l, r, li);
				}
			switch(pb[pi++]) {
			// math
			case '+' : l=l+r; break;
			case '-' : l=l-r; break;
			case '*' : l=l*r; break;
			case '/' : l=l/r; break;
			case '\\': math(); break;
			// logic
			case '&' : l=l&r; break;
			case '|' : l=l|r; break;
			case '~' : r=~r; break;
			case '`' : l=l^r; break;
			// cmp
			case '=' : r=l==r?1:0; break;
			case '<' : r=l< r?1:0; break;
			case '>' : r=l> r?1:0; break;
			case '{' : r=l<=r?1:0; break;
			case '}' : r=l>=r?1:0; break;
			case ',' : r=r<=l && r>=ws[li-1]?1:0; break;
			// flow
			case ':' : int t=is[ti]; is[ti]=pi; pi=t; break;
			case ';' : pi=is[ti--]; break;
			case '.' : pi=is[ti]; break;
			case '(' : is[++ti]=pi; break;
			case ')' : ti--; break;
			case '!' : pi=eh[(int)r]; li=0; di=0; ti=0; l=0; r=0; break;
			case '?' : if (r==0) { pi++; } /*sag r=l; if (li > 0) { l=ws[--li]; } */ break;
			case '\'': r = 0; while (pb[pi++] != '\'') { r <<= 8; r |= pb[pi-1]; }; break;
			case '"' : int si = pi; while (pb[pi++] != '"'); vs[++di] = copyOfRange(pb, si, pi-1); break;
			// stack
			case '^' : ws[li++]=l; l=r; break;
			case '_' : r=l; if (li > 0) { l=ws[--li]; } break;
			case '%' : l=l^r; r=l^r; l=l^r; break;
			// process 
			case '#' : process(); break;
			// vector
			case '[' : vector(); break;
			// misc
			case '$' : System.out.println(new String(vs[di--])); break;
			// no-ops
			case ' ' : 
			case '\n':
			case '\r':
			case '\t': break;  
			// load
			default:
				byte i = pb[pi-1];
				if (i >= '0' && i <= '9') {
					r=i-'0';
				} else
				if (i >= 'a' && i <= 'z') {
					t=a_z[i-'a'];
					checkTarget(t, i);
					is[++ti] = pi;
					pi=t;
				} else
				if (i >= 'A' && i <= 'Z') {
					t=A_Z[i-'A'];
					checkTarget(t, i);
					is[++ti] = t;
				} else 
				if (i < 0) { // absolute index
					t = ((pb[pi-1] & 0b01111111) << 8) + pb[pi++];
					checkTarget(t, i);
					is[++ti] = t;
				} else
					notDefined(i); 
			}
			}
			state();
		}

		private void process() {
			switch(pb[pi++]) {
			case '-' : pi=pb.length; break;
			default  : notDefined((byte)'#', pb[pi-1]);
			}
		}

		private void vector() {
			switch(pb[pi++]) {
			case ',' : vs[++di] = new byte[] { (byte) ('0'+l) }; break; // just to test
			default  : notDefined((byte)'[', pb[pi-1]);
			}
		}

		private void math() { // \? 
			switch(pb[pi++]) {
			case '~' : l=l%r; break;
			case '{' : l=min(l, r); break;
			case '}' : l=max(l, r); break;
			case '<' : l=l<<r; break;
			case '>' : l=l>>r; break;
			case '\\': l=rotateLeft(l, (int)r); break;
			case '/' : l=rotateRight(l, (int)r); break;
			}
		}

		private void notDefined(byte... is) {
			throw new RuntimeException("Instruction not defined: "+new String(is));
		}
		
		private void checkTarget(int t, byte... is) {
			if (t<=0)
				throw new RuntimeException("Target not defined: "+new String(is));
		}
		
		private void state() {
			PrintStream out = System.out;
			out.print("WS: ");
			for (int i = 0; i < li; i++) {
				out.print(ws[i]);
				out.print(' ');
			}
			out.print(l);
			out.print(' ');
			out.println(r);
		}

	}
	
}
