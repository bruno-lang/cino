package cino;

import static java.lang.Long.rotateLeft;
import static java.lang.Long.rotateRight;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.copyOfRange;

import java.io.PrintStream;
import java.util.Arrays;

import javax.naming.OperationNotSupportedException;

public class VM {

	public static void run(byte[] program) {
		CPU cpu = new CPU();
		Program prog = new Program(program);
		cpu.cont(new Process(prog, 50));
	}

	static final class Program {

		final byte[] pb;
		final int[]  jt;
		final int[]  eh  = new int[10];

		Program(byte[] pb) { 
			this.pb = pb; 
			this.jt = prepare(); 
		}

		private int[] prepare() {
			int n = 1;
			int[] jt = new int[128];
			// index namespaces
			boolean sol = true;
			if (false) {
				final int[]  A_Z = new int[27];
				A_Z[26] = pb.length;
				for (int i = 0; i < pb.length; i++) {
					byte inst = pb[i];
					if (sol) {
						if (inst >= '0' && inst <= '9') {
							eh[inst-'0'] = i+1;
							pb[i] = ' ';
						} else
							if (inst >= 'A' && inst <= 'Z') {
								A_Z[inst-'A'] = i+1;
								pb[i] = ' ';
							}
					}
				}
			}
			final int[]  a_z = new int[26];
			//			for (int ns = 0; ns < A_Z.length-1; ns++) {
			//				int sons = A_Z[ns];
			//				int eons = A_Z[ns+1];
			// index blocks a-z (in current namespace)
			sol = true;
			for (int i = 0; i < pb.length; i++) {
				byte inst = pb[i];
				if (sol) {
					if (inst >= 'a' && inst <= 'z') {
						a_z[inst-'a'] = i;
					}
				}
				sol = inst == '\n';
			}
			// substitute letters with jump table index
			for (int i = 0; i < a_z.length; i++) {
				int idx = a_z[i];
				byte l = (byte) ('a'+i);
				if (idx != 0) {
					for (int k = 0; k < pb.length; k++) {
						if (pb[k] == l) {
							pb[k] = (byte) -n;
						}
					}
					int e = idx;
					while (pb[++e] != '\n');
					block(idx, e);
					jt[n++] = idx;
				}
			}
			//			}
			return Arrays.copyOf(jt, n);
		}

		void block(int sob, int eob) {
			pb[sob-1] = '`'; // return
			pb[sob] = (byte) (eob-sob+1);
			int i = sob+1;
			while (i < eob) {
				byte inst = pb[i];
				if (inst == '?') {
					pb[i] = pb[i+1] == '?' ? 127 : (byte) (pb[i+1]-'0');
					int e = i+1;
					while (pb[e++] != ' ');
					pb[i+1] = (byte) (e-i-1);
					pb[e-1] = '`'; // return
					i=e;
				} else {
					i++;
				}
			}
		}
	}


	static final class Process {

		final Program prog;		int pi;
		final long[]   ws;  	int li;
		final byte[][] vs;  	int di;
		final int[]    is, ie;  int ti;

		Process(Program prog, int stack) {	
			this.prog = prog;
			this.ws = new long[stack];
			this.vs = new byte[stack][];
			this.is = new  int[stack];
			this.ie = new  int[stack];
		}

	}

	static final class CPU {

		boolean debug = true;

		long[]   ws; 	 int li; long l; long r;
		byte[][] vs; 	 int di;
		int[]    is, ie; int ti = -1;
		byte[]   pb;

		int[]    eh, jt;

		public void cont(Process p) {
			// static state (re-reference for better readability in java)
			pb  = p.prog.pb;
			eh  = p.prog.eh;
			jt  = p.prog.jt;
			// execution state
			ws = p.ws;
			vs = p.vs;
			is = p.is;
			ie = p.ie;
			li = p.li;
			di = p.di;
			ti = p.ti;
			l = ws[li];
			r = ws[li+1];

			eval(p.pi, pb.length);
		}

		@Override
		public String toString() {
			return new String(pb);
		}

