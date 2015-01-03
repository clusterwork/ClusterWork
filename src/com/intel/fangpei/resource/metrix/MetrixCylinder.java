package com.intel.fangpei.resource.metrix;

import java.util.concurrent.CopyOnWriteArrayList;

import com.intel.fangpei.resource.metrix.Metrix;
import com.intel.fangpei.util.TimeCounter;

//one metric description
public class MetrixCylinder {
	int splitNum = 0 ;
	Class type = null;
	CopyOnWriteArrayList<Metrix>[] splits = null;
	public MetrixCylinder(Class type){
		splitNum = MCODE.total;
		setType(type);
		splits = new CopyOnWriteArrayList[splitNum];
		for ( int i =0;i<splitNum;i++){
			splits[i] = new CopyOnWriteArrayList();
		}
}
	protected void freshSplit() {
		for (int stateid = 0;stateid < splitNum;stateid++){
			CopyOnWriteArrayList<Metrix> list = splits[stateid];
			int num = list.size();
			
			for (int j = 0;j < num;j++){
				if(list.get(j).state== stateid){
					continue;
				}
				else{
					int newstate = list.get(j).state;
					if(newstate < splitNum){
						splits[newstate].add(list.get(j));
						//delete from old split
						list.remove(j);
					}
					else{
						list.remove(j);
						System.out.println("[freshsplit] split state out of range,remove!");
					}
				}
			}
		}
		
	}
	protected Metrix find(int id){
		for ( int state = 0 ; state < splitNum; state++){
			CopyOnWriteArrayList<Metrix> split = splits[state];
			int len = split.size();
			for ( int i = 0 ; i < len; i++){
				if ( split.get(i).id == id ) {
					return split.get(i);
				}
			}
		}
		return null;
			
	}
	public void setType(Class type){
		this.type = type;
		//checkAllTypeMatch();
	}
	public Metrix add(Metrix metrix,int state){
		if ( state >= splitNum || metrix == null){
			//out of control state
			System.out.println("state is null or not support state!");
			return null;
		}
		//check type?
		if ( type != null ){
			if ( metrix.getClass() != type ){
				System.out.println("type not matched!");
				return null;
			}
		}
		if (splits == null){
			System.out.print("split is null");
		}
		splits[state].add(metrix);
		return metrix;
	}
//should support remove?
//	public void remove(Metrix metrix){
//		if (metrix == null){
//			System.out.println("Metrix is null!");
//			return;
//		}
//	}
	
}