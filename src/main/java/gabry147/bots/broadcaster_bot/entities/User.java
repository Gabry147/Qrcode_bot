package gabry147.bots.broadcaster_bot.entities;

import javax.persistence.*;

import gabry147.bots.broadcaster_bot.entities.dao.Broadcaster_BotDao;
import gabry147.bots.broadcaster_bot.entities.extra.Role;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
public class User implements Serializable {
    @Id
    @Column(name="user_id")
    private long userId;

    private String username;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToMany(cascade = CascadeType.PERSIST,fetch = FetchType.EAGER, mappedBy = "users")
    private Set<ChatEntity> chatEntities;

    public static User getById(Long id){
        EntityManager em= Broadcaster_BotDao.instance.createEntityManager();
        User u=em.find(User.class,id);
        Broadcaster_BotDao.instance.closeConnections(em);

        return u;
    }

    public static User saveUser(User u){
        EntityManager em=Broadcaster_BotDao.instance.createEntityManager();
        EntityTransaction tx=em.getTransaction();
        tx.begin();
        u=em.merge(u);
        tx.commit();
        Broadcaster_BotDao.instance.closeConnections(em);

        return u;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<ChatEntity> getChatEntities() {
        return chatEntities;
    }

    public void setChatEntities(Set<ChatEntity> chatEntities) {
        this.chatEntities = chatEntities;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