		void eval(int ps, int pe) {
			int pi = ps;
			System.out.printf("%d %d\n", ps, pe);
			while (pi < pe) {
				if (debug) {
					System.out.printf("%s ", (char)pb[pi]);
					state();
				}
				final byte i = pb[pi++]; 
				switch(i) {
				// math
				case '+' : l=l+r; break;
				case '-' : l=l-r; break;
				case '*' : l=l*r; break;
				case '/' : l=l/r; break;
				case '%' : l=l%r; break;
				case ')' : r++; break;
				case '(' : r--; break;
				case '\\': 
					switch(pb[pi++]) {
					case '{' : l=min(l, r); break;
					case '}' : l=max(l, r); break;
					case '<' : l=l<<r; break;
					case '>' : l=l>>r; break;
					case '\\': l=rotateLeft(l, (int)r); break;
					case '/' : l=rotateRight(l, (int)r); break;
					}				
					break;
					// logic
				case '&' : l=l&r; break;
				case '|' : l=l|r; break;
				case '~' : r=~r;  break;
				case '^' : l=l^r; break;
				// cmp
				case '=' : 
					switch(pb[pi++]) {
					case '=' : r=l==r?1:0; break;
					case '<' : r=l<=r?1:0; break;
					case '>' : r=l>=r?1:0; break;
					case '~' : r=r<=l && r>=ws[li-1]?1:0; break;
					default  : notDefined('=', pb[pi-1]);
					}				
					break;
				case '<' : r=l<r?1L:0L; break;
				case '>' : r=l>r?1L:0L; break;
				// jump (internal)
				case 0   :   
				case 1   : 
				case 2   :
				case 3   :
				case 4   :
				case 5   :
				case 6   :
				case 7   :
				case 8   :
				case 9   : if (r==i) { pi++; } else { pi+=pb[pi]; } break;
				case 127 : if (r!=0) { pi++; } else { pi+=pb[pi]; } break;
				case 14  : pi-=pb[pi]; break;
				case 15  : pi+=pb[pi]; break;
				case 16  : pi=ps; break;
				// flow
				case '`' : return;
				/*
			case ':' : int t=is[ti]; is[ti]=pi; pi=t; break;
			case ';' : pi=is[ti--]; break;
			case '.' : pi=is[ti]; break;
			case '{' : is[++ti]=pi; break;
			case '}' : ti--; break;
			case '!' : pi=eh[(int)r]; li=0; di=0; ti=0; l=0; r=0; break;
				 */
				// quote
				case '[' : is[++ti] = pi; while (pb[pi] != ']') pi++; ie[ti]=pi; pi++; break;
				// stack
				case ':' : ws[li++]=l; l=r; break;
				case ';' : r=l; break;
				case ',' : l=l^r; r=l^r; l=l^r; break;
				case '_' : r=l; if (li > 0) { l=ws[--li]; } break;
				// process 
				case '#' : throw new UnsupportedOperationException();
				// vector
				case '.' : 
					switch(pb[pi++]) {
					// just to test
					case ',' : vs[++di] = new byte[] { (byte) ('0'+l) }; break; 
					// loops
					case '*' : int pi_old = pi; int n = (int) r; eval(is[ti], ie[ti--]); int s=is[ti]; int e=ie[ti--]; for (int j = n; j > 0; j--) { eval(s, e); } pi=pi_old; break;
					default  : notDefined('[', pb[pi-1]);
					}				
					break;
					// print
				case '$' : System.out.println(new String(vs[di--])); break;
				// no-ops
				case ' ' : 
				case '\n':
				case '\r': break;
				// load
				case '\'': r = 0; while (pb[pi++] != '\'') { r <<= 8; r |= pb[pi-1]; }; break;
				case '"' : int si = pi; while (pb[pi++] != '"'); vs[++di] = copyOfRange(pb, si, pi-1); break;
				default:
					if (i >= '0' && i <= '9') {
						r=i-'0';
					} else
						if (i < 0) { // call via jump table
							int t = jt[-i];
							eval(t+1, t+(pb[t]&0xFF));
						} else
							notDefined(' ',i); 
				}
			}
			state();
		}

		private void notDefined(char group, byte inst) {
			throw new RuntimeException("Instruction not defined: "+group+new String(new byte[]{inst}));
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