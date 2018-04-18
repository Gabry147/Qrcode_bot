package gabry147.bots.broadcaster_bot.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@NamedQuery(name = "CommandEntity.findAll",query = "SELECT c FROM CommandEntity c")
@Table(name = "users")
public class CommandEntity  implements Serializable {
	@Id
    @Column(name="command_id")
    private String commandId;
	
	@Column(name="username")
    private String username;
}
