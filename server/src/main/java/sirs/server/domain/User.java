package sirs.server.domain;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    /* Cascade ensure that when user is deleted all pending invites are deleted
       Orphan removal ensures that when invite is removed from list, it is deleted in the database
    */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Invite> pendingInvites = new ArrayList<Invite>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "file_ids", nullable = false)
    private List<File> files = new ArrayList<File>();

    // TODO: change type
    private byte[] certificate = new byte[1];

    public User() {
    }

    public User(String username, byte[] certificate) {
        this.username = username;
        this.certificate = certificate;
    }

    public int getId() {
        return id;
    }

    public List<Invite> getPendingInvites() {
        return pendingInvites;
    }

    public void setPendingInvites(List<Invite> pendingInvites) {
        this.pendingInvites = pendingInvites;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void addInvite(Invite invite) {
        this.pendingInvites.add(invite);
    }

    public void addFile(File file) {
        this.files.add(file);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", pendingInvites=" + pendingInvites +
                ", files=" + files +
                ", certificate=" + Arrays.toString(certificate) +
                '}';
    }
}
