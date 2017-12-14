import java.io.Serializable;

/**
 * 记录正在运行的服务器信息
 */
public class RunningData implements Serializable {

	private static final long serialVersionUID = 4260577459043203630L;
	
	
	
	private Long cid;   //记录服务器的id
	private String name;  //记录服务器的名称
	public Long getCid() {
		return cid;
	}
	public void setCid(Long cid) {
		this.cid = cid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	

}
