package com.intel.fangpei.task;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.process.ChildStrategy;
import com.intel.fangpei.process.MyChildStrategy2;
import com.intel.fangpei.process.ProcessManager;
import com.intel.fangpei.task.NodeTaskManager.PriorityLine.PriorityNode;
import com.intel.fangpei.util.ConfManager;

public class NodeTaskManager implements Runnable {
	private static int DEFAULT_JVM_LIMIT = 3;
	/**
	 * jvm限制个数
	 */
	private int jvmLimit = 0;
	/**
	 * 当前jvm个数，等同于childstrategy个数
	 */
	private int jvmNum = 0;
	/**
	 * 已启动的jvm个数，包括已完成的jvm个数
	 */
	private int startedJvmNum = 0;
	/**
	 * 判断当前nodetaskmanager是否启动，因为该线程只会启动一次
	 */
	private boolean isStarted = false;

	private ReentrantLock lock = new ReentrantLock();

	PriorityLine line;

	public NodeTaskManager() {
		jvmLimit = ConfManager.getInt("jvm.limit", DEFAULT_JVM_LIMIT);
		// jvmLimit = 3;
		line = new PriorityLine();
	}

	/**
	 * add a node to line
	 * 
	 * @param ts
	 * @param priority
	 */
	public synchronized void registerTaskRunner(TaskRunner tr, int priority) {
		System.out.println("register taskrunner,priority : " + priority);
		line.addNode(priority, tr);
	}

	/**
	 * 用于测试，打印排序后的结果
	 */
	public void printSortedMap() {
		line.print();
	}

	/**
	 * 获取当前jvm个数，如果大于等于限制jvmLimit，则沉睡1秒，然后再获取当前jvm个数。
	 * 接着按顺序启动sortedTaskRunners中的TR，直到当前jvm个数+下一个TR的jvm个数超过限制jvmLimit或者map元素读完。
	 */
	@Override
	public void run() {
		isStarted = true;
		LocalRetry lr = new LocalRetry();
		lr.start();
		while (true) {
			startedJvmNum = line.getStartedNum();
			jvmNum = startedJvmNum - ProcessManager.getFinishedJvmNum();
			while (jvmNum >= jvmLimit) {
				try {
					Thread.sleep(1000);
					System.out.println("Zzz..");
					jvmNum = startedJvmNum - ProcessManager.getFinishedJvmNum();
					System.out.println("process jvm num is : " + jvmNum);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			lock.lock();

			try {
				PriorityNode node = line.firstNode();
				while (node != null) {
					if (node.v.isStarted()) {
						node = node.next;
					} else {
						TaskRunner tr = node.v;
						if (tr.getJvmNum() + jvmNum <= jvmLimit) {
							tr.setStarted(true);
							new Thread(tr).start();
							jvmNum += tr.getJvmNum();
							System.out.println("start thread priority is :"
									+ node.k + ", jvmNum is:" + jvmNum
									+ ", runtime is "
									+ ProcessManager.getFinishedJvmNum());
						} else {
							break;
						}
					}
				}
			} finally {
				lock.unlock();

			}
		}
	}

	/**
	 * 判断当前线程是否已经启动
	 * 
	 * @return
	 */
	public synchronized boolean isStarted() {
		System.out.println("ntm is started : " + isStarted);
		return isStarted;
	}

	/**
	 * 设置当前线程状态
	 * 
	 * @param isStarted
	 */
	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}

	class PriorityLine {
		protected PriorityNode head = null;

		public class PriorityNode {
			Integer k = null;
			TaskRunner v = null;
			PriorityNode next = null;

			PriorityNode(Integer k, TaskRunner v) {
				this.k = k;
				this.v = v;
			}
		}

		private void addNode(PriorityNode node) {
			PriorityNode p = head;
			PriorityNode prev = p;
			if (p == null) {
				head = node;
			}
			// 如果node比head元素还要大，则node成为新的head
			else if (head.k.compareTo(node.k) < 0) {
				node.next = head;
				head = node;
			} else {
				while (p.next != null && p.next.k.compareTo(node.k) >= 0) {
					p = p.next;
				}
				// 如果最后一个元素都比node大，则node添加到链表尾端
				if (p.next == null) {
					p.next = node;
				}
				// node插入p和p.next之间
				else {
					PriorityNode tmp = p.next;
					p.next = node;
					node.next = tmp;
				}

			}
		}

		public void addNode(Integer k, TaskRunner v) {
			lock.lock();
			System.out.println("line lock");
			try {
				System.out.println("add node");
				addNode(new PriorityNode(k, v));
			} finally {
				System.out.println("line unlock");
				lock.unlock();
			}
		}

		public PriorityNode firstNode() {
			return head;
		}

		// 暂未使用
		public PriorityNode firstUnstartedNode() {
			PriorityNode p = head;
			if (p == null) {
				return null;
			}
			while (p != null) {
				if (p.v.isStarted()) {
					p = p.next;
				} else {
					return p;
				}
			}
			return p;

		}

		public void print() {
			PriorityNode p = head;
			while (p != null) {
				System.out.println("priority is : " + p.k);
				p = p.next;
			}
		}

		public int getStartedNum() {
			int startedNum = 0;
			PriorityNode p = head;
			while (p != null) {
				if (p.v.isStarted()) {
					startedNum += p.v.getJvmNum();
				}
				p = p.next;
			}
			return startedNum;
		}

	}

	class LocalRetry extends Thread {
		ArrayList<ChildStrategy> alreadyInProgress = new ArrayList<ChildStrategy>();

		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
					//System.out.println("I'm LocalRetry,I'm woring...");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// 1.get innormal dead child list
				PriorityNode node = line.firstNode();
				while (node != null) {
					if (!node.v.isStarted()) {
						node = node.next;
					} else {
						TaskRunner tr = node.v;
						ArrayList<ChildStrategy> restartChildStrategies = tr.getInnormalKilled();
						
						// 2.restart
						if (!restartChildStrategies.isEmpty()) {
							if (tr.isFinished()) {
								// if taskrunner is finished,create a new
								// one,which has the same taskid;start the
								// taskrunner without
								// considering jvmLimit,but still update jvmNum.
								System.out.println("create a new taskrunner ");
								tr = tr.clone();
								node.v = tr;
								for (ChildStrategy childStrategy : restartChildStrategies) {
									if (!alreadyInProgress
											.contains(childStrategy)) {
										ChildStrategy child = childStrategy.clone();
										tr.extendNewStrategy(
												child,
												new String[]{});
										alreadyInProgress.add(childStrategy);
										System.out
												.println("add childstrategy to a new taskrunner ");
									}
								}
								tr.setStarted(true);
								new Thread(tr).start();
								jvmNum += tr.getJvmNum();
							} else {
								for (ChildStrategy childStrategy : restartChildStrategies) {
									if (!alreadyInProgress
											.contains(childStrategy)) {
										ChildStrategy child = childStrategy.clone();
										System.out.println("-----ChildStrategy Type Judge Begin-------");
										System.out.println(child instanceof MyChildStrategy2);
										System.out.println("-----ChildStrategy Type Judge End-------");
										tr.extendNewStrategy(
												child,
												new String[]{});
										alreadyInProgress.add(childStrategy);
										System.out
												.println("add childstrategy to an old taskrunner");
									}
								}
							}
						}
						node = node.next;
					}
				}

			}

		}

	}
}
