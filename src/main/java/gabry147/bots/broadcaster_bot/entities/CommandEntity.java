package gabry147.bots.broadcaster_bot.entities;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import gabry147.bots.broadcaster_bot.entities.dao.Broadcaster_BotDao;

@Entity
@NamedQuery(name = "CommandEntity.findAll",query = "SELECT c FROM CommandEntity c")
@Table(name = "commands")
public class CommandEntity  implements Serializable {
	@Id
    @Column(name="command_id")
    private String commandId;
	
	@Column(name="body")
    private String body;

	public static CommandEntity getById(String id){
        EntityManager em= Broadcaster_BotDao.instance.createEntityManager();
        CommandEntity cm=em.find(CommandEntity.class,id);
        Broadcaster_BotDao.instance.closeConnections(em);

        return cm;
    }
    
    public static List<CommandEntity> getAll(){
        EntityManager em=Broadcaster_BotDao.instance.createEntityManager();
        List<CommandEntity> commandEntities = em.createNamedQuery("CommandEntity.findAll").getResultList();
        Broadcaster_BotDao.instance.closeConnections(em);
        return commandEntities;
    }

    public static CommandEntity saveCommand(CommandEntity cm){
        EntityManager em=Broadcaster_BotDao.instance.createEntityManager();
        EntityTransaction tx=em.getTransaction();
        tx.begin();
        cm=em.merge(cm);
        tx.commit();
        Broadcaster_BotDao.instance.closeConnections(em);

        return cm;
    }
    
    public static void deleteCommand(CommandEntity cm){
    	EntityManager em=Broadcaster_BotDao.instance.createEntityManager();
        EntityTransaction tx=em.getTransaction();
        tx.begin();
        cm=em.merge(cm);
        em.remove(cm);
        tx.commit();
        Broadcaster_BotDao.instance.closeConnections(em);
    }
	
	public String getCommandId() {
		return commandId;
	}

	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
