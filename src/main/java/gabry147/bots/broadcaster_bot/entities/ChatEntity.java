package gabry147.bots.broadcaster_bot.entities;

import javax.persistence.*;

import gabry147.bots.broadcaster_bot.entities.dao.Broadcaster_BotDao;
import gabry147.bots.broadcaster_bot.entities.extra.ChatRole;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@NamedQuery(name = "ChatEntity.findAll",query = "SELECT c FROM ChatEntity c")
@Table(name = "chats")
public class ChatEntity implements Serializable{

    @Id
    @Column(name = "chat_id")
    private long chatId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added")
    private Date added;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name="role")
    private ChatRole role;

    public static ChatEntity getById(long id){
        EntityManager em= Broadcaster_BotDao.instance.createEntityManager();
        ChatEntity chatEntity =em.find(ChatEntity.class,id);
        Broadcaster_BotDao.instance.closeConnections(em);

        return chatEntity;
    }

    public static List<ChatEntity> getAll(){
        EntityManager em=Broadcaster_BotDao.instance.createEntityManager();
        List<ChatEntity> chatEntities = em.createNamedQuery("ChatEntity.findAll").getResultList();
        Broadcaster_BotDao.instance.closeConnections(em);

        return chatEntities;
    }

    public static ChatEntity saveChat(ChatEntity c){
        EntityManager em=Broadcaster_BotDao.instance.createEntityManager();
        EntityTransaction tx=em.getTransaction();
        tx.begin();
        c=em.merge(c);
        tx.commit();
        Broadcaster_BotDao.instance.closeConnections(em);
        return c;
    }
    
    public static void removeChat(ChatEntity c){
        EntityManager em=Broadcaster_BotDao.instance.createEntityManager();
        EntityTransaction tx=em.getTransaction();
        tx.begin();
        c=em.merge(c);
        em.remove(c);
        tx.commit();
        Broadcaster_BotDao.instance.closeConnections(em);
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

	public ChatRole getRole() {
		return role;
	}

	public void setRole(ChatRole role) {
		this.role = role;
	}
}
