package edu.sjtu.infosec.ismp.manager.TM.discover.service.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infosec.ismp.manager.rmi.lm.pfLog.model.SystemLog;
import org.infosec.ismp.manager.rmi.tm.discover.model.Line;
import org.infosec.ismp.manager.rmi.tm.discover.model.Node;
import org.infosec.ismp.manager.rmi.tm.discover.model.NodeType;
import org.infosec.ismp.manager.rmi.tm.discover.model.appletForm.ICMPAppletForm;
import org.infosec.ismp.manager.rmi.tm.discover.service.agent.ICMPService;
import org.infosec.ismp.manager.rmi.tm.discover.service.applet.ICMPSearchService;
import org.springframework.transaction.annotation.Transactional;

import edu.sjtu.infosec.ismp.manager.LM.pfLog.service.SystemLogService;
import edu.sjtu.infosec.ismp.manager.SYSM.user.self.service.UserService;
import edu.sjtu.infosec.ismp.manager.TM.discover.comm.attribute.BaseAttribute;
import edu.sjtu.infosec.ismp.manager.TM.discover.dao.LineDao;
import edu.sjtu.infosec.ismp.manager.TM.discover.dao.NodeDao;
import edu.sjtu.infosec.ismp.manager.TM.discover.dao.NodeTypeDao;
import edu.sjtu.infosec.ismp.security.User;


/**
 * ICMP搜索Service实现类
 * @author Wu Guojie
 * @date 2009-6-8
 * @version 1.0
 */
public class ICMPSearchServiceImpl implements ICMPSearchService {
	private ICMPService icmpGetService;
	
	public void setIcmpGetService(ICMPService icmpGetService) {
		this.icmpGetService = icmpGetService;
	}
	
	private SystemLogService systemlogservice;

	public void setSystemlogservice(SystemLogService systemlogservice) {
		this.systemlogservice = systemlogservice;
	}

	private UserService userService;

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	
	
	
	/**
	 * 节点dao
	 */
	private NodeDao nodeDao;
	/**
	 * 节点类型dao
	 */
	private NodeTypeDao nodeTypeDao;
	/**
	 * 节点间连线dao
	 */
	private LineDao lineDao;
	
	
	/**
     * @param vNodeDao the nodeDao to set
     */
    public void setNodeDao(NodeDao vNodeDao) {
    	nodeDao = vNodeDao;
    }
	/**
     * @param vNodeTypeDao the nodeTypeDao to set
     */
    public void setNodeTypeDao(NodeTypeDao vNodeTypeDao) {
    	nodeTypeDao = vNodeTypeDao;
    }
	/**
     * @param vLineDao the lineDao to set
     */
    public void setLineDao(LineDao vLineDao) {
    	lineDao = vLineDao;
    }
	
	
	
	

	/**
	 * 搜索
	 * @param icmpAppletForm
	 * icmpAppletForm
	 * @return 搜索结果list
	 */
	public List<Node> doSearch(ICMPAppletForm icmpAppletForm) throws Exception {
		
		return null;
	}

	/**
	 * 通过icmp来Ping
	 * @param ip
	 * ip
	 * @param outTime
	 * 超时时间
	 * @return ping结果
	 */
	public Map<String, String> icmpPing(String ip, int outTime) throws Exception {
		Process p = null;
		InputStream is=null;
		StringBuffer sb = new StringBuffer();
		/**
		 * 结果map
		 */
		Map<String, String> map = new HashMap<String, String>();
		/**
		 * 耗时
		 */
		double pingTime = 0;
		/**
		 * ping消息
		 */
		String pingMessage = "";

        try {
        	BaseAttribute.setLastActiveTime();
	        /**
	         * 此处应为agent中的代码，由于agent接口暂时没有提供，为实现效果，先将代码写在此
	         */
        	/*
        	p = Runtime.getRuntime().exec("ping " + ip + " -n 1 -w " + outTime);
	        is = p.getInputStream();
	        int b;
	        while ((b = is.read()) != -1) {
	            sb.append((char) b);
	        }
	        pingMessage = pingMessage + sb.toString();
	        */
        	pingMessage = icmpGetService.ping(ip, outTime);
	        
	        if(pingMessage.contains("Packets: Sent = 1, Received = 1, Lost = 0 (0% loss),")){
	        	if(pingMessage.contains("time=")){
    		        pingTime = Double.parseDouble(pingMessage.substring(pingMessage.indexOf("time=") + "time=".length(), pingMessage.indexOf("ms", pingMessage.indexOf("time="))))/1000;
    	        }else{
    	        	pingTime = 0.001;
    			}
	        }else{
        		pingTime = (long)outTime/1000;
        	}
        	
	        map.put("pingMessage", pingMessage);
	        map.put("pingTime", pingTime+"");
	        return map;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
//			return null;
		}
	}

