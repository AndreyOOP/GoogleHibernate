package Tst;

import javax.persistence.*;

@Entity
@Table(name = "testuserauth", schema = "gdb", catalog = "")
public class TableEntity {

    private String login;
    private String password;

    @Id
    @Column(name="login")
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Basic
    @Column(name="password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
