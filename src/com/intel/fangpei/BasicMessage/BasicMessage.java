package com.intel.fangpei.BasicMessage;

import java.util.HashMap;
import java.util.Map;
/**
 * Defined all the operation Command here.
 * <p>these command is used in {@link com.intel.fangpei.terminal.Node},{@link com.intel.fangpei.terminal.Admin}
 * <p>{@link com.intel.fangpei.terminal.SelectSocket} will dispatch all request by the Command.
 */
public class BasicMessage {
	Map<Integer, String> s = new HashMap<Integer, String>();
	public static final int NODE = 81;
	public static final int ADMIN =   80;
	public static final int SERVER =   79;
	public static final int OP_LOGIN =   15;
	public static final int OP_QUIT =   10;
	public static final int OP_CLOSE =   9;
	public static final int OP_EXEC =   1;
	public static final int OP_MESSAGE =   2;
	public static final int OP_SYSINFO =   11;
//	public static final byte OP_lOAD_DISK =   5;
	public static final int VERSION = 1;
	public static final int OP_HELP =   8;
	public static final int OK =   7;
	public static final int OP_SH =   12;
	public static final int TASKS =   31;
	public static final int HOSTS =   35;
	public static final int CHILDS =   36;
	public static final int TASKINFO =   32;
	public static final int HOSTINFO =   34;
	public static final int CHILDINFO =  33;
}
