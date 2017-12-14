import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.CreateMode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 主工作服务器类
 */
public class WorkServer {

	private volatile boolean running = false;  //记录服务器的状态

	private ZkClient zkClient;

	private static final String MASTER_PATH = "/master"; //master节点对应的zookeeper路径

	private IZkDataListener dataListener; //监听master节点的删除时间

	private RunningData serverData;  // 记录当前节点的信息

	private RunningData masterData; // 记录master节点的信息
	
	private ScheduledExecutorService delayExector = Executors.newScheduledThreadPool(1); //创建一个调度器
	private int delayTime = 5;  //延时时间

	public WorkServer(RunningData rd) {
		this.serverData = rd;
        //实例化dataListener
		this.dataListener = new IZkDataListener() {

			public void handleDataDeleted(String dataPath) throws Exception {
				// TODO Auto-generated method stub
				
				takeMaster();

                /**
                 * 为了应对网络抖动，当master节点被删除，判断master节点是否是当前节点，如果是，
                 * 进行争抢master操作，如果不是，延迟五秒钟再争抢master
                 */
//				if (masterData!=null && masterData.getName().equals(serverData.getName())){
//					takeMaster();
//
//				}else{
//					delayExector.schedule(new Runnable(){
//						public void run(){
//							takeMaster();
//						}
//					}, delayTime, TimeUnit.SECONDS);
//
//				}
				
				
			}

			public void handleDataChange(String dataPath, Object data)
					throws Exception {
				// TODO Auto-generated method stub

			}
		};
	}

	public ZkClient getZkClient() {
		return zkClient;
	}

	public void setZkClient(ZkClient zkClient) {
		this.zkClient = zkClient;
	}

    /**
     * 服务开启方法
     * @throws Exception
     */
	public void start() throws Exception {
		if (running) {
			throw new Exception("server has startup...");
		}
		running = true;
		zkClient.subscribeDataChanges(MASTER_PATH, dataListener);  //注册节点改变事件
		takeMaster();  //竞争master

	}

    /**
     * 服务停止方法
     * @throws Exception
     */
	public void stop() throws Exception {
		if (!running) {
			throw new Exception("server has stoped");
		}
		running = false;
		
		delayExector.shutdown();

		zkClient.unsubscribeDataChanges(MASTER_PATH, dataListener);

		releaseMaster();

	}


    /**
     * 选举master方法
     */
	private void takeMaster() {
		if (!running)
			return;

		try {
			zkClient.create(MASTER_PATH, serverData, CreateMode.EPHEMERAL); //创建一个master节点
			masterData = serverData;
			System.out.println(serverData.getName()+" is master");
            //模拟，五秒钟服务自动释放master权限
			delayExector.schedule(new Runnable() {			
				public void run() {
					// TODO Auto-generated method stub
					if (checkMaster()){
						releaseMaster();
					}
				}
			}, 5, TimeUnit.SECONDS);
			
		} catch (ZkNodeExistsException e) {
			RunningData runningData = zkClient.readData(MASTER_PATH, true);  //读取节点数据
			if (runningData == null) { //没有获取到节点数据，说明master不存在，竞争master
				takeMaster();
			} else {
				masterData = runningData;
			}
		} catch (Exception e) {
			// ignore;
		}

	}


    /**
     * 释放master方法
     */
	private void releaseMaster() {
        //判断当前节点是否是master,如果是，则删除master节点
		if (checkMaster()) {
			zkClient.delete(MASTER_PATH);

		}

	}

    /**
     * 检查自己是否是master方法
     * @return
     */
	private boolean checkMaster() {
		try {
			RunningData eventData = zkClient.readData(MASTER_PATH);
			masterData = eventData;
			if (masterData.getName().equals(serverData.getName())) {
				return true;
			}
			return false;
		} catch (ZkNoNodeException e) { //节点不存在情况
			return false;
		} catch (ZkInterruptedException e) { //中断异常，如果中断，重试
			return checkMaster();
		} catch (ZkException e) {
			return false;
		}
	}

}