	/**
	 * 开始搜索
	 */
	public boolean startSearch() throws Exception {
		/**
		 * 实例化全局属性
		 */
		BaseAttribute baseAttribute = BaseAttribute.getInstance();
		if((!baseAttribute.TOPO_SEARCH_STATE_RUNNING) & (baseAttribute.TOPO_SEARCH_RUNNING_COUNT==0)){
			baseAttribute.running();
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 结束搜索
	 */
	public boolean stoppedSearch() throws Exception {
		/**
		 * 实例化全局属性
		 */
		BaseAttribute baseAttribute = BaseAttribute.getInstance();
		if(baseAttribute.TOPO_SEARCH_STATE_RUNNING & (baseAttribute.TOPO_SEARCH_RUNNING_COUNT==1)){
			baseAttribute.stopped();
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 通过id获取节点类型
	 * @param id
	 * id
	 * @return 节点类型
	 */
	 @Transactional
	public NodeType getNodeTypeById(int id) throws Exception {
		/**
		 * 节点类型
		 */
		NodeType nodeType = nodeTypeDao.findNodeTypeById(id);
		return nodeType;
	}

	/**
	 * 将节点list入库
	 * @param list
	 * 节点list
	 */
	 @Transactional
	public void addNodeList(List<Node> list) throws Exception {
		List<Line> lineList = lineDao.findAllLines();
		if(lineList!=null && lineList.size()>0){
			for(Line line : lineList){
				lineDao.deleteLine(line);
			}
		}
		List<Node> nodeList = nodeDao.findAllNodes();
		if(nodeList!=null && nodeList.size()>0){
			for(Node node : nodeList){
				nodeDao.deleteNode(node);
			}
		}
		try{
			if(list!=null && list.size()>0){
				for(Node node : list){
					nodeDao.addNode(node);
				}
			}else{
//				throw new Exception("增加的节点不能为空！");
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * 是否有人正在执行搜索功能
	 * @return 是/否
	 */
	public boolean isSearching() throws Exception {
		/**
		 * 实例化全局属性
		 */
		BaseAttribute baseAttribute = BaseAttribute.getInstance();
		return baseAttribute.TOPO_SEARCH_STATE_RUNNING;
	}
	
//	/**
//	 * 方法说明：获取所有AgentBO列表(数据库的)
//	 * @return
//	 */
//	public List<AgentBO> getAllAgentBO() throws Exception{
//		return icmpGetService.getAllAgentBO();
//	}
//	
//	/**
//	 * 方法说明：获取所有已经向Manager注册的AgentBO列表（邓东和数据库的交际）
//	 * @return
//	 */
//	public List<AgentBO> getAllRegisteredAgentBO() throws Exception{
//		return icmpGetService.getAllRegisteredAgentBO();
//	}

	/**
	 * 写日志
	 * @param log
	 * 日志
	 */
	public void writeToSysLog(SystemLog log, int userId) {
		User user = userService.getUserByIdService(userId);
		log.setUsername(user.getUsername());
		log.setRoleName(user.getRoleNames());
		systemlogservice.saveSystemLog(log);
	}
	
	
}
