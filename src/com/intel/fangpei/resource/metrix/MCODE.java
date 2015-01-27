package com.intel.fangpei.resource.metrix;

public class MCODE {
	public static int total = 5;
	public static int INIT = 0;
	public static int RUNNING = 1;
	public static int FINISH = 2;
	public static int INTERRUPT = 3;
	public static int OTHER = 4;
	public enum CODESTR{
		init("初始化",0),
		running("运行中",1),
		finish("完成",2),
		interrupt("中断",3),
		other("其他",4);
		private String name;
		private int index;
		private CODESTR(String name,int index){
			this.name=name;
			this.index = index;
		}
        public static String getName(int index) {
            for (CODESTR c : CODESTR.values()) {
                if (c.index == index) {
                    return c.name;
                }
            }
            return null;
        }
	};
}
